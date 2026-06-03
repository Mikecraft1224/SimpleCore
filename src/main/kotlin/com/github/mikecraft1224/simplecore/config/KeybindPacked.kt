package com.github.mikecraft1224.simplecore.config

import com.github.mikecraft1224.simplecore.input.api.Modifiers

/**
 * Packs and unpacks a GLFW key/mouse code + modifier flags into a single [Int].
 *
 * Layout:
 * - Bits 0-15 : GLFW key code or mouse button index
 * - Bit 16    : Ctrl
 * - Bit 17    : Shift
 * - Bit 18    : Alt
 * - Bit 19    : Mouse flag (1 = mouse button, 0 = keyboard key)
 *
 * Example:
 * ```kotlin
 * val packed = KeybindPacked.pack(GLFW.GLFW_KEY_S, Modifiers(ctrl = true))
 * val key    = KeybindPacked.keyCode(packed)    // GLFW.GLFW_KEY_S
 * val mods   = KeybindPacked.modifiers(packed)  // Modifiers(ctrl = true)
 *
 * val mouse  = KeybindPacked.packMouse(GLFW.GLFW_MOUSE_BUTTON_4)
 * KeybindPacked.isMouse(mouse)                  // true
 * ```
 */
object KeybindPacked {
    private const val KEY_MASK   = 0x0000FFFF
    private const val CTRL_BIT   = 1 shl 16
    private const val SHIFT_BIT  = 1 shl 17
    private const val ALT_BIT    = 1 shl 18
    private const val MOUSE_BIT  = 1 shl 19

    /** Packs a keyboard [keyCode] and [modifiers] into a single int. */
    fun pack(keyCode: Int, modifiers: Modifiers = Modifiers()): Int {
        var v = keyCode and KEY_MASK
        if (modifiers.ctrl)  v = v or CTRL_BIT
        if (modifiers.shift) v = v or SHIFT_BIT
        if (modifiers.alt)   v = v or ALT_BIT
        return v
    }

    /** Packs a mouse [button] index and [modifiers] into a single int. */
    fun packMouse(button: Int, modifiers: Modifiers = Modifiers()): Int =
        pack(button, modifiers) or MOUSE_BIT

    /** Returns `true` if [packed] represents a mouse button rather than a keyboard key. */
    fun isMouse(packed: Int): Boolean = (packed and MOUSE_BIT) != 0

    /** Extracts the GLFW key code or mouse button index from a packed value. */
    fun keyCode(packed: Int): Int = packed and KEY_MASK

    /** Extracts the [Modifiers] from a packed value. */
    fun modifiers(packed: Int) = Modifiers(
        ctrl  = (packed and CTRL_BIT)  != 0,
        shift = (packed and SHIFT_BIT) != 0,
        alt   = (packed and ALT_BIT)   != 0,
    )
}
