package com.github.mikecraft1224.simplecore.command.api

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

/**
 * A Brigadier [ArgumentType] that parses a Kotlin/Java enum by name with automatic
 * case-insensitive tab completion.
 *
 * Create instances via the companion object factories or the [enumArg] top-level function:
 * ```kotlin
 * enum class GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }
 *
 * argument("mode", enumArg<GameMode>()) {
 *     executes {
 *         val mode = get<GameMode>("mode")
 *         sendFeedback("Mode: $mode")
 *     }
 * }
 * ```
 *
 * Tab completion offers all enum names, filtered case-insensitively by the player's current
 * input. Parsing is also case-insensitive by default.
 *
 * ### Factory variants
 * | Factory | Name used in tab completion / parsing |
 * |---------|---------------------------------------|
 * | `EnumArgumentType.lowercase<E>()` | `survival`, `creative` … |
 * | `EnumArgumentType.name<E>()` | `SURVIVAL`, `CREATIVE` … (Kotlin default) |
 * | `EnumArgumentType.custom<E> { it.displayName }` | whatever [toString] returns |
 *
 * The [enumArg] shorthand uses `lowercase<E>()`.
 */
class EnumArgumentType<E : Enum<E>> @PublishedApi internal constructor(
    clazz: Class<E>,
    private val toString: (E) -> String,
) : ArgumentType<E> {

    /** Maps lower-cased name -> enum constant. Built once at construction time. */
    private val mapping: Map<String, E> = clazz.enumConstants.associateBy { toString(it).lowercase() }

    private val unknownValueException = DynamicCommandExceptionType { input ->
        LiteralMessage("Unknown value '$input'. Expected one of: ${mapping.keys.joinToString(", ")}")
    }

    override fun parse(reader: StringReader): E {
        val input = reader.readString()
        return mapping[input.lowercase()]
            ?: throw unknownValueException.createWithContext(reader, input)
    }

    override fun <S : Any> listSuggestions(
        context: CommandContext<S>,
        builder: SuggestionsBuilder,
    ): CompletableFuture<Suggestions> {
        val remaining = builder.remainingLowerCase
        mapping.keys
            .filter { it.startsWith(remaining) }
            .forEach { builder.suggest(it) }
        return builder.buildFuture()
    }

    companion object {
        /**
         * Creates an [EnumArgumentType] where enum constants are identified by their lowercase
         * [Enum.name] - e.g. `SURVIVAL` is matched and suggested as `survival`.
         */
        inline fun <reified E : Enum<E>> lowercase(): EnumArgumentType<E> =
            EnumArgumentType(E::class.java) { it.name.lowercase() }

        /**
         * Creates an [EnumArgumentType] where enum constants are identified by their exact
         * [Enum.name] - e.g. `SURVIVAL` is matched and suggested as `SURVIVAL`.
         */
        inline fun <reified E : Enum<E>> name(): EnumArgumentType<E> =
            EnumArgumentType(E::class.java) { it.name }

        /**
         * Creates an [EnumArgumentType] with a custom string representation.
         *
         * The [toString] function must return a whitespace-free string for each constant.
         * Matching during parsing is case-insensitive regardless of what [toString] returns.
         *
         * ```kotlin
         * enum class Difficulty(val label: String) {
         *     PEACEFUL("peaceful"), EASY("easy"), NORMAL("normal"), HARD("hard");
         * }
         *
         * EnumArgumentType.custom<Difficulty> { it.label }
         * ```
         */
        inline fun <reified E : Enum<E>> custom(noinline toString: (E) -> String): EnumArgumentType<E> =
            EnumArgumentType(E::class.java, toString)
    }
}

/**
 * Shorthand for [EnumArgumentType.lowercase] - the most common variant.
 *
 * ```kotlin
 * argument("mode", enumArg<GameMode>()) {
 *     executes { sendFeedback("${get<GameMode>("mode")}") }
 * }
 * ```
 */
inline fun <reified E : Enum<E>> enumArg(): EnumArgumentType<E> = EnumArgumentType.lowercase()
