@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.command

import com.github.mikecraft1224.simplecore.command.api.ClientCommandContext
import com.github.mikecraft1224.simplecore.command.api.SubCommand
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

/**
 * DSL builder for a command or subcommand node.
 *
 * Returned as the receiver of [CommandRegistry.client], [literal], and [argument] blocks.
 * The builder stores the command tree as data and materialises it into Brigadier nodes via
 * [buildLiteral] once per game session join.
 *
 * ### Tree structure
 * ```
 * CommandRegistry.client("mymod") {           <- literal node
 *     literal("sub") {                         <- literal child
 *         argument("count", integer(1, 10)) {  <- argument child
 *             executes { ... }
 *         }
 *     }
 * }
 * ```
 *
 * ### Space decomposition
 * Spaces in [literal] and [argument] names automatically create nested nodes:
 * ```kotlin
 * // These two are equivalent:
 * literal("test echo") { executes { sendFeedback("hi") } }
 *
 * literal("test") {
 *     literal("echo") { executes { sendFeedback("hi") } }
 * }
 *
 * // And these two are equivalent:
 * argCallback("give item", word()) { sendFeedback("Item: ${string("item")}") }
 *
 * literal("give") {
 *     argument("item", word()) { executes { sendFeedback("Item: ${string("item")}") } }
 * }
 * ```
 */
