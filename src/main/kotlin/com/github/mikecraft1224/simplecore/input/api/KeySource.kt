package com.github.mikecraft1224.simplecore.input.api

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping

/**
 * Represents the source of a key binding.
 *
 * - [Vanilla] wraps a Minecraft [KeyMapping] registered through Fabric's keybinding API.
 * - [Virtual] holds an [InputConstants.Key] that supports both keyboard keys and mouse buttons.
 */
sealed interface KeySource {
    /** A keybind backed by a vanilla [KeyMapping] registered with Fabric's keybinding API. */
    class Vanilla(val keyBinding: KeyMapping) : KeySource

    /**
     * A keybind driven by direct GLFW polling, supporting both keyboard keys
     * ([InputConstants.Type.KEYSYM]) and mouse buttons ([InputConstants.Type.MOUSE]).
     *
     * @property key The bound key or mouse button. Mutable to support runtime rebinding via
     *   [com.github.mikecraft1224.simplecore.input.KeybindRegistry.updateVirtualKeybind].
     */
    class Virtual(var key: InputConstants.Key) : KeySource
}
