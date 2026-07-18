@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.render.world

import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.utils.Color
import net.minecraft.client.Minecraft
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.gizmos.TextGizmo
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws a circle outline at [center] via [net.minecraft.gizmos.Gizmos.circle].
 *
 * @param center World-space center point.
 * @param radius Circle radius in blocks.
 */
fun RenderWorldEvent.drawCircle(
    center: Vec3,
    radius: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
) {
    val props = Gizmos.circle(center, radius.toFloat(), GizmoStyle.stroke(color.argb, lineWidth))
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/** Draws a filled disc at [center]. */
fun RenderWorldEvent.drawFilledCircle(
    center: Vec3,
    radius: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
) {
    val props = Gizmos.circle(center, radius.toFloat(), GizmoStyle.fill(color.argb))
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/** Draws a connected sequence of line segments through [points]. */
fun RenderWorldEvent.drawPolyline(
    points: List<Vec3>,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
) {
    for (i in 0 until points.size - 1) {
        draw3DLine(points[i], points[i + 1], color, seeThroughBlocks, lineWidth)
    }
}

/**
 * Draws a quadratic Bezier curve from [p1] to [p3] with one control point, tessellated into
 * straight segments.
 */
fun RenderWorldEvent.drawBezier(
    p1: Vec3,
    control: Vec3,
    p3: Vec3,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
    steps: Int = 20,
) {
    val pts = ArrayList<Vec3>(steps + 1)
    for (i in 0..steps) {
        val t = i.toDouble() / steps
        val mt = 1.0 - t
        val x = mt * mt * p1.x + 2 * mt * t * control.x + t * t * p3.x
        val y = mt * mt * p1.y + 2 * mt * t * control.y + t * t * p3.y
        val z = mt * mt * p1.z + 2 * mt * t * control.z + t * t * p3.z
        pts.add(Vec3(x, y, z))
    }
    drawPolyline(pts, color, seeThroughBlocks, lineWidth)
}

/**
 * Draws a line whose color interpolates from [colorFrom] to [colorTo], approximated as a
 * sequence of short segments since gizmo lines only support one solid color each.
 */
fun RenderWorldEvent.drawGradientLine(
    from: Vec3,
    to: Vec3,
    colorFrom: Color,
    colorTo: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
    segments: Int = 12,
) {
    for (i in 0 until segments) {
        val t0 = i.toDouble() / segments
        val t1 = (i + 1).toDouble() / segments
        val a = from.lerp(to, t0)
        val b = from.lerp(to, t1)
        val c = colorFrom.blend(colorTo, ((t0 + t1) / 2).toFloat())
        draw3DLine(a, b, c, seeThroughBlocks, lineWidth)
    }
}

/**
 * Draws a gradient tracer line from the player's crosshair to [to].
 *
 * Like [drawTracer], the start point is offset [forwardOffset] blocks along the player's look
 * direction rather than along the camera-to-[to] vector - a line drawn directly toward its own
 * endpoint is collinear with the view axis and therefore invisible (every point along it
 * projects to the same pixel).
 */
fun RenderWorldEvent.drawGradientTracer(
    to: Vec3,
    colorFrom: Color,
    colorTo: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
    forwardOffset: Double = 2.0,
) {
    val player = Minecraft.getInstance().player ?: return
    val eyePos = player.getEyePosition(tickDelta)
    val lookVec = player.getViewVector(tickDelta)
    drawGradientLine(eyePos.add(lookVec.scale(forwardOffset)), to, colorFrom, colorTo, seeThroughBlocks, lineWidth)
}

/**
 * Renders a billboard text label at a world-space position via [net.minecraft.gizmos.Gizmos.billboardText].
 *
 * @param pos World-space anchor.
 * @param scale Text scale multiplier (1f = default size).
 */
fun RenderWorldEvent.drawText(
    pos: Vec3,
    text: String,
    scale: Float = 1f,
    color: Color = Color.WHITE,
    seeThroughBlocks: Boolean = true,
) {
    val props = Gizmos.billboardText(text, pos, TextGizmo.Style.forColorAndCentered(color.argb).withScale(scale))
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/**
 * Renders billboard text that scales with distance and hides when too close or too far.
 *
 * @param baseScale Desired apparent scale at 10 blocks distance.
 * @param hideTooCloseAt Distance below which the label is skipped entirely.
 * @param maxDistance Distance beyond which the label is skipped when [seeThroughBlocks] is false.
 */
fun RenderWorldEvent.drawDynamicText(
    pos: Vec3,
    text: String,
    baseScale: Float = 1f,
    color: Color = Color.WHITE,
    seeThroughBlocks: Boolean = true,
    hideTooCloseAt: Double = 0.0,
    maxDistance: Double? = null,
) {
    val distance = camera.position().distanceTo(pos)
    if (distance < hideTooCloseAt) return
    if (!seeThroughBlocks && maxDistance != null && distance > maxDistance) return
    val scale = (baseScale / (distance * 0.1)).toFloat().coerceIn(0.3f, 2.0f)
    drawText(pos, text, scale, color, seeThroughBlocks)
}

/**
 * Draws a small pillar with a text label at [pos] - a world-space waypoint marker.
 *
 * @param pos World-space position of the waypoint base.
 * @param label Text shown above the marker.
 * @param beacon When true, also draws a tall thin translucent column above the marker so it
 *   stays visible from far away (a simplified stand-in for vanilla's real beacon beam, which
 *   needs internals not reachable from this event - see [renderBeaconBeam]).
 */
fun RenderWorldEvent.drawWaypoint(
    pos: Vec3,
    label: String,
    color: Color,
    seeThroughBlocks: Boolean = true,
    beacon: Boolean = false,
    minimumAlpha: Float = 0.2f,
) {
    val distance = camera.position().distanceTo(pos)
    val alpha = (0.1f + 0.005f * (distance * distance).toFloat()).coerceIn(minimumAlpha, 1f)
    val box = AABB(pos.x - 0.25, pos.y, pos.z - 0.25, pos.x + 0.25, pos.y + 0.5, pos.z + 0.25)
    drawFilledBox(box, color.withAlpha(alpha), seeThroughBlocks)
    drawText(Vec3(pos.x, pos.y + 0.75, pos.z), label, 1f, Color.WHITE, seeThroughBlocks)
    if (beacon && distance > 5.0) renderBeaconBeam(Vec3(pos.x, pos.y + 1, pos.z), color)
}

/**
 * Renders a simplified translucent vertical beam of [color] at [pos] - a lightweight
 * long-distance visibility marker, not a call into vanilla's real beacon renderer (that needs a
 * `SubmitNodeStorage` not reachable from [RenderWorldEvent]).
 */
fun RenderWorldEvent.renderBeaconBeam(
    pos: Vec3,
    color: Color,
    height: Double = 300.0,
    radius: Double = 0.15,
    seeThroughBlocks: Boolean = true,
) {
    val box = AABB(pos.x - radius, pos.y, pos.z - radius, pos.x + radius, pos.y + height, pos.z + radius)
    drawFilledBox(box, color.withAlpha(90), seeThroughBlocks)
}

// -- Wireframe volumes (composed from circle/line gizmos - no filled variant available without
// -- raw custom geometry, which needs a RenderType mods currently can't construct on 26.2) -------

/**
 * Draws a wireframe sphere using latitude rings and longitude meridians, composed from
 * [net.minecraft.gizmos.Gizmos.line] calls.
 */
fun RenderWorldEvent.drawSphere(
    center: Vec3,
    radius: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
    stacks: Int = 16,
    slices: Int = 32,
    lineWidth: Float = 1f,
) {
    for (stack in 1 until stacks) {
        val phi = Math.PI * stack / stacks
        val ringY = cos(phi) * radius
        val ringR = sin(phi) * radius
        for (slice in 0 until slices) {
            val t0 = 2 * Math.PI * slice / slices
            val t1 = 2 * Math.PI * (slice + 1) / slices
            draw3DLine(
                Vec3(center.x + cos(t0) * ringR, center.y + ringY, center.z + sin(t0) * ringR),
                Vec3(center.x + cos(t1) * ringR, center.y + ringY, center.z + sin(t1) * ringR),
                color, seeThroughBlocks, lineWidth,
            )
        }
    }
    for (slice in 0 until slices) {
        val theta = 2 * Math.PI * slice / slices
        val cosT = cos(theta); val sinT = sin(theta)
        for (stack in 0 until stacks) {
            val phi0 = Math.PI * stack / stacks
            val phi1 = Math.PI * (stack + 1) / stacks
            draw3DLine(
                Vec3(center.x + sin(phi0) * cosT * radius, center.y + cos(phi0) * radius, center.z + sin(phi0) * sinT * radius),
                Vec3(center.x + sin(phi1) * cosT * radius, center.y + cos(phi1) * radius, center.z + sin(phi1) * sinT * radius),
                color, seeThroughBlocks, lineWidth,
            )
        }
    }
}

/** Draws a wireframe cylinder: two circle rims plus vertical connecting lines. */
fun RenderWorldEvent.drawCylinder(
    base: Vec3,
    radius: Double,
    height: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
    segments: Int = 32,
    lineWidth: Float = 1f,
) {
    val top = Vec3(base.x, base.y + height, base.z)
    drawCircle(base, radius, color, seeThroughBlocks, lineWidth)
    drawCircle(top, radius, color, seeThroughBlocks, lineWidth)
    for (i in 0 until segments) {
        val angle = 2 * Math.PI * i / segments
        val dx = cos(angle) * radius
        val dz = sin(angle) * radius
        draw3DLine(
            Vec3(base.x + dx, base.y, base.z + dz),
            Vec3(top.x + dx, top.y, top.z + dz),
            color, seeThroughBlocks, lineWidth,
        )
    }
}

/** Draws a wireframe pyramid: a square base plus four edges converging on [apex]. */
fun RenderWorldEvent.drawPyramid(
    apex: Vec3,
    baseCenter: Vec3,
    baseRadius: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
) {
    val corners = listOf(
        Vec3(baseCenter.x - baseRadius, baseCenter.y, baseCenter.z - baseRadius),
        Vec3(baseCenter.x + baseRadius, baseCenter.y, baseCenter.z - baseRadius),
        Vec3(baseCenter.x + baseRadius, baseCenter.y, baseCenter.z + baseRadius),
        Vec3(baseCenter.x - baseRadius, baseCenter.y, baseCenter.z + baseRadius),
    )
    for (i in corners.indices) {
        draw3DLine(corners[i], corners[(i + 1) % corners.size], color, seeThroughBlocks, lineWidth)
        draw3DLine(apex, corners[i], color, seeThroughBlocks, lineWidth)
    }
}
