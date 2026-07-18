package com.github.mikecraft1224.simplecore.overlay

import com.github.mikecraft1224.simplecore.overlay.api.HudElement

/**
 * A named group of HUD overlays belonging to one mod.
 *
 * Use this instead of [HudManager] directly when your mod has multiple overlays and you want
 * players to be able to reposition only your mod's overlays without seeing every other mod's
 * elements in the same editor.
 *
 * Registration still delegates to the shared [HudManager] (all mods' elements render through
 * the same event dispatcher), but [openEditor] opens a filtered view showing only this group's
 * elements.
 *
 * ```kotlin
 * // In your mod's main object or init:
 * val OVERLAYS = HudGroup("My Mod")
 *
 * // In your loader:
 * OVERLAYS.register(KillTracker)
 * OVERLAYS.register(TimerHud)
 *
 * // From a keybind or command - HudGroup does NOT register any keybind automatically:
 * KeybindRegistry.registerVirtual("mymod.editor", KeyDescriptor.keyboard(GLFW.GLFW_KEY_F8),
 *     KeyContext.IN_GAME, onPress = { _ -> OVERLAYS.openEditor { myConfig.save() } })
 * ```
 *
 * **On keybinds:** `openEditor()` is a plain method call. Wire it to whatever trigger suits
 * your mod — a keybind, a `/mymod editor` command, or a button in your config screen.
 * For mods where the editor is rarely needed, a command is preferable to a keybind so it
 * does not occupy a slot in the player's controls screen.
 *
 * @param name Human-readable group name displayed in the editor header.
 */
class HudGroup(val name: String) {

    private val labels = mutableSetOf<String>()

    /**
     * Registers [element] with the shared [HudManager] and tracks it in this group.
     * Duplicate registrations are silently ignored.
     */
    fun register(element: HudElement) {
        HudManager.register(element)
        labels.add(element.displayName)
    }

    /**
     * Removes [element] from the shared [HudManager] and from this group.
     */
    fun unregister(element: HudElement) {
        HudManager.unregister(element)
        labels.remove(element.displayName)
    }

    /**
     * Opens the overlay editor filtered to show only this group's elements.
     *
     * @param showOnlyActive When `true`, disabled overlays (ghost handles) are hidden and
     *   only overlays that rendered in the current frame are shown. Useful for mods with many
     *   context-sensitive overlays that are inactive most of the time. Defaults to `false`.
     * @param onClose Optional callback invoked when the editor screen closes. Use it to
     *   persist config changes:
     *   ```kotlin
     *   OVERLAYS.openEditor { myConfig.save() }
     *   ```
     */
    fun openEditor(showOnlyActive: Boolean = false, onClose: (() -> Unit)? = null) {
        OverlayRegistry.openEditScreen(labels.toSet(), name, showOnlyActive, onClose)
    }
}