class CommandBuilder internal constructor(
    internal val name: String,
    /** `null` for literal nodes; the argument type for argument nodes. */
    private val argumentType: ArgumentType<*>? = null,
) {
    private var executes: (ClientCommandContext.() -> Unit)? = null
    private var requires: ((FabricClientCommandSource) -> Boolean)? = null
    private var suggestionsProvider: (SuggestionsBuilder.(CommandContext<FabricClientCommandSource>) -> Unit)? = null
    private val children = mutableListOf<CommandBuilder>()

    /** Human-readable description of this node. Exposed on [CommandHandle] for the root. */
    internal var description: String = ""

    // -------------------------------------------------------------------------
    // DSL - metadata
    // -------------------------------------------------------------------------

    /**
     * Sets a human-readable description for this command.
     *
     * The description is stored on the [CommandHandle] returned by [CommandRegistry.client]
     * and is available for use by a help command or listing UI.
     *
     * ```kotlin
     * CommandRegistry.client("mymod") {
     *     description("Main command for My Mod")
     *     // ...
     * }
     * ```
     */
    fun description(text: String) {
        description = text
    }

    // -------------------------------------------------------------------------
    // DSL - execution
    // -------------------------------------------------------------------------

    /**
     * Sets the execution handler for this node.
     *
     * The block receives a [ClientCommandContext] as its receiver, which exposes typed argument
     * accessors and feedback helpers.
     *
     * ```kotlin
     * literal("hello") {
     *     executes { sendFeedback("Hello!") }
     * }
     * ```
     */
    fun executes(block: ClientCommandContext.() -> Unit) {
        executes = block
    }

    /**
     * Sets an execution handler that needs no [ClientCommandContext].
     *
     * Use when the node fires a side effect and doesn't read arguments or send chat feedback.
     * When you need feedback or argument access, use [executes] instead:
     * ```kotlin
     * // No context needed - simpleCallback is cleanest:
     * literal("reload") {
     *     simpleCallback { manager.reload() }
     * }
     *
     * // Context needed for feedback/args - use executes:
     * literal("reload") {
     *     executes { manager.reload(); sendFeedback("§aReloaded.") }
     * }
     * ```
     *
     * Equivalent to `executes { block() }`.
     */
    fun simpleCallback(block: () -> Unit) {
        executes = { block() }
    }

    // -------------------------------------------------------------------------
    // DSL - guards
    // -------------------------------------------------------------------------

    /**
     * Adds a guard predicate to this node.
     *
     * When the predicate returns `false`, this node and all its descendants are hidden from
     * tab completion and cannot be executed. Evaluated on every command or suggestion request.
     *
     * ```kotlin
     * literal("admin") {
     *     requires { source -> source.player?.hasPermissionLevel(4) == true }
     *     executes { sendFeedback("Admin-only command.") }
     * }
     * ```
     *
     * [CommandHandle.disable] is implemented via this mechanism - the guard always returns
     * `false` when the handle is disabled.
     */
    fun requires(predicate: (FabricClientCommandSource) -> Boolean) {
        requires = predicate
    }

    // -------------------------------------------------------------------------
    // DSL - tab completion
    // -------------------------------------------------------------------------

    /**
     * Overrides tab-completion suggestions for this argument node.
     *
     * The block receives a [SuggestionsBuilder] as receiver and the current command context
     * as a parameter. Only has an effect on nodes added via [argument]; silently ignored on literals.
     *
     * ```kotlin
     * argument("player", word()) {
     *     suggests { _ ->
     *         suggest("Steve")
     *         suggest("Alex")
     *     }
     *     executes { sendFeedback("Selected: ${string("player")}") }
     * }
     * ```
     */
    fun suggests(provider: SuggestionsBuilder.(ctx: CommandContext<FabricClientCommandSource>) -> Unit) {
        suggestionsProvider = provider
    }

    /**
     * Sets a static, known-at-call-time list of tab-completion suggestions for this argument node.
     *
     * Filters suggestions by prefix automatically - only entries that start with the player's
     * current input (case-insensitive) are offered:
     * ```kotlin
     * argument("mode", word()) {
     *     suggestStatic("fast", "normal", "slow")
     *     executes { sendFeedback("Mode: ${string("mode")}") }
     * }
     * ```
     *
     * Prefer this over [suggests] for fixed option sets; use [suggestDynamic] when the
     * options are computed at runtime.
     */
    fun suggestStatic(vararg options: String) {
        val opts = options.toList()
        suggestionsProvider = { _ ->
            val remaining = remainingLowerCase
            opts.filter { it.lowercase().startsWith(remaining) }.forEach { suggest(it) }
        }
    }

    /**
     * Sets a dynamically-computed list of tab-completion suggestions for this argument node.
     *
     * [supplier] is called fresh on every tab-complete request, so the list can change at
     * runtime (e.g. online players, loaded config keys). Results are filtered by prefix:
     * ```kotlin
     * argument("player", word()) {
     *     suggestDynamic { mc.networkHandler?.playerList?.map { it.profile.name } ?: emptyList() }
     *     executes { sendFeedback("Player: ${string("player")}") }
     * }
     * ```
     */
    fun suggestDynamic(supplier: () -> Collection<String>) {
        suggestionsProvider = { _ ->
            val remaining = remainingLowerCase
            supplier().filter { it.lowercase().startsWith(remaining) }.forEach { suggest(it) }
        }
    }

    // -------------------------------------------------------------------------
    // DSL - subcommand tree
    // -------------------------------------------------------------------------

    /**
     * Adds one or more literal subcommand nodes, all sharing the same [block].
     *
     * When [name] or an alias contains spaces they are automatically decomposed into nested
     * literal nodes, with the [block] applied to the innermost one:
     * ```kotlin
     * // Single literal:
     * literal("reload") { executes { sendFeedback("Reloaded.") } }
     *
     * // Multi-word shorthand - creates nested literals "test" -> "echo":
     * literal("test echo") { executes { sendFeedback("Echo!") } }
     *
     * // Multiple aliases at the same level (all trigger the same block):
     * literal("reload", "rl") { executes { sendFeedback("Reloaded.") } }
     * ```
     */
    fun literal(name: String, vararg aliases: String, block: CommandBuilder.() -> Unit) {
        checkNotGreedy()
        children += buildLiteralNode(name, block)
        for (alias in aliases) {
            children += buildLiteralNode(alias, block)
        }
    }

    /**
     * Shorthand for a literal node that executes immediately - no nested [executes] block needed.
     *
     * Equivalent to `literal(name) { executes { block } }`. Supports multiple names as aliases
     * and space decomposition.
     *
     * ```kotlin
     * // Instead of:
     * literal("reload") {
     *     executes { manager.reload(); sendFeedback("Reloaded.") }
     * }
     *
     * // Write:
     * literalCallback("reload") { manager.reload(); sendFeedback("Reloaded.") }
     *
     * // With alias:
     * literalCallback("reload", "rl") { manager.reload(); sendFeedback("Reloaded.") }
     * ```
     */
    fun literalCallback(vararg names: String, block: ClientCommandContext.() -> Unit) {
        for (name in names) {
            children += buildLiteralNode(name) { executes(block) }
        }
    }

    /**
     * Installs a [SubCommand] into this node.
     *
     * Delegates to [SubCommand.register] with `this` as the receiver, allowing any
     * [SubCommand] defined in a separate file to add its literals and arguments here.
     *
     * ```kotlin
     * CommandRegistry.client("mymod") {
     *     install(GiveCommand)
     *     install(TeleportCommand)
     * }
     * ```
     *
     * [SubCommand]s can themselves call `install` to compose further sub-groups at any depth.
     */
    fun install(sub: SubCommand) {
        with(sub) { register() }
    }

    /**
     * Adds a typed argument node.
     *
     * Use the factory functions from [com.github.mikecraft1224.simplecore.command.api]
     * (`integer()`, `word()`, `greedyString()`, etc.) for the [type] argument.
     *
     * When [name] contains spaces the leading words become auto-inserted literal nodes and
     * only the last word is the argument name. This keeps flat commands readable:
     * ```kotlin
     * // Instead of:
     * literal("give") {
     *     argument("item", word()) { executes { sendFeedback(string("item")) } }
     * }
     *
     * // Write:
     * argument("give item", word()) { executes { sendFeedback(string("item")) } }
     * ```
     */
    fun argument(name: String, type: ArgumentType<*>, block: CommandBuilder.() -> Unit) {
        checkNotGreedy()
        val parts = name.trim().split(" ")
        val argNode = CommandBuilder(parts.last(), argumentType = type).apply(block)
        children += buildLiteralChain(parts.dropLast(1), argNode)
    }

    /**
     * Shorthand for an argument node that executes immediately - no nested [executes] block needed.
     *
     * Equivalent to `argument(name, type) { executes { block } }`. Supports space decomposition
     * for the name.
     *
     * ```kotlin
     * // Instead of:
     * argument("message", greedyString()) {
     *     executes { sendFeedback(string("message")) }
     * }
     *
     * // Write:
     * argCallback("message", greedyString()) { sendFeedback(string("message")) }
     *
     * // With space prefix:
     * argCallback("echo message", greedyString()) { sendFeedback(string("message")) }
     * ```
     */
    fun argCallback(name: String, type: ArgumentType<*>, block: ClientCommandContext.() -> Unit) {
        argument(name, type) { executes(block) }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Asserts that this node is not a greedy string argument.
     *
     * A greedy string argument consumes all remaining input, so attaching children to it
     * would create a branch that Brigadier can never reach. This guard catches the mistake
     * early with a clear message rather than a silent no-op or a confusing Brigadier error.
     */
    private fun checkNotGreedy() {
        require(
            argumentType !is StringArgumentType ||
            argumentType.type != StringArgumentType.StringType.GREEDY_PHRASE
        ) {
            "Cannot add children to greedy string argument '$name'. Greedy arguments must be terminal."
        }
    }

    /**
     * Decomposes [name] on whitespace and creates nested [CommandBuilder] literal nodes,
     * applying [block] to the innermost one.
     */
    private fun buildLiteralNode(name: String, block: CommandBuilder.() -> Unit): CommandBuilder {
        val parts = name.trim().split(" ")
        val innermost = CommandBuilder(parts.last()).apply(block)
        return buildLiteralChain(parts.dropLast(1), innermost)
    }

    /**
     * Wraps [innermost] in a chain of literal nodes for each entry in [names].
     * Returns [innermost] directly when [names] is empty.
     *
     * Note: private members of a class are accessible between instances of the same class in
     * Kotlin (class-scoped, not instance-scoped), so `other.children` is accessible here.
     */
    private fun buildLiteralChain(names: List<String>, innermost: CommandBuilder): CommandBuilder {
        if (names.isEmpty()) return innermost
        val inner = buildLiteralChain(names.drop(1), innermost)
        val node = CommandBuilder(names.first())
        node.children += inner
        return node
    }

    // -------------------------------------------------------------------------
    // Brigadier materialisation - called once per game session join
    // -------------------------------------------------------------------------

    /** Builds the root literal Brigadier node. Called once per game session join. */
    internal fun buildLiteral(handle: CommandHandle): LiteralArgumentBuilder<FabricClientCommandSource> {
        val node = ClientCommandManager.literal(name)
        val guard = requires
        node.requires { source -> handle.isEnabled && (guard == null || guard(source)) }
        executes?.let { block ->
            node.executes { ctx ->
                ClientCommandContext(ctx).block()
                Command.SINGLE_SUCCESS
            }
        }
        for (child in children) {
            if (child.argumentType == null) node.then(child.buildLiteral(handle))
            else node.then(child.buildArgument(handle))
        }
        return node
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildArgument(handle: CommandHandle): RequiredArgumentBuilder<FabricClientCommandSource, *> {
        val node = ClientCommandManager.argument(name, argumentType as ArgumentType<Any>)
        suggestionsProvider?.let { provider ->
            node.suggests { ctx, builder ->
                builder.provider(ctx)
                builder.buildFuture()
            }
        }
        val guard = requires
        node.requires { source -> handle.isEnabled && (guard == null || guard(source)) }
        executes?.let { block ->
            node.executes { ctx ->
                ClientCommandContext(ctx).block()
                Command.SINGLE_SUCCESS
            }
        }
        for (child in children) {
            if (child.argumentType == null) node.then(child.buildLiteral(handle))
            else node.then(child.buildArgument(handle))
        }
        return node
    }
}
