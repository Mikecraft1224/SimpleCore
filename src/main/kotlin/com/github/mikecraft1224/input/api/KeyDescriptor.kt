package com.github.mikecraft1224.input.api

import net.minecraft.client.util.InputUtil

/**
 * Describes a key or mouse button binding together with its required modifier keys.
 *
 * Use the [keyboard] or [mouse] companion factories for the common cases, or construct directly
 * with a specific [InputUtil.Key] when you already have one (e.g. loaded from a config).
 *
 * @property key The bound key or mouse button. Defaults to [InputUtil.UNKNOWN_KEY].
 * @property modifiers Required modifier keys (Ctrl, Shift, Alt) for this binding to activate.
 *
 * ### Example
 * ```kotlin
 * // Plain keyboard key
 * val zoomKey = KeyDescriptor.keyboard(GLFW.GLFW_KEY_C)
 *
 * // Keyboard key with modifier
 * val saveKey = KeyDescriptor.keyboard(GLFW.GLFW_KEY_S, Modifiers(ctrl = true))
 *
 * // Mouse button
 * val sideButton = KeyDescriptor.mouse(GLFW.GLFW_MOUSE_BUTTON_4)
 * ```
 */
data class KeyDescriptor(
    var key: InputUtil.Key = InputUtil.UNKNOWN_KEY,
    var modifiers: Modifiers = Modifiers(),
) {
    companion object {
        /**
         * Creates a [KeyDescriptor] for a keyboard key identified by its GLFW key code.
         *
         * @param keyCode A GLFW key constant such as `GLFW.GLFW_KEY_C`.
         * @param modifiers Required modifier keys. Defaults to no modifiers.
         */
        fun keyboard(keyCode: Int, modifiers: Modifiers = Modifiers()): KeyDescriptor =
            KeyDescriptor(InputUtil.Type.KEYSYM.createFromCode(keyCode), modifiers)

        /**
         * Creates a [KeyDescriptor] for a mouse button identified by its GLFW button index.
         *
         * @param button A GLFW mouse button constant such as `GLFW.GLFW_MOUSE_BUTTON_4`.
         * @param modifiers Required modifier keys. Defaults to no modifiers.
         */
        fun mouse(button: Int, modifiers: Modifiers = Modifiers()): KeyDescriptor =
            KeyDescriptor(InputUtil.Type.MOUSE.createFromCode(button), modifiers)
    }
}