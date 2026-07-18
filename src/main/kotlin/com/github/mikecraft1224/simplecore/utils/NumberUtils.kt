@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.roundToLong

private val ROMAN_NUMERALS = listOf(
    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
    100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
    10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
)

private val SHORT_FORMAT_SUFFIXES = listOf("" to 1L, "K" to 1_000L, "M" to 1_000_000L, "B" to 1_000_000_000L, "T" to 1_000_000_000_000L)

/** Rounds this value to [decimals] decimal places. */
fun Double.roundTo(decimals: Int): Double {
    val factor = 10.0.pow(decimals)
    return (this * factor).roundToLong() / factor
}

/** Formats this value with a fixed number of [decimals], e.g. `3.14159.toFixed(2)` -> `"3.14"`. */
fun Double.toFixed(decimals: Int): String = "%.${decimals}f".format(this)

/** Formats a large number with a K/M/B/T suffix, e.g. `1_234_567L.shortFormat()` -> `"1.23M"`. */
fun Long.shortFormat(decimals: Int = 2): String {
    val absValue = kotlin.math.abs(this)
    val (suffix, divisor) = SHORT_FORMAT_SUFFIXES.lastOrNull { absValue >= it.second } ?: SHORT_FORMAT_SUFFIXES.first()
    if (divisor == 1L) return toString()
    return "${(this.toDouble() / divisor).toFixed(decimals)}$suffix"
}

fun Int.shortFormat(decimals: Int = 2): String = toLong().shortFormat(decimals)
fun Double.shortFormat(decimals: Int = 2): String = roundToLong().shortFormat(decimals)

/** Inserts thousands separators, e.g. `1234567L.addSeparators()` -> `"1,234,567"`. */
fun Long.addSeparators(): String = DecimalFormat("#,###").format(this)
fun Int.addSeparators(): String = toLong().addSeparators()

/** Converts to a roman numeral string. Only supports 1..3999. */
fun Int.toRomanNumeral(): String {
    require(this in 1..3999) { "Roman numerals only support 1..3999, got $this" }
    var remaining = this
    val sb = StringBuilder()
    for ((value, symbol) in ROMAN_NUMERALS) {
        while (remaining >= value) {
            sb.append(symbol)
            remaining -= value
        }
    }
    return sb.toString()
}
