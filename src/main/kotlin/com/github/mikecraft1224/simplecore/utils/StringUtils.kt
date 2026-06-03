@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.client.MinecraftClient

private val COLOR_CODE_REGEX = Regex("§[0-9a-fk-orA-FK-OR]")
private val PLAYER_NAME_REGEX = Regex("[A-Za-z0-9_]{3,16}")

fun String.removeColorCodes(): String = COLOR_CODE_REGEX.replace(this, "")

fun String.stripFormattingCodes(): String = removeColorCodes()

fun String.isPlayerName(): Boolean = PLAYER_NAME_REGEX.matches(this)

fun String.widthInPixels(): Int =
    MinecraftClient.getInstance().textRenderer.getWidth(this)

fun String.splitToWidth(maxWidth: Int): List<String> {
    val tr = MinecraftClient.getInstance().textRenderer
    val words = split(" ")
    val lines = mutableListOf<String>()
    val current = StringBuilder()

    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (tr.getWidth(candidate) <= maxWidth) {
            if (current.isNotEmpty()) current.append(' ')
            current.append(word)
        } else {
            if (current.isNotEmpty()) lines.add(current.toString())
            current.clear()
            current.append(word)
        }
    }
    if (current.isNotEmpty()) lines.add(current.toString())
    return lines
}
