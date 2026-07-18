@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.examples.render

import com.github.mikecraft1224.simplecore.bus.api.Subscribe
import com.github.mikecraft1224.simplecore.bus.events.RenderEntityOutlineEvent
import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.render.world.draw3DLine
import com.github.mikecraft1224.simplecore.render.world.drawFilledBox
import com.github.mikecraft1224.simplecore.render.world.drawText
import com.github.mikecraft1224.simplecore.render.world.drawTracer
import com.github.mikecraft1224.simplecore.utils.Color
import com.github.mikecraft1224.simplecore.utils.center
import com.github.mikecraft1224.simplecore.utils.nearestEntity
import net.minecraft.world.entity.Mob
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Picks the nearest mob within 32 blocks and renders:
 *  - A filled xray box around its interpolated hitbox
 *  - A tracer from screen center to the mob (works in first- and third-person)
 *  - A floating name label above the mob
 *  - A vanilla-style glow outline (the same effect as the Glowing potion), via [onOutline]
 */
object EntityTargetExample {

    private fun findTarget(): Mob? = nearestEntity<Mob>(32.0)

    /** Highlights the nearest mob with a red glow outline - always visible through walls. */
    @Subscribe
    fun onOutline(event: RenderEntityOutlineEvent) {
        val target = findTarget() ?: return
        event.highlight(target, Color(255, 60, 60))
    }

    @Subscribe
    fun onRenderWorld(event: RenderWorldEvent) {
        val target = findTarget() ?: return

        // Interpolate position using tickDelta for smooth sub-tick motion (avoids snappy on-tick updates)
        val lerpedPos = target.getPosition(event.tickDelta)
        val hw = target.bbWidth / 2.0
        val lerpedBox = AABB(
            lerpedPos.x - hw, lerpedPos.y,                lerpedPos.z - hw,
            lerpedPos.x + hw, lerpedPos.y + target.bbHeight, lerpedPos.z + hw,
        )
        val center = lerpedBox.center()

        // Filled red box around the mob's interpolated hitbox, visible through blocks
        event.drawFilledBox(lerpedBox, Color(255, 60, 60, 80), seeThroughBlocks = true)

        // Tracer from the player's crosshair toward the mob.
        event.drawTracer(center, Color(255, 100, 50, 200), seeThroughBlocks = true, lineWidth = 4f)

        // Floating name above the mob
        val nameY = lerpedBox.maxY + 0.3
        event.drawText(
            pos   = Vec3(center.x, nameY, center.z),
            text  = "§c${target.name.string}",
            scale = 1f,
            color = Color.WHITE,
            seeThroughBlocks = true,
        )
    }
}
