@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import java.util.TreeMap

/**
 * Fired once per frame, immediately after Minecraft renders entities in the world.
 *
 * Handlers receive the render context and enqueue draw calls via the extension functions
 * in `WorldRenderUtils.kt` and `WorldRenderShapes.kt`. Draw calls are not executed
 * immediately -- they are queued by priority and flushed in order after all handlers run.
 *
 * Priority constants (lower value = drawn first):
 * - [PRIORITY_WORLD] -- filled/outlined geometry (boxes, circles)
 * - [PRIORITY_LINE]  -- line geometry (3D lines, tracers)
 * - [PRIORITY_TEXT]  -- text labels, rendered on top of all geometry
 *
 * ```kotlin
 * @Feature
 * object WorldRenderer {
 *     @Subscribe
 *     fun onRender(event: RenderWorldEvent) {
 *         if (!McUtils.isInGame) return
 *         event.drawFilledBox(someBox, Color(255, 0, 0, 128))
 *         event.drawTracer(targetPos, Color.WHITE)
 *     }
 * }
 * ```
 *
 * @property matrices               World-space matrix stack; camera transform is already applied.
 * @property camera                 Current camera; use `camera.pos` for the eye position.
 * @property vertexConsumerProvider Immediate vertex consumer. Do not call `draw()` directly --
 *                                  the event flushes the buffer after each priority level.
 * @property tickDelta              Partial tick progress in [0, 1] for smooth position interpolation.
 */
class RenderWorldEvent(
    val matrices: MatrixStack,
    val camera: Camera,
    val vertexConsumerProvider: VertexConsumerProvider.Immediate,
    val tickDelta: Float,
    val isCurrentlyDeferring: Boolean = true
) : Event() {

    /** Deferred render actions keyed by priority (lower = rendered first). */
    private val renderQueue = TreeMap<Int, MutableList<() -> Unit>>()

    /** Enqueue a render action at a given priority level. Lower priority values render first. */
    internal fun enqueue(priority: Int, action: () -> Unit) {
        renderQueue.getOrPut(priority) { mutableListOf() }.add(action)
    }

    /**
     * Iterates the sorted priority map and for each level:
     * 1. Calls [VertexConsumerProvider.Immediate.draw] once to flush any entity VCP geometry
     *    submitted at that priority before our shapes.
     * 2. Executes all enqueued actions for that level.
     *
     * Called by [registerEvents] after all handlers have run.
     */
    fun flushRenderQueue() {
        for ((_, actions) in renderQueue) {
            vertexConsumerProvider.draw()
            for (action in actions) action()
        }
        renderQueue.clear()
    }

    companion object : EventCompanion {
        /** Priority for filled/outlined world geometry (boxes, circles). Rendered first. */
        const val PRIORITY_WORLD = 0
        /** Priority for line geometry (3D lines, tracers). Rendered second. */
        const val PRIORITY_LINE  = 100
        /** Priority for text labels. Rendered last (on top of geometry). */
        const val PRIORITY_TEXT  = 200

        private var registered = false

        override fun registerEvents() {
            if (registered) return

            WorldRenderEvents.AFTER_ENTITIES.register { ctx ->
                val vertexConsumers = ctx.consumers() as? VertexConsumerProvider.Immediate ?: return@register

                val stack = ctx.matrices() ?: MatrixStack()
                val client = MinecraftClient.getInstance()
                val camera = client.gameRenderer.camera

                val event = RenderWorldEvent(
                    stack,
                    camera,
                    vertexConsumers,
                    client.renderTickCounter.getTickProgress(true),
                    false
                )
                EventRegistry.post(event)
                event.flushRenderQueue()
            }

            registered = true
        }
    }
}