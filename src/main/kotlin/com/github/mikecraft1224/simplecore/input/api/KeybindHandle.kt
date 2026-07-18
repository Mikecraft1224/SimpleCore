@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.input.api

import com.github.mikecraft1224.simplecore.config.KeybindPacked
import com.github.mikecraft1224.simplecore.config.ProcessedConfig
import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

/**
 * An opaque handle to a registered keybind, obtained from
 * [com.github.mikecraft1224.simplecore.input.KeybindRegistry.registerVanilla] or
 * [com.github.mikecraft1224.simplecore.input.KeybindRegistry.registerVirtual].
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
     * Companion object that tracks all handles that have a pending config binding set via
     * [withConfigBinding]. Used by [ProcessedConfig.applyKeybindBindings] to wire bindings
     * automatically without requiring the caller to hold the handle variable.
     */
    companion object {
        /**
         * Global set of handles that carry a pending config-field binding.
         * Populated by [withConfigBinding]; consumed (and entries applied) by
         * [ProcessedConfig.applyKeybindBindings].
         */
        internal val pendingConfigBindings: MutableList<Pair<KeybindHandle, KProperty0<*>>> =
            mutableListOf()
    }

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
        if (action.pressed) action.release(Minecraft.getInstance())
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
     * Fabric's [net.minecraft.client.option.KeyBinding] cannot be rebound programmatically mid-session.
     *
     * Call this after [com.github.mikecraft1224.simplecore.input.KeybindRegistry.registerVirtual] and after
     * [com.github.mikecraft1224.simplecore.config.ConfigProcessor.process] has produced the [entry].
     *
     * @return this handle for chaining
     */
    fun bindConfigEntry(entry: ProcessedEntry.KeybindEntry): KeybindHandle {
        val virtualSource = action.source as? KeySource.Virtual
        entry.onChanged = { packed ->
            val keyCode = KeybindPacked.keyCode(packed)
            val mods = KeybindPacked.modifiers(packed)
            virtualSource?.key = if (KeybindPacked.isMouse(packed))
                InputConstants.Type.MOUSE.getOrCreate(keyCode)
            else
                InputConstants.Type.KEYSYM.getOrCreate(keyCode)
            action.modifiers = mods
        }
        return this
    }

    /**
     * Links a config field to this virtual keybind by Kotlin property reference.
     *
     * The matching [ProcessedEntry.KeybindEntry] is located by field name inside [model],
     * so the binding is refactor-safe - no string names required.
     *
     * @param model the [ProcessedConfig] produced by [com.github.mikecraft1224.simplecore.config.ConfigProcessor.process]
     * @param field a property reference on the config object (e.g. `testConfig::configKey`)
     * @return this handle for chaining
     */
    fun bindConfigField(model: ProcessedConfig, field: KMutableProperty0<Int>): KeybindHandle =
        bindConfigFieldByName(model, field.name)

    /**
     * Finds the [ProcessedEntry.KeybindEntry] matching [fieldName] inside [model] and wires
     * up [bindConfigEntry].
     *
     * @return this handle for chaining
     */
    fun bindConfigFieldByName(model: ProcessedConfig, fieldName: String): KeybindHandle {
        fun walk(entries: List<ProcessedEntry>): ProcessedEntry.KeybindEntry? {
            for (e in entries) {
                if (e is ProcessedEntry.KeybindEntry && e.fieldName == fieldName) return e
                if (e is ProcessedEntry.CollapsibleGroup) walk(e.children)?.let { return it }
            }
            return null
        }
        val entry = model.categories.firstNotNullOfOrNull { cat ->
            walk(cat.entries) ?: cat.subcategories.firstNotNullOfOrNull { walk(it.entries) }
        } ?: return this
        return bindConfigEntry(entry)
    }

    /**
     * Stores a config property reference on this handle so the binding can be applied
     * automatically when [ProcessedConfig.applyKeybindBindings] is called.
     *
     * ```kotlin
     * KeybindRegistry.registerVirtual(
     *     id   = "mymod.open_config",
     *     key  = KeyDescriptor.keyboard(config.configKey.keyCode),
     *     KeyContext.IN_GAME,
     *     onPress = { client ->
     *         val model = ConfigProcessor.process(config).applyKeybindBindings()
     *         client.setScreen(ConfigScreen(null, model, manager))
     *     },
     * ).withConfigBinding(config::configKey)
     * ```
     *
     * @param property property reference whose name matches a [com.github.mikecraft1224.simplecore.config.api.values.Keybind]
     *                 field in the config object.
     * @return this handle for chaining
     */
    fun withConfigBinding(property: KProperty0<*>): KeybindHandle {
        // Remove any previous binding for this handle (defensive; normally called once)
        pendingConfigBindings.removeIf { it.first === this }
        pendingConfigBindings.add(this to property)
        return this
    }
}

/**
 * Applies all pending keybind-config bindings that were registered via
 * [KeybindHandle.withConfigBinding] and whose field name matches a
 * [ProcessedEntry.KeybindEntry] in this model.
 *
 * Call this immediately after [com.github.mikecraft1224.simplecore.config.ConfigProcessor.process] to
 * ensure every keybind that was registered with [KeybindHandle.withConfigBinding] is wired
 * up to its config entry automatically - no stored handle variable required.
 *
 * ```kotlin
 * val model = ConfigProcessor.process(config).applyKeybindBindings()
 * client.setScreen(ConfigScreen(null, model, manager))
 * ```
 *
 * @return this [ProcessedConfig] for fluent chaining
 */
fun ProcessedConfig.applyKeybindBindings(): ProcessedConfig {
    for ((handle, property) in KeybindHandle.pendingConfigBindings) {
        handle.bindConfigFieldByName(this, property.name)
    }
    return this
}
