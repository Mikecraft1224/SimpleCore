package com.github.mikecraft1224.input.api

import net.minecraft.client.MinecraftClient

/**
 * An opaque handle to a registered keybind, obtained from
 * [com.github.mikecraft1224.input.KeybindRegistry.registerVanilla] or
 * [com.github.mikecraft1224.input.KeybindRegistry.registerVirtual].
 *
 * Use this handle to inspect the underlying [action], unregister the keybind at runtime,
 * or temporarily suppress it without fully removing it from the registry.
 *
 * ### Vanilla keybind caveat
 * Calling [unregister] on a vanilla keybind removes it from SimpleCore's dispatch loop and
 * prevents all callbacks from firing, but does **not** remove it from Minecraft's keybinding
 * options screen. Fabric's keybinding API provides no mechanism for that. The `KeyBinding`
 * object will continue to appear in the options menu for the remainder of the session.
 *
 * ### Example
 * ```kotlin
 * val zoomHandle = KeybindRegistry.registerVirtual("mymod.zoom", KeyDescriptor.keyboard(GLFW.GLFW_KEY_C)) {
 *     onPress = { client -> startZoom(client) }
 *     onRelease = { client -> stopZoom(client) }
 * }
 *
 * // Later, when the feature is disabled:
 * zoomHandle.unregister()
 * ```
 *
 * @property action The underlying [KeyAction] for read-only inspection (e.g. checking [KeyAction.pressed]).
 */
class KeybindHandle internal constructor(
    val action: KeyAction,
    private val removeFromRegistry: () -> Unit,
) {
    /**
     * Whether this handle is still registered in the keybind registry.
     * Becomes `false` after [unregister] is called.
     */
    @Volatile
    var isRegistered: Boolean = true
        private set

    /**
     * Removes this keybind from the registry and releases it if currently pressed.
     *
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    fun unregister() {
        if (!isRegistered) return
        isRegistered = false
        if (action.pressed) action.release(MinecraftClient.getInstance())
        removeFromRegistry()
    }

    /**
     * Temporarily suppresses this keybind without removing it from the registry.
     *
     * While blocked the keybind will not fire any callbacks. Any currently pressed state
     * is released on the next tick. Call [unblock] to resume normal operation.
     */
    fun block() {
        action.individuallyBlocked = true
    }

    /**
     * Resumes a keybind that was previously suppressed with [block].
     */
    fun unblock() {
        action.individuallyBlocked = false
    }
}
