package com.github.mikecraft1224.simplecore.render.internal

import com.github.mikecraft1224.simplecore.SimpleCore
import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.events.RenderEntityOutlineEvent
import net.minecraft.client.Minecraft
import net.minecraft.world.entity.Entity

/**
 * Backs the vanilla entity-outline (Glowing effect) render pass with mod-queued highlights.
 *
 * Populated once per frame from [RenderEntityOutlineEvent]'s Fabric hook; consumed by
 * `EntityRendererMixin`'s injection into `EntityRenderer.finalizeRenderState`, which overwrites
 * `EntityRenderState.outlineColor` for any entity present in [frame].
 */
internal object EntityOutlineRegistry {
    private var frame: Map<Entity, Int> = emptyMap()

    fun beginFrame() {
        if (!SimpleCore.EVENTBUS.existHandlers(RenderEntityOutlineEvent::class)) {
            if (frame.isNotEmpty()) frame = emptyMap()
            return
        }
        val level = Minecraft.getInstance().level
        if (level == null) {
            frame = emptyMap()
            return
        }
        val entities = level.entitiesForRendering().toList()
        val queue = HashMap<Entity, Int>()
        EventRegistry.post { RenderEntityOutlineEvent(entities, queue) }
        frame = queue
    }

    fun colorFor(entity: Entity): Int? = frame[entity]
}
