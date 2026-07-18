@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.command.api

import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.IdentifierArgument
//? if >= 26.2 {
/*import net.minecraft.commands.arguments.TeamColorArgument
import net.minecraft.world.scores.TeamColor
*///?} else {
import net.minecraft.commands.arguments.ColorArgument
import net.minecraft.ChatFormatting
//?}
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

/**
 * Wraps the Brigadier [CommandContext] for a client command, providing typed argument
 * accessors, Minecraft-specific helpers, and chat feedback utilities.
 *
 * This is the receiver type inside every `executes { }` block in the command DSL:
 * ```kotlin
 * literal("greet") {
 *     argument("name", word()) {
 *         executes {
 *             // 'this' is ClientCommandContext
 *             sendFeedback("Hello, ${string("name")}!")
 *         }
 *     }
 * }
 * ```
 */
class ClientCommandContext internal constructor(
    /** The raw Brigadier context. Available for advanced use cases not covered by this wrapper. */
    val raw: CommandContext<FabricClientCommandSource>,
) {

    /** The Fabric client command source. */
    val source: FabricClientCommandSource get() = raw.source

    /** The Minecraft client instance. */
    val client: Minecraft get() = source.client

    /** The local player. Non-null during any client command execution. */
    val player: LocalPlayer get() = source.player

    /** The full raw command string that was entered (e.g. `"mymod give Steve dirt 32"`). */
    val input: String get() = raw.input

    // -------------------------------------------------------------------------
    // Primitive argument accessors
    // -------------------------------------------------------------------------

    /** Returns the integer argument with the given [name]. */
    fun int(name: String): Int = IntegerArgumentType.getInteger(raw, name)

    /** Returns the long argument with the given [name]. */
    fun long(name: String): Long = LongArgumentType.getLong(raw, name)

    /** Returns the float argument with the given [name]. */
    fun float(name: String): Float = FloatArgumentType.getFloat(raw, name)

    /** Returns the double argument with the given [name]. */
    fun double(name: String): Double = DoubleArgumentType.getDouble(raw, name)

    /** Returns the boolean argument with the given [name]. */
    fun bool(name: String): Boolean = BoolArgumentType.getBool(raw, name)

    /**
     * Returns the string argument with the given [name].
     * Works for any string argument type: [word], [string], or [greedyString].
     */
    fun string(name: String): String = StringArgumentType.getString(raw, name)

    // -------------------------------------------------------------------------
    // Minecraft argument accessors
    // -------------------------------------------------------------------------

    /**
     * Returns the [Identifier] argument with the given [name].
     * Use with [identifier][com.github.mikecraft1224.simplecore.command.api.identifier] argument type.
     */
    @Suppress("UNCHECKED_CAST")
    fun identifier(name: String): Identifier = IdentifierArgument.getId(raw as CommandContext<CommandSourceStack>, name)

    /**
     * Returns the team-color argument with the given [name].
     * Use with [color][com.github.mikecraft1224.simplecore.command.api.color] argument type.
     */
    @Suppress("UNCHECKED_CAST")
    //? if >= 26.2 {
    /*fun color(name: String): TeamColor = TeamColorArgument.getTeamColor(raw as CommandContext<CommandSourceStack>, name)
    *///?} else {
    fun color(name: String): ChatFormatting = ColorArgument.getColor(raw as CommandContext<CommandSourceStack>, name)
    //?}

    // -------------------------------------------------------------------------
    // Generic accessor - for argument types not covered above
    // -------------------------------------------------------------------------

    /**
     * Returns any argument by [name], cast to the reified type [T].
     *
     * Use this for Minecraft argument types not covered by the named accessors above,
     * or for custom `ArgumentType<T>` implementations:
     * ```kotlin
     * argument("nbt", NbtCompoundArgumentType.nbtCompound()) {
     *     executes {
     *         val tag = get<NbtCompound>("nbt")
     *         sendFeedback(tag.toString())
     *     }
     * }
     * ```
     */
    inline fun <reified T> get(name: String): T = raw.getArgument(name, T::class.java)

    // -------------------------------------------------------------------------
    // Feedback helpers
    // -------------------------------------------------------------------------

    /** Sends a plain-text message to the local player's chat output. */
    fun sendFeedback(message: String) = source.sendFeedback(Component.literal(message))

    /** Sends a formatted [Component] message to the local player's chat output. */
    fun sendFeedback(message: Component) = source.sendFeedback(message)

    /** Sends a plain-text error message to the local player's chat (shown in red). */
    fun sendError(message: String) = source.sendError(Component.literal(message))

    /** Sends a formatted [Component] error message to the local player's chat (shown in red). */
    fun sendError(message: Component) = source.sendError(message)

    // -------------------------------------------------------------------------
    // Exception helper
    // -------------------------------------------------------------------------

    /**
     * Throws a [com.mojang.brigadier.exceptions.CommandSyntaxException] with [message]
     * as the reason, immediately aborting command execution.
     *
     * The exception is caught by Brigadier and shown to the player as a red system
     * message - appropriate for invalid argument combinations caught at execution time:
     * ```kotlin
     * executes {
     *     val count = int("count")
     *     if (count > player.inventory.size()) fail("You only have ${player.inventory.size()} slots.")
     *     // ...
     * }
     * ```
     *
     * For errors that don't abort execution (e.g. partial success), use [sendError] instead.
     */
    fun fail(message: String): Nothing =
        throw SimpleCommandExceptionType(Component.literal(message)).create()
}
