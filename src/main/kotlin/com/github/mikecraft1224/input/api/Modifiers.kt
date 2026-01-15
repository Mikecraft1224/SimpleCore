package com.github.mikecraft1224.input.api

import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

/**
 * Represents a combination of modifier keys (Ctrl, Shift, Alt).
 *
 * @property ctrl Whether the Ctrl key is required.
 * @property shift Whether the Shift key is required.
 * @property alt Whether the Alt key is required.
 */
data class Modifiers(
    val ctrl: Boolean = false,
    val shift: Boolean = false,
    val alt: Boolean = false,
) {
    fun matches(win: Long): Boolean {
        if (win == 0L) return false
        fun down(code: Int) = InputUtil.isKeyPressed(win, code)
        if (ctrl  && !(down(GLFW.GLFW_KEY_LEFT_CONTROL) || down(GLFW.GLFW_KEY_RIGHT_CONTROL))) return false
        if (shift && !(down(GLFW.GLFW_KEY_LEFT_SHIFT)   || down(GLFW.GLFW_KEY_RIGHT_SHIFT)))   return false
        if (alt   && !(down(GLFW.GLFW_KEY_LEFT_ALT)     || down(GLFW.GLFW_KEY_RIGHT_ALT)))     return false
        return true
    }

    fun matchesMask(mask: Int): Boolean {
        fun has(bit: Int) = (mask and bit) != 0
        if (ctrl  && !has(GLFW.GLFW_MOD_CONTROL)) return false
        if (shift && !has(GLFW.GLFW_MOD_SHIFT))   return false
        if (alt   && !has(GLFW.GLFW_MOD_ALT))     return false
        return true
    }
}
