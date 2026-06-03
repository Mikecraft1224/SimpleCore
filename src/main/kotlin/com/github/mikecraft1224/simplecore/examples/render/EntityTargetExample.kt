@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.examples.render

import com.github.mikecraft1224.simplecore.bus.api.Subscribe
import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.render.world.draw3DLine
import com.github.mikecraft1224.simplecore.render.world.drawFilledBox
import com.github.mikecraft1224.simplecore.render.world.drawText
import com.github.mikecraft1224.simplecore.utils.Color
import com.github.mikecraft1224.simplecore.utils.McUtils
import net.minecraft.entity.mob.MobEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * Picks the nearest mob within 32 blocks and renders:
 *  - A filled xray box around its interpolated hitbox
 *  - A tracer from screen center to the mob (works in first- and third-person)
 *  - A floating name label above the mob
 */
object EntityTargetExample {

    @Subscribe
    fun onRenderWorld(event: RenderWorldEvent) {
        val player = McUtils.player ?: return
        val world  = McUtils.world  ?: return

        val target = world.getEntitiesByClass(MobEntity::class.java, player.boundingBox.expand(32.0)) { true }
            .minByOrNull { it.squaredDistanceTo(player) }
            ?: return

        // Interpolate position using tickDelta for smooth sub-tick motion (avoids snappy on-tick updates)
        val lerpedPos = target.getLerpedPos(event.tickDelta)
        val hw = target.width / 2.0
        val lerpedBox = Box(
            lerpedPos.x - hw, lerpedPos.y,              lerpedPos.z - hw,
            lerpedPos.x + hw, lerpedPos.y + target.height, lerpedPos.z + hw,
        )
        val center = lerpedBox.center

        // Filled red box around the mob's interpolated hitbox, visible through blocks
        event.drawFilledBox(lerpedBox, Color(255, 60, 60, 80), seeThroughBlocks = true)

        // Tracer from screen-center (eye + look direction * 2) toward the mob.
        // Using the player's rotation vector means the line always originates from center-screen
        // rather than toward the target, so it functions as a proper crosshair tracer.
        val eyePos = player.getCameraPosVec(event.tickDelta)
        val lookVec = player.getRotationVec(event.tickDelta)
        event.draw3DLine(eyePos.add(lookVec.multiply(2.0)), center, Color(255, 100, 50, 200))

        // Floating name above the mob
        val nameY = lerpedBox.maxY + 0.3
        event.drawText(
            pos   = Vec3d(center.x, nameY, center.z),
            text  = "§c${target.name.string}",
            scale = 1f,
            color = Color.WHITE,
            shadow = true,
            seeThroughBlocks = true,
        )
    }
}
