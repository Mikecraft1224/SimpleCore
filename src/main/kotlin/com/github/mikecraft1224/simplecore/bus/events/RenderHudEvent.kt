@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import com.github.mikecraft1224.simplecore.overlay.OverlayRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

/**
 * Fired once per frame via a persistent [net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement]
 * registered with [HudElementRegistry]. Replaces the old (pre-26.2) `HudRenderCallback`-based
 * dispatch - 26.2 moved HUD drawing to a persistent-registration model rather than a per-frame
 * callback list, so this event exists as a thin bridge back to SimpleCore's event-bus style.
 *
 * ```kotlin
 * @Feature
 * object MyHud {
 *     @Subscribe
 *     fun onRenderHud(event: RenderHudEvent) {
 *         event.state.fill(10, 10, 60, 30, 0x80000000.toInt())
 *     }
 * }
 * ```
 *
 * @property state     The [GuiGraphicsExtractor] to draw into this frame.
 * @property tickDelta Partial tick progress in [0, 1].
 */
class RenderHudEvent(
    val state: GuiGraphicsExtractor,
    val tickDelta: Float,
) : Event() {

    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return

            HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("simplecore", "render_hud_event")) { state, delta ->
                OverlayRegistry.beginFrame()
                EventRegistry.post { RenderHudEvent(state, delta.getGameTimeDeltaPartialTick(false)) }
            }

            registered = true
        }
    }
}
