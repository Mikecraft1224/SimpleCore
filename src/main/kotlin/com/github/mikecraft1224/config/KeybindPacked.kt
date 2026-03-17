package com.github.mikecraft1224.config

import com.github.mikecraft1224.input.api.Modifiers

/**
 * Packs and unpacks a GLFW key code + modifier flags into a single [Int].
 *
 * Layout:
 * - Bits 0-15 : GLFW key code
 * - Bit 16    : Ctrl
 * - Bit 17    : Shift
 * - Bit 18    : Alt
 *
 * Example:
 * ```kotlin
 * val packed = KeybindPacked.pack(GLFW.GLFW_KEY_S, Modifiers(ctrl = true))
 * val key    = KeybindPacked.keyCode(packed)    // GLFW.GLFW_KEY_S
 * val mods   = KeybindPacked.modifiers(packed)  // Modifiers(ctrl = true)
 * ```
 */
object KeybindPacked {
    private const val KEY_MASK  = 0x0000FFFF
    private const val CTRL_BIT  = 1 shl 16
    private const val SHIFT_BIT = 1 shl 17
    private const val ALT_BIT   = 1 shl 18

    /** Packs [keyCode] and [modifiers] into a single int. */
    fun pack(keyCode: Int, modifiers: Modifiers = Modifiers()): Int {
        var v = keyCode and KEY_MASK
        if (modifiers.ctrl)  v = v or CTRL_BIT
        if (modifiers.shift) v = v or SHIFT_BIT
        if (modifiers.alt)   v = v or ALT_BIT
        return v
    }

    /** Extracts the GLFW key code from a packed value. */
    fun keyCode(packed: Int): Int = packed and KEY_MASK

    /** Extracts the [Modifiers] from a packed value. */
    fun modifiers(packed: Int) = Modifiers(
        ctrl  = (packed and CTRL_BIT)  != 0,
        shift = (packed and SHIFT_BIT) != 0,
        alt   = (packed and ALT_BIT)   != 0,
    )
}
