package com.github.mikecraft1224.simplecore.config.api.values

/**
 * A config field value type representing a keyboard or mouse keybind.
 *
 * Assign a [Keybind] to a config field - the config processor infers the entry type
 * automatically from the field type.
 *
 * ```kotlin
 * @Entry(description = "Key to open the config screen")
 * var configKey = Keybind(GLFW.GLFW_KEY_INSERT)
 *
 * // With modifiers:
 * @Entry("Special action")
 * var specialKey = Keybind.of(GLFW.GLFW_KEY_G, ctrl = true)
 * ```
 *
 * The value is packed into a single [Int] (bits 0-15: GLFW key code, bit 16: Ctrl,
 * bit 17: Shift, bit 18: Alt) and serialized as-is, matching the format used by
 * [com.github.mikecraft1224.simplecore.config.KeybindPacked].
 *
 * @param packed The initial packed keybind value (GLFW key/mouse code + modifier bits).
 */
class Keybind(var packed: Int = 0) {

    /** Captures the value at construction time so the config screen can offer a reset. */
    val defaultPacked: Int = packed

    companion object {
        /**
         * Creates a [Keybind] from a GLFW key code and optional modifier flags.
         * ```kotlin
         * val kb = Keybind.of(GLFW.GLFW_KEY_G, ctrl = true)
         * ```
         */
        fun of(
            keyCode: Int,
            ctrl: Boolean = false,
            shift: Boolean = false,
            alt: Boolean = false,
        ): Keybind {
            var p = keyCode and 0xFFFF
            if (ctrl)  p = p or (1 shl 16)
            if (shift) p = p or (1 shl 17)
            if (alt)   p = p or (1 shl 18)
            return Keybind(p)
        }

        /**
         * Creates a [Keybind] from a GLFW mouse button index and optional modifier flags.
         * ```kotlin
         * val kb = Keybind.mouse(GLFW.GLFW_MOUSE_BUTTON_4)
         * ```
         */
        fun mouse(button: Int, ctrl: Boolean = false, shift: Boolean = false, alt: Boolean = false): Keybind {
            var p = button and 0xFFFF
            if (ctrl)  p = p or (1 shl 16)
            if (shift) p = p or (1 shl 17)
            if (alt)   p = p or (1 shl 18)
            p = p or (1 shl 19)
            return Keybind(p)
        }
    }

    /** Whether this keybind is a mouse button (rather than a keyboard key). */
    val isMouse: Boolean get() = (packed shr 19) and 1 != 0

    /** The raw GLFW key code or mouse button index extracted from [packed]. */
    val keyCode: Int get() = packed and 0xFFFF

    /** Whether the Ctrl modifier is set. */
    val ctrl: Boolean get() = (packed shr 16) and 1 != 0

    /** Whether the Shift modifier is set. */
    val shift: Boolean get() = (packed shr 17) and 1 != 0

    /** Whether the Alt modifier is set. */
    val alt: Boolean get() = (packed shr 18) and 1 != 0
}
