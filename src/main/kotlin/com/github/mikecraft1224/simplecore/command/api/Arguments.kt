@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.command.api

import com.mojang.brigadier.arguments.*
import net.minecraft.command.argument.ColorArgumentType
import net.minecraft.command.argument.IdentifierArgumentType

/**
 * Convenience factories for Brigadier argument types.
 *
 * These are designed to be used inside the command DSL so you don't need to write
 * `IntegerArgumentType.integer(...)` at every call site:
 * ```kotlin
 * CommandRegistry.client("mymod") {
 *     argument("count", integer(1, 64)) {
 *         executes { sendFeedback("Count: ${int("count")}") }
 *     }
 *     argument("message", greedyString()) {
 *         executes { sendFeedback(string("message")) }
 *     }
 * }
 * ```
 *
 * For argument types not covered here, pass the Brigadier/Minecraft `ArgumentType<T>` directly
 * to [com.github.mikecraft1224.simplecore.command.CommandBuilder.argument] and retrieve the value via
 * [ClientCommandContext.get].
 */

// -------------------------------------------------------------------------
// Primitive types
// -------------------------------------------------------------------------

/** Integer argument, optionally clamped to the [[min], [max]] range. */
fun integer(min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): IntegerArgumentType =
    IntegerArgumentType.integer(min, max)

/** Long argument, optionally clamped to the [[min], [max]] range. */
fun longArg(min: Long = Long.MIN_VALUE, max: Long = Long.MAX_VALUE): LongArgumentType =
    LongArgumentType.longArg(min, max)

/** Float argument, optionally clamped to the [[min], [max]] range. */
fun floatArg(min: Float = -Float.MAX_VALUE, max: Float = Float.MAX_VALUE): FloatArgumentType =
    FloatArgumentType.floatArg(min, max)

/** Double argument, optionally clamped to the [[min], [max]] range. */
fun doubleArg(min: Double = -Double.MAX_VALUE, max: Double = Double.MAX_VALUE): DoubleArgumentType =
    DoubleArgumentType.doubleArg(min, max)

/** Boolean argument - accepts `true` or `false`. */
fun bool(): BoolArgumentType = BoolArgumentType.bool()

// -------------------------------------------------------------------------
// String types
// -------------------------------------------------------------------------

/** Single-word string argument - no spaces allowed. */
fun word(): StringArgumentType = StringArgumentType.word()

/** Quoted or single-word string argument. */
fun string(): StringArgumentType = StringArgumentType.string()

/**
 * Greedy string argument - consumes the entire remainder of the input.
 * Must be the last argument in any command branch.
 */
fun greedyString(): StringArgumentType = StringArgumentType.greedyString()

// -------------------------------------------------------------------------
// Minecraft types
// -------------------------------------------------------------------------

/**
 * Resource location argument - accepts `namespace:path` identifiers.
 * Retrieve via [ClientCommandContext.identifier].
 *
 * ```kotlin
 * argument("id", identifier()) {
 *     executes { sendFeedback("ID: ${identifier("id")}") }
 * }
 * ```
 */
fun identifier(): IdentifierArgumentType = IdentifierArgumentType.identifier()

/**
 * Minecraft formatting color argument - accepts color names (`red`, `blue`, `gold`, etc.).
 * Retrieve via [ClientCommandContext.color].
 *
 * ```kotlin
 * argument("color", color()) {
 *     executes { sendFeedback("Color: ${color("color").getName()}") }
 * }
 * ```
 */
fun color(): ColorArgumentType = ColorArgumentType.color()
