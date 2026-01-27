package com.github.mikecraft1224.bus.events

import com.github.mikecraft1224.bus.EventRegistry
import com.github.mikecraft1224.bus.api.Event
import com.github.mikecraft1224.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack

@Suppress("UNUSED")
class RenderWorldEvent(
    val matrices: MatrixStack,
    val camera: Camera,
    val vertexConsumerProvider: VertexConsumerProvider.Immediate,
    val tickDelta: Float,
    val isCurrentlyDeferring: Boolean = true
) : Event<RenderWorldEvent>() {
    companion object : EventCompanion {
        var registered = false

        override fun registerEvents() {
            if (registered) return

            WorldRenderEvents.AFTER_ENTITIES.register { ctx ->
                val vertexConsumers = ctx.consumers() as? VertexConsumerProvider.Immediate ?: return@register

                val stack = ctx.matrices() ?: MatrixStack()
                val client = MinecraftClient.getInstance()
                val camera = client.gameRenderer.camera

                EventRegistry.post {
                    RenderWorldEvent(
                        stack,
                        camera,
                        vertexConsumers,
                        client.renderTickCounter.getTickProgress(true),
                        false
                    )
                }
            }

            registered = true
        }
    }
}