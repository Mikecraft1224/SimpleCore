package com.github.mikecraft1224.input.api

import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil

/**
 * Represents the source of a key binding.
 *
 * - [Vanilla] wraps a Minecraft [KeyBinding] registered through Fabric's keybinding API.
 * - [Virtual] holds an [InputUtil.Key] that supports both keyboard keys and mouse buttons.
 */
sealed interface KeySource {
    /** A keybind backed by a vanilla [KeyBinding] registered with Fabric's keybinding API. */
    class Vanilla(val keyBinding: KeyBinding) : KeySource

    /**
     * A keybind driven by direct GLFW polling, supporting both keyboard keys
     * ([InputUtil.Type.KEYSYM]) and mouse buttons ([InputUtil.Type.MOUSE]).
     *
     * @property key The bound key or mouse button. Mutable to support runtime rebinding via
     *   [com.github.mikecraft1224.input.KeybindRegistry.updateVirtualKeybind].
     */
    class Virtual(var key: InputUtil.Key) : KeySource
}
