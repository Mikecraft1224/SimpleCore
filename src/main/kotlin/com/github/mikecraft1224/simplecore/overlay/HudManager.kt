@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.overlay

import com.github.mikecraft1224.simplecore.bus.api.Feature
import com.github.mikecraft1224.simplecore.bus.api.Subscribe
import com.github.mikecraft1224.simplecore.bus.events.HudMouseClickEvent
import com.github.mikecraft1224.simplecore.bus.events.RenderHudEvent
import com.github.mikecraft1224.simplecore.overlay.api.HudElement
import com.github.mikecraft1224.simplecore.overlay.api.PendingTooltip
import net.minecraft.client.MinecraftClient

/**
 * Central dispatcher for all [HudElement] instances.
 *
 * Automatically picked up and registered with the event bus by
 * [com.github.mikecraft1224.simplecore.bus.FeatureAutoLoader] — no manual bus registration needed.
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
     */
    fun register(element: HudElement) {
        if (element !in elements) elements.add(element)
    }

    /**
     * Removes [element] from the render loop.
     */
    fun unregister(element: HudElement) {
        elements.remove(element)
    }

    /**
     * Opens the interactive overlay position editor.
     *
     * Call this from a keybind handler or command. The optional [onClose] callback is invoked
     * when the editor screen is closed — use it to persist config changes:
     *
     * ```kotlin
     * KeybindRegistry.registerVirtual("mymod.editor", KeyDescriptor.keyboard(GLFW.GLFW_KEY_F7),
     *     KeyContext.IN_GAME, onPress = { _ -> HudManager.openEditor { myConfig.save() } })
     * ```
     */
    fun openEditor(onClose: (() -> Unit)? = null) {
        OverlayRegistry.openEditScreen(onClose)
    }

    @Subscribe
    fun onRenderHud(event: RenderHudEvent) {
        PendingTooltip.clear()
        for (element in elements) {
            element.beginFrame()
            element.renderFrame(event)
        }
        val client = MinecraftClient.getInstance()
        val scale = client.window.scaleFactor
        val mx = (client.mouse.x / scale).toInt()
        val my = (client.mouse.y / scale).toInt()
        PendingTooltip.renderLast(event.ctx, mx, my, event.screenWidth)
    }

    @Subscribe
    fun onHudMouseClick(event: HudMouseClickEvent) {
        for (element in elements) {
            if (element.routeMouseClicked(event.mx, event.my, event.button)) break
        }
    }
}
