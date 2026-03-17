package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a keybind in the config GUI.
 *
 * The field must be of type [Int]. The stored value packs the GLFW key code
 * and modifier flags into a single int using [com.github.mikecraft1224.config.KeybindPacked]:
 * - Bits 0-15: GLFW key code
 * - Bit 16: Ctrl modifier
 * - Bit 17: Shift modifier
 * - Bit 18: Alt modifier
 *
 * Use [com.github.mikecraft1224.config.KeybindPacked.keyCode] and
 * [com.github.mikecraft1224.config.KeybindPacked.modifiers] to unpack the stored value.
 *
 * To link this keybind entry to a virtual keybind so that changes in the config screen
 * take effect immediately at runtime, call [com.github.mikecraft1224.input.api.KeybindHandle.bindConfigEntry]
 * on the [com.github.mikecraft1224.input.api.KeybindHandle] returned by
 * [com.github.mikecraft1224.input.KeybindRegistry.registerVirtual].
 *
 * @param defaultKey The default GLFW key code for the keybind (no modifiers by default).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorKeybind(
    val defaultKey: Int,
)
