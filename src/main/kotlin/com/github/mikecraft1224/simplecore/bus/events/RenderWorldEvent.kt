@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft

/**
 * Fired once per frame, immediately before vanilla submits its own gizmo collection for the level
 * render pass. Handlers draw world-space debug/utility shapes via [net.minecraft.gizmos.Gizmos] -
 * see the `drawXxx` extension functions in `WorldRenderShapes.kt`/`WorldRenderUtils.kt`.
 *
 * Unlike the pre-26.2 version of this event, there is no matrices/vertex-consumer-provider or
 * priority queue here: [net.minecraft.gizmos.Gizmos] calls take plain world-space [net.minecraft.world.phys.Vec3]
 * positions (vanilla handles the camera-relative math internally) and vanilla's own gizmo
 * collector handles draw ordering, so handlers just call `Gizmos.*` directly.
 *
 * ```kotlin
 * @Feature
 * object WorldRenderer {
 *     @Subscribe
 *     fun onRender(event: RenderWorldEvent) {
 *         if (!McUtils.isInGame) return
 *         event.drawFilledBox(someBox, Color(255, 0, 0, 128))
 *     }
 * }
 * ```
 *
 * @property camera     Current camera; use `camera.position()` for the eye position.
 * @property tickDelta  Partial tick progress in [0, 1] for smooth position interpolation.
 */
class RenderWorldEvent(
    val camera: Camera,
    val tickDelta: Float,
) : Event() {

    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return

            LevelRenderEvents.BEFORE_GIZMOS.register { _ ->
                val client = Minecraft.getInstance()
                //? if >= 26.2 {
                /*val camera = client.gameRenderer.mainCamera()
                *///?} else {
                val camera = client.gameRenderer.mainCamera
                //?}
                val tickDelta = client.deltaTracker.getGameTimeDeltaPartialTick(false)
                EventRegistry.post { RenderWorldEvent(camera, tickDelta) }
            }

            registered = true
        }
    }
}
