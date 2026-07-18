@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.render.internal.EntityOutlineRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import com.github.mikecraft1224.simplecore.utils.Color
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.world.entity.Entity

/**
 * Fired once per frame with the set of entities currently loaded in the world. Handlers queue
 * entities to highlight with a colored outline (the same effect as the vanilla Glowing potion
 * effect / spectator highlight) by calling [highlight].
 *
 * Queued colors take effect starting the *next* rendered frame - this event fires slightly after
 * this frame's entity outline colors have already been finalized, so there is a one-frame lag.
 * Imperceptible in practice.
 *
 * The underlying mechanism is always visible through walls (it reuses vanilla's own entity
 * outline post-process pass, which draws to a separate framebuffer with no terrain in it) -
 * there is no non-xray variant.
 *
 * ```kotlin
 * @Feature
 * object PartyHighlighter {
 *     @Subscribe
 *     fun onOutline(event: RenderEntityOutlineEvent) {
 *         for (entity in event.entities) {
 *             if (entity is PlayerEntity && entity.name in partyMembers) {
 *                 event.highlight(entity, Color.CYAN)
 *             }
 *         }
 *     }
 * }
 * ```
 */
class RenderEntityOutlineEvent(
    val entities: List<Entity>,
    private val queue: MutableMap<Entity, Int>,
) : Event() {

    /** Queues [entity] to be drawn with a [color] outline starting next frame. */
    fun highlight(entity: Entity, color: Color) {
        // Force full alpha (vanilla's outline shader ignores alpha) and avoid colliding with
        // the packed-zero NO_OUTLINE sentinel for a fully-black requested color.
        queue[entity] = color.rgb or 0xFF000000.toInt()
    }

    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return
            LevelRenderEvents.BEFORE_GIZMOS.register { _ -> EntityOutlineRegistry.beginFrame() }
            registered = true
        }
    }
}
