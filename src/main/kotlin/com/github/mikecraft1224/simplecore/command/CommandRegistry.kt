package com.github.mikecraft1224.simplecore.command

import com.github.mikecraft1224.simplecore.Logger
import com.github.mikecraft1224.simplecore.bus.api.Feature
import net.fabricmc.fabric.api.client.command.v2.ClientCommands
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central registry for client-side chat commands.
 *
 * Discovered automatically as a [@Feature][Feature] - no explicit registration required.
 * Commands are defined with a Kotlin DSL that wraps Brigadier's API. Each registration
 * returns a [CommandHandle] for enabling/disabling the command at runtime.
 *
 * ### Registering a command
 * ```kotlin
 * CommandRegistry.client("mymod") {
 *     executes { sendFeedback("Hello from /mymod!") }
 *
 *     literal("sub") {
 *         executes { sendFeedback("Hello from /mymod sub!") }
 *     }
 * }
 * ```
 *
 * ### With arguments
 * ```kotlin
 * CommandRegistry.client("greet") {
 *     argument("name", word()) {
 *         executes { sendFeedback("Hello, ${string("name")}!") }
 *     }
 * }
 * ```
 *
 * ### Aliases
 * ```kotlin
 * val handle = CommandRegistry.client("simplecore", "sc") {
 *     executes { sendFeedback("SimpleCore ${BuildConfig.MOD_VERSION}") }
 * }
 *
 * // Disable later if needed:
 * handle.disable()
 * ```
 *
 * ### Permission guards
 * ```kotlin
 * CommandRegistry.client("admin") {
 *     requires { source -> source.player?.hasPermissionLevel(4) == true }
 *     executes { sendFeedback("Admin command executed.") }
 * }
 * ```
 *
 * ### Tab completion
 * ```kotlin
 * CommandRegistry.client("select") {
 *     argument("mode", word()) {
 *         suggests { _ -> listOf("fast", "slow", "auto").forEach { suggest(it) } }
 *         executes { sendFeedback("Mode: ${string("mode")}") }
 *     }
 * }
 * ```
 *
 * ### Argument type imports
 * Import from [com.github.mikecraft1224.simplecore.command.api] for clean DSL syntax:
 * `integer()`, `word()`, `greedyString()`, `bool()`, etc.
 */
@Suppress("Unused")
@Feature
object CommandRegistry {

    private data class Registration(
        val handle: CommandHandle,
        val builder: CommandBuilder,
        val aliases: List<String>,
    )

    private val registrations: MutableList<Registration> = CopyOnWriteArrayList()

    init {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registrations.forEach { (handle, builder, aliases) ->
                val node = dispatcher.register(builder.buildLiteral(handle))
                aliases.forEach { alias ->
                    val aliasBuilder = ClientCommands.literal(alias)
                        .redirect(node)
                    node.command?.let { aliasBuilder.executes(it) }
                    dispatcher.register(aliasBuilder)
                }
            }
        }
    }

    /**
     * Registers a client-side chat command.
     *
     * The command tree is built lazily from [block] on each game session join, so the
     * same block describes the command whether you join singleplayer or a multiplayer server.
     * All aliases share the same [CommandHandle] - disabling it disables every alias.
     *
     * @param name The primary command name (without the leading `/`).
     * @param aliases Optional alternative names that invoke the same command tree.
     * @param block DSL block that defines the command's structure and behaviour.
     * @return A [CommandHandle] for enabling/disabling the command at runtime.
     */
    fun client(name: String, vararg aliases: String, block: CommandBuilder.() -> Unit): CommandHandle {
        val handle = CommandHandle(name)
        val registeredNames = registrations
            .flatMap { listOf(it.builder.name) + it.aliases }
            .toHashSet()

        val primaryBuilder = CommandBuilder(name).apply(block)
        handle.description = primaryBuilder.description

        (listOf(name) + aliases.toList()).forEach { n ->
            if (n in registeredNames) Logger.warn("[Commands] Command '$n' is already registered.")
        }

        registrations += Registration(handle, primaryBuilder, aliases.toList())
        return handle
    }
}
