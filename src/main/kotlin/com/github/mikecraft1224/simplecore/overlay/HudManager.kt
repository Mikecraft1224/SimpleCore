@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.overlay

import com.github.mikecraft1224.simplecore.bus.api.Feature
import com.github.mikecraft1224.simplecore.bus.api.Subscribe
import com.github.mikecraft1224.simplecore.bus.events.HudMouseClickEvent
import com.github.mikecraft1224.simplecore.bus.events.RenderHudEvent
import com.github.mikecraft1224.simplecore.overlay.api.HudElement
import com.github.mikecraft1224.simplecore.overlay.api.PendingTooltip
import net.minecraft.client.Minecraft

/**
 * Central dispatcher for all [HudElement] instances.
 *
 * Automatically picked up and registered with the event bus by
 * [com.github.mikecraft1224.simplecore.bus.FeatureAutoLoader] - no manual bus registration needed.
 * [RenderHudEvent] is lazily hooked up the first time a handler for it exists.
 *
 * Call [register] from your mod's initializer (or any loader class) to activate a [HudElement]:
 *
 * ```kotlin
 * HudManager.register(MyHud)
 * ```
 */
@Feature
object HudManager {

    private val elements = mutableListOf<HudElement>()

    /**
     * Adds [element] to the render loop. Safe to call multiple times;
     * duplicate registrations are silently ignored.
     *
     * Also seeds the element into [OverlayRegistry] so it appears in the drag editor
     * immediately, even before its first rendered frame.
     */
    fun register(element: HudElement) {
        if (element !in elements) {
            elements.add(element)
            OverlayRegistry.seed(element.displayName, element.position) { element.resetToDefault() }
        }
    }

    /**
     * Removes [element] from the render loop and from the overlay editor.
     */
    fun unregister(element: HudElement) {
        elements.remove(element)
        OverlayRegistry.unregisterElement(element.displayName)
    }

    /**
     * Opens the interactive overlay position editor showing all registered overlays.
     *
     * For a mod-scoped editor that shows only one mod's overlays, use [HudGroup.openEditor]
     * instead.
     *
     * @param showOnlyActive When `true`, ghost/disabled entries are hidden; only overlays that
     *   rendered this frame are shown. Defaults to `false`.
     * @param onClose Optional callback invoked when the editor screen closes. Use it to
     *   persist config changes:
     *   ```kotlin
     *   HudManager.openEditor { myConfig.save() }
     *   ```
     */
    fun openEditor(showOnlyActive: Boolean = false, onClose: (() -> Unit)? = null) {
        OverlayRegistry.openEditScreen(showOnlyActive = showOnlyActive, onClose = onClose)
    }

    @Subscribe
    fun onRenderHud(event: RenderHudEvent) {
        PendingTooltip.clear()
        for (element in elements) {
            element.beginFrame()
            element.renderFrame(event)
        }
        val client = Minecraft.getInstance()
        val mx = client.mouseHandler.getScaledXPos(client.window).toInt()
        val my = client.mouseHandler.getScaledYPos(client.window).toInt()
        PendingTooltip.renderLast(event.state, mx, my, event.state.guiWidth())
    }

    @Subscribe
    fun onHudMouseClick(event: HudMouseClickEvent) {
        for (element in elements) {
            if (element.routeMouseClicked(event.mx, event.my, event.button)) break
        }
    }
}
