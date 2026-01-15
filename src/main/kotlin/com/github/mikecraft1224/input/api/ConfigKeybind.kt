package com.github.mikecraft1224.input.api

import org.lwjgl.glfw.GLFW

/**
 * Represents a keybind configuration with a key code and optional modifier keys for virtual keybinds.
 *
 * @property keyCode The GLFW key code for the main key of the keybind. Defaults to `GLFW.GLFW_KEY_UNKNOWN`.
 * @property modifiers The modifier keys (Ctrl, Shift, Alt) associated with the keybind. Defaults to no modifiers.
 */
data class ConfigKeybind(
    var keyCode: Int = GLFW.GLFW_KEY_UNKNOWN,
    var modifiers: Modifiers = Modifiers()
)
