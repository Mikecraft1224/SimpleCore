@file:Suppress("DEPRECATION", "unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import com.github.mikecraft1224.simplecore.overlay.OverlayRegistry
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

/**
 * Fired once per frame during the Minecraft HUD render pass.
 *
 * Subscribe to this event to render content on the in-game HUD (health bar layer, not world).
 * Use [com.github.mikecraft1224.simplecore.overlay.api.renderAt] on an
 * [com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition] to render a repositionable overlay.
 *
 * ```kotlin
 * @Subscribe
 * fun onHud(event: RenderHudEvent) {
 *     position.renderAt(event.ctx, event.screenWidth, event.screenHeight, "My HUD") { ctx ->
 *         ctx.drawText(textRenderer, "Hello", 0, 0, 0xFFFFFF, true)
 *         OverlaySize(60, 9)
 *     }
 * }
 * ```
 */
class RenderHudEvent(
    val ctx: DrawContext,
    val tickDelta: Float,
    val screenWidth: Int,
    val screenHeight: Int,
    /** Whether a screen is currently open. Use to conditionally show/hide HUD elements. */
    val phase: Phase = Phase.GAME_OVERLAY,
) : Event() {

    enum class Phase {
        /** Fired when no GUI screen is open (normal in-game view). */
        GAME_OVERLAY,
        /** Fired when a GUI screen is open (inventory, chest, etc.). */
        IN_INVENTORY,
    }

    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return
            // TODO: HudRenderCallback is deprecated. Migrate to the layer-based HUD API
            //  (HudLayerRegistrationCallback or equivalent) when bumping Fabric API.
            HudRenderCallback.EVENT.register { drawContext, tickCounter ->
                val client = MinecraftClient.getInstance()
                val phase = if (client.currentScreen != null) Phase.IN_INVENTORY else Phase.GAME_OVERLAY
                OverlayRegistry.beginFrame()
                EventRegistry.post {
                    RenderHudEvent(
                        drawContext,
                        tickCounter.getTickProgress(true),
                        client.window.scaledWidth,
                        client.window.scaledHeight,
                        phase,
                    )
                }
            }
            registered = true
        }
    }
}
