@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import kotlin.math.roundToInt

/**
 * SimpleCore ARGB color type. Components are 0..255.
 *
 * This class is deliberately named `Color` inside the `utils` package to avoid clashing with
 * [java.awt.Color] at the call site - import this class explicitly when both are in scope.
 */
data class Color(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255,
) {
    /** Packed ARGB int: bits 31-24 = alpha, 23-16 = red, 15-8 = green, 7-0 = blue. */
    val argb: Int get() = (a shl 24) or (r shl 16) or (g shl 8) or b

    /** Packed RGB int (alpha is ignored). */
    val rgb: Int get() = (r shl 16) or (g shl 8) or b

    /** Returns a copy with the specified alpha (0..255). */
    fun withAlpha(alpha: Int): Color = copy(a = alpha.coerceIn(0, 255))

    /** Returns a copy with the specified alpha as a 0..1 float. */
    fun withAlpha(alpha: Float): Color = withAlpha((alpha * 255f).roundToInt())

    /**
     * Returns a darker copy by multiplying each RGB channel by [factor] and clamping to 0..255.
     * Alpha is unchanged. [factor] should be in 0..1; values > 1 will lighten instead.
     */
    fun darker(factor: Double = 0.7): Color = Color(
        r = (r * factor).roundToInt().coerceIn(0, 255),
        g = (g * factor).roundToInt().coerceIn(0, 255),
        b = (b * factor).roundToInt().coerceIn(0, 255),
        a = a,
    )

    /**
     * Returns a lighter copy by multiplying each RGB channel by [factor] and clamping to 0..255.
     * Alpha is unchanged. [factor] should be > 1; values < 1 will darken instead.
     */
    fun lighter(factor: Double = 1.3): Color = Color(
        r = (r * factor).roundToInt().coerceIn(0, 255),
        g = (g * factor).roundToInt().coerceIn(0, 255),
        b = (b * factor).roundToInt().coerceIn(0, 255),
        a = a,
    )

    /**
     * Linear interpolation between this color and [other].
     * [t] = 0 returns this color, [t] = 1 returns [other].
     */
    fun blend(other: Color, t: Float): Color {
        val tc = t.coerceIn(0f, 1f)
        val rc = 1f - tc
        return Color(
            r = (r * rc + other.r * tc).roundToInt().coerceIn(0, 255),
            g = (g * rc + other.g * tc).roundToInt().coerceIn(0, 255),
            b = (b * rc + other.b * tc).roundToInt().coerceIn(0, 255),
            a = (a * rc + other.a * tc).roundToInt().coerceIn(0, 255),
        )
    }

    /** Converts to a [java.awt.Color] for interop with APIs that require it. */
    fun toAwtColor(): java.awt.Color = java.awt.Color(r, g, b, a)

    /** Returns a CSS hex string: `#RRGGBB` when fully opaque, `#RRGGBBAA` otherwise. */
    fun toHex(): String = if (a == 255) {
        "#%02X%02X%02X".format(r, g, b)
    } else {
        "#%02X%02X%02X%02X".format(r, g, b, a)
    }

    companion object {
        /**
         * Maps a Minecraft chat color code character (e.g. `'a'`, `'c'`) to the
         * closest [Color]. Returns `null` for formatting codes (bold, italic, etc.).
         */
        fun fromMinecraftCode(char: Char): Color? = when (char.lowercaseChar()) {
            '0' -> Color(0,   0,   0  )  // black
            '1' -> Color(0,   0,   170)  // dark blue
            '2' -> Color(0,   170, 0  )  // dark green
            '3' -> Color(0,   170, 170)  // dark aqua
            '4' -> Color(170, 0,   0  )  // dark red
            '5' -> Color(170, 0,   170)  // dark purple
            '6' -> Color(255, 170, 0  )  // gold
            '7' -> Color(170, 170, 170)  // gray
            '8' -> Color(85,  85,  85 )  // dark gray
            '9' -> Color(85,  85,  255)  // blue
            'a' -> Color(85,  255, 85 )  // green
            'b' -> Color(85,  255, 255)  // aqua
            'c' -> Color(255, 85,  85 )  // red
            'd' -> Color(255, 85,  255)  // light purple
            'e' -> Color(255, 255, 85 )  // yellow
            'f' -> Color(255, 255, 255)  // white
            else -> null
        }

        /** Unpacks a packed ARGB int into a [Color]. */
        fun fromArgb(argb: Int): Color = Color(
            r = (argb shr 16) and 0xFF,
            g = (argb shr  8) and 0xFF,
            b =  argb         and 0xFF,
            a = (argb shr 24) and 0xFF,
        )

        /** Unpacks a packed RGB int into a [Color] with the given [alpha]. */
        fun fromRgb(rgb: Int, alpha: Int = 255): Color = Color(
            r = (rgb shr 16) and 0xFF,
            g = (rgb shr  8) and 0xFF,
            b =  rgb         and 0xFF,
            a = alpha,
        )

        /**
         * Parses a CSS-style hex string.
         * Accepts `#RRGGBB` and `#RRGGBBAA` (leading `#` is stripped if present).
         */
        fun fromHex(hex: String): Color {
            val clean = hex.trimStart('#')
            return when (clean.length) {
                6 -> fromRgb(clean.toLong(16).toInt())
                8 -> {
                    val rgb   = clean.substring(0, 6).toLong(16).toInt()
                    val alpha = clean.substring(6, 8).toInt(16)
                    fromRgb(rgb, alpha)
                }
                else -> error("Invalid hex color string: '$hex'. Expected #RRGGBB or #RRGGBBAA.")
            }
        }

        /** Converts a [java.awt.Color] to a [Color]. */
        fun fromAwtColor(c: java.awt.Color): Color = Color(c.red, c.green, c.blue, c.alpha)

        // Named constants
        val WHITE       = Color(255, 255, 255)
        val BLACK       = Color(0,   0,   0)
        val RED         = Color(255, 0,   0)
        val GREEN       = Color(0,   255, 0)
        val BLUE        = Color(0,   0,   255)
        val YELLOW      = Color(255, 255, 0)
        val CYAN        = Color(0,   255, 255)
        val TRANSPARENT = Color(0,   0,   0,   0)
    }
}
