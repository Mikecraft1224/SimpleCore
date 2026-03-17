package com.github.mikecraft1224.input.api

import com.github.mikecraft1224.config.KeybindPacked
import com.github.mikecraft1224.config.ProcessedConfig
import com.github.mikecraft1224.config.ProcessedEntry
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.InputUtil
import kotlin.reflect.KMutableProperty0

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

    /**
     * Links a config entry to this virtual keybind so that changing the key in the config screen
     * also updates the runtime binding immediately without a restart.
     *
     * Only works with virtual keybinds ([KeySource.Virtual]). Vanilla keybinds backed by
     * Fabric's [KeyBinding] cannot be rebound programmatically mid-session.
     *
     * Call this after [com.github.mikecraft1224.input.KeybindRegistry.registerVirtual] and after
     * [com.github.mikecraft1224.config.ConfigProcessor.process] has produced the [entry].
     *
     * @return this handle for chaining
     */
    fun bindConfigEntry(entry: ProcessedEntry.KeybindEntry): KeybindHandle {
        val virtualSource = action.source as? KeySource.Virtual
        entry.onChanged = { packed ->
            // Unpack the key code from the packed int (upper bits are modifier flags)
            val keyCode = KeybindPacked.keyCode(packed)
            virtualSource?.key = InputUtil.Type.KEYSYM.createFromCode(keyCode)
        }
        return this
    }

    /**
     * Links a config field to this virtual keybind by Kotlin property reference.
     *
     * The matching [ProcessedEntry.KeybindEntry] is located by field name inside [model],
     * so the binding is refactor-safe - no string names required.
     *
     * @param model the [ProcessedConfig] produced by [com.github.mikecraft1224.config.ConfigProcessor.process]
     * @param field a property reference on the config object (e.g. `testConfig::configKey`)
     * @return this handle for chaining
     */
    fun bindConfigField(model: ProcessedConfig, field: KMutableProperty0<Int>): KeybindHandle {
        fun walk(entries: List<ProcessedEntry>): ProcessedEntry.KeybindEntry? {
            for (e in entries) {
                if (e is ProcessedEntry.KeybindEntry && e.fieldName == field.name) return e
                if (e is ProcessedEntry.CollapsibleGroup) walk(e.children)?.let { return it }
            }
            return null
        }
        val entry = model.categories.firstNotNullOfOrNull { cat ->
            walk(cat.entries) ?: cat.subcategories.firstNotNullOfOrNull { walk(it.entries) }
        } ?: return this
        return bindConfigEntry(entry)
    }
}
