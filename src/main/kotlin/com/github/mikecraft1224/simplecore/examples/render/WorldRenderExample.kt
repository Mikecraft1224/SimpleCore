@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.examples.render

import com.github.mikecraft1224.simplecore.bus.api.Subscribe
import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.render.world.BoxStyle
import com.github.mikecraft1224.simplecore.render.world.draw3DLine
import com.github.mikecraft1224.simplecore.render.world.drawBlockHighlight
import com.github.mikecraft1224.simplecore.render.world.drawBox
import com.github.mikecraft1224.simplecore.render.world.drawFilledCircle
import com.github.mikecraft1224.simplecore.render.world.drawText
import com.github.mikecraft1224.simplecore.render.world.drawTracer
import com.github.mikecraft1224.simplecore.render.world.drawWaypoint
import com.github.mikecraft1224.simplecore.utils.Color
import com.github.mikecraft1224.simplecore.utils.McUtils
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

object WorldRenderExample {

    @Subscribe
    fun onRenderWorld(event: RenderWorldEvent) {
        val player = McUtils.player ?: return
        val foot = player.getLerpedPos(event.tickDelta)

        // Filled + wireframe box east — BoxStyle.BOTH toggles between FILLED, OUTLINED, BOTH
        event.drawBox(
            box   = Box(foot.x + 2, foot.y, foot.z - 0.5, foot.x + 3, foot.y + 1, foot.z + 0.5),
            color = Color(0, 200, 220, 80),
            style = BoxStyle.BOTH,
        )

        // Xray filled box west — visible through walls, seeThroughBlocks now actually works
        event.drawBox(
            box            = Box(foot.x - 3, foot.y, foot.z - 0.5, foot.x - 2, foot.y + 1, foot.z + 0.5),
            color          = Color.RED.withAlpha(120),
            style          = BoxStyle.FILLED,
            seeThroughBlocks = true,
        )

        // Highlight the block the player is standing on
        event.drawBlockHighlight(
            pos   = player.blockPos.down(),
            color = Color(255, 255, 0, 80),
        )

        // Line from 1 block west to 1 block east
        event.draw3DLine(
            from  = Vec3d(foot.x - 1.0, foot.y + 0.5, foot.z),
            to    = Vec3d(foot.x + 1.0, foot.y + 0.5, foot.z),
            color = Color.YELLOW.withAlpha(220),
        )

        // Floating text label above the player
        event.drawText(
            pos              = Vec3d(foot.x, foot.y + 2.5, foot.z),
            text             = "SimpleCore",
            scale            = 1.5f,
            color            = Color.WHITE,
            shadow           = true,
            seeThroughBlocks = true,
        )

        // Tracer from camera toward look direction
        val look = player.rotationVector
        val target = event.camera.pos.add(look.multiply(3.0))
        event.drawTracer(
            to    = target,
            color = Color(255, 100, 100, 200),
        )

        // Filled circle on the ground — double-sided, visible from above and below
        event.drawFilledCircle(
            center           = Vec3d(foot.x, foot.y, foot.z),
            radius           = 2.0,
            color            = Color(100, 255, 100, 80),
            seeThroughBlocks = true,
        )

        // Waypoint 5 blocks north
        event.drawWaypoint(
            pos   = Vec3d(foot.x, foot.y, foot.z - 5.0),
            label = "North",
            color = Color(255, 200, 0, 180),
        )
    }
}
