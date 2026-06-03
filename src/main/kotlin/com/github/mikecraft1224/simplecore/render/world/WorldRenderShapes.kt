@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.render.world

import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.render.api.ScRenderPipelines
import com.github.mikecraft1224.simplecore.render.internal.PipelineRenderer
import com.github.mikecraft1224.simplecore.utils.Color
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.entity.Entity
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Draws a horizontal circle outline at [center].
 *
 * @param center World-space center point (circle lies on the Y plane of this point).
 * @param radius Circle radius in blocks.
 * @param segments Number of line segments approximating the circle.
 * @param seeThroughBlocks When true, the outline is visible through terrain.
 */
fun RenderWorldEvent.drawCircle(
    center: Vec3d,
    radius: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
    segments: Int = 32,
) {
    val step = (2 * Math.PI) / segments
    val cam = camera.pos
    val r = color.r / 255f; val g = color.g / 255f
    val b = color.b / 255f; val a = color.a / 255f

    enqueue(RenderWorldEvent.PRIORITY_LINE) {
        matrices.push()
        matrices.translate(-cam.x, -cam.y, -cam.z)
        val entry = matrices.peek()
        val fy = center.y.toFloat()

        fun VertexConsumer.emitSeg(ax: Float, az: Float, bx: Float, bz: Float) {
            val ddx = bx - ax; val ddz = bz - az
            val l = sqrt(ddx * ddx + ddz * ddz).coerceAtLeast(0.001f)
            vertex(entry, ax, fy, az).color(r, g, b, a).normal(entry, ddx / l, 0f, ddz / l)
            vertex(entry, bx, fy, bz).color(r, g, b, a).normal(entry, ddx / l, 0f, ddz / l)
        }

        if (seeThroughBlocks) {
            PipelineRenderer.drawLines(ScRenderPipelines.LINE_XRAY) {
                for (i in 0 until segments) {
                    val a0 = i * step; val a1 = (i + 1) * step
                    emitSeg(
                        (center.x + cos(a0) * radius).toFloat(), (center.z + sin(a0) * radius).toFloat(),
                        (center.x + cos(a1) * radius).toFloat(), (center.z + sin(a1) * radius).toFloat(),
                    )
                }
            }
        } else {
            val layer = RenderLayer.LINES
            val buf = vertexConsumerProvider.getBuffer(layer)
            for (i in 0 until segments) {
                val a0 = i * step; val a1 = (i + 1) * step
                buf.emitSeg(
                    (center.x + cos(a0) * radius).toFloat(), (center.z + sin(a0) * radius).toFloat(),
                    (center.x + cos(a1) * radius).toFloat(), (center.z + sin(a1) * radius).toFloat(),
                )
            }
            vertexConsumerProvider.draw(layer)
        }

        matrices.pop()
    }
}

/**
 * Draws a filled horizontal disc at [center], visible from both above and below.
 *
 * @param center World-space center point (disc lies on the Y plane of this point).
 * @param radius Disc radius in blocks.
 * @param segments Number of triangular wedge segments.
 * @param seeThroughBlocks When true, the disc is visible through terrain.
 */
fun RenderWorldEvent.drawFilledCircle(
    center: Vec3d,
    radius: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
    segments: Int = 32,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    val cam = camera.pos
    val r = color.r / 255f; val g = color.g / 255f
    val b = color.b / 255f; val a = color.a / 255f
    val step = (2 * Math.PI) / segments

    enqueue(priority) {
        if (seeThroughBlocks) {
            matrices.push()
            matrices.translate(-cam.x, -cam.y, -cam.z)
            val mat = matrices.peek().positionMatrix
            PipelineRenderer.drawQuads(ScRenderPipelines.FILLED_XRAY) {
                val cx = center.x.toFloat(); val cy = center.y.toFloat(); val cz = center.z.toFloat()
                for (i in 0 until segments) {
                    val a0 = i * step
                    val a1 = (i + 1) * step
                    val ex0 = (center.x + cos(a0) * radius).toFloat()
                    val ez0 = (center.z + sin(a0) * radius).toFloat()
                    val ex1 = (center.x + cos(a1) * radius).toFloat()
                    val ez1 = (center.z + sin(a1) * radius).toFloat()
                    // Top face (visible from above, +Y): reversed winding
                    vertex(mat, cx,  cy, cz ).color(r, g, b, a)
                    vertex(mat, ex1, cy, ez1).color(r, g, b, a)
                    vertex(mat, ex0, cy, ez0).color(r, g, b, a)
                    vertex(mat, ex0, cy, ez0).color(r, g, b, a)
                    // Bottom face (visible from below, -Y)
                    vertex(mat, cx,  cy, cz ).color(r, g, b, a)
                    vertex(mat, ex0, cy, ez0).color(r, g, b, a)
                    vertex(mat, ex1, cy, ez1).color(r, g, b, a)
                    vertex(mat, ex1, cy, ez1).color(r, g, b, a)
                }
            }
            matrices.pop()
        } else {
            val layer = RenderLayer.getDebugFilledBox()
            val buf = vertexConsumerProvider.getBuffer(layer)
            matrices.push()
            matrices.translate(-cam.x, -cam.y, -cam.z)
            val entry = matrices.peek()
            val cx = center.x.toFloat(); val cy = center.y.toFloat(); val cz = center.z.toFloat()
            for (i in 0 until segments) {
                val a0 = i * step
                val a1 = (i + 1) * step
                val ex0 = (center.x + cos(a0) * radius).toFloat()
                val ez0 = (center.z + sin(a0) * radius).toFloat()
                val ex1 = (center.x + cos(a1) * radius).toFloat()
                val ez1 = (center.z + sin(a1) * radius).toFloat()
                // Top face
                buf.vertex(entry, cx,  cy, cz ).color(r, g, b, a)
                buf.vertex(entry, ex1, cy, ez1).color(r, g, b, a)
                buf.vertex(entry, ex0, cy, ez0).color(r, g, b, a)
                buf.vertex(entry, ex0, cy, ez0).color(r, g, b, a)
                // Bottom face
                buf.vertex(entry, cx,  cy, cz ).color(r, g, b, a)
                buf.vertex(entry, ex0, cy, ez0).color(r, g, b, a)
                buf.vertex(entry, ex1, cy, ez1).color(r, g, b, a)
                buf.vertex(entry, ex1, cy, ez1).color(r, g, b, a)
            }
            matrices.pop()
            vertexConsumerProvider.draw(layer)
        }
    }
}

/** Draws a connected sequence of line segments through [points]. */
fun RenderWorldEvent.drawPolyline(
    points: List<Vec3d>,
    color: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    for (i in 0 until points.size - 1) {
        draw3DLine(points[i], points[i + 1], color, seeThroughBlocks, priority)
    }
}

/**
 * Draws a quadratic Bézier curve from [p1] to [p3] with one control point.
 *
 * @param p1 Start point.
 * @param control Pull-point that shapes the curve (not on the curve itself).
 * @param p3 End point.
 * @param steps Number of line segments approximating the curve.
 */
fun RenderWorldEvent.drawBezier(
    p1: Vec3d,
    control: Vec3d,
    p3: Vec3d,
    color: Color,
    seeThroughBlocks: Boolean = false,
    steps: Int = 20,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    val pts = ArrayList<Vec3d>(steps + 1)
    for (i in 0..steps) {
        val t = i.toDouble() / steps
        val mt = 1.0 - t
        // Quadratic bezier: (1-t)^2*p1 + 2*(1-t)*t*control + t^2*p3
        val x = mt * mt * p1.x + 2 * mt * t * control.x + t * t * p3.x
        val y = mt * mt * p1.y + 2 * mt * t * control.y + t * t * p3.y
        val z = mt * mt * p1.z + 2 * mt * t * control.z + t * t * p3.z
        pts.add(Vec3d(x, y, z))
    }
    drawPolyline(pts, color, seeThroughBlocks, priority)
}

/** Draws a filled box around [entity]'s bounding box. */
fun RenderWorldEvent.drawEntityBox(
    entity: Entity,
    color: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    drawFilledBox(entity.boundingBox, color, seeThroughBlocks, priority)
}

/**
 * Draws a small pillar with a text label at [pos] — a simple world-space waypoint marker.
 *
 * @param pos World-space position of the waypoint base.
 * @param label Text shown above the marker.
 */
fun RenderWorldEvent.drawWaypoint(
    pos: Vec3d,
    label: String,
    color: Color,
    seeThroughBlocks: Boolean = true,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    val box = net.minecraft.util.math.Box(pos.x - 0.25, pos.y, pos.z - 0.25, pos.x + 0.25, pos.y + 0.5, pos.z + 0.25)
    drawFilledBox(box, color, seeThroughBlocks, priority)
    drawText(Vec3d(pos.x, pos.y + 0.75, pos.z), label, 1f, Color.WHITE, true, seeThroughBlocks)
}

/**
 * Draws a billboard text label that scales with distance so it remains a consistent apparent size.
 *
 * Scale is clamped to the range [0.3, 2.0] to avoid being unreadably small or enormous.
 *
 * @param baseScale Desired apparent scale at 10 blocks distance.
 */
fun RenderWorldEvent.drawDynamicText(
    pos: Vec3d,
    text: String,
    baseScale: Float = 1f,
    color: Color = Color.WHITE,
    shadow: Boolean = true,
    seeThroughBlocks: Boolean = true,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    val distance = camera.pos.distanceTo(pos)
    val scale = (baseScale / (distance * 0.1)).toFloat().coerceIn(0.3f, 2.0f)
    drawText(pos, text, scale, color, shadow, seeThroughBlocks, priority)
}

/**
 * Draws a wireframe sphere using latitude rings and longitude meridians.
 *
 * @param center World-space center of the sphere.
 * @param radius Sphere radius in blocks.
 * @param color Line color.
 * @param seeThroughBlocks When true, lines are visible through terrain.
 * @param stacks Number of horizontal latitude rings (excluding poles).
 * @param slices Number of vertical longitude meridians.
 */
fun RenderWorldEvent.drawSphere(
    center: Vec3d,
    radius: Double,
    color: Color,
    seeThroughBlocks: Boolean = false,
    stacks: Int = 16,
    slices: Int = 32,
    priority: Int = RenderWorldEvent.PRIORITY_LINE,
) {
    val cam = camera.pos
    val r = color.r / 255f; val g = color.g / 255f
    val b = color.b / 255f; val a = color.a / 255f
    val cx = center.x.toFloat(); val cy = center.y.toFloat(); val cz = center.z.toFloat()

    enqueue(priority) {
        matrices.push()
        matrices.translate(-cam.x, -cam.y, -cam.z)
        val entry = matrices.peek()

        fun VertexConsumer.emitSeg(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float) {
            val ddx = bx - ax; val ddy = by - ay; val ddz = bz - az
            val l = sqrt(ddx * ddx + ddy * ddy + ddz * ddz).coerceAtLeast(0.001f)
            vertex(entry, ax, ay, az).color(r, g, b, a).normal(entry, ddx / l, ddy / l, ddz / l)
            vertex(entry, bx, by, bz).color(r, g, b, a).normal(entry, ddx / l, ddy / l, ddz / l)
        }

        fun doRings(vc: VertexConsumer) {
            for (stack in 1 until stacks) {
                val phi = Math.PI * stack / stacks
                val ringY = cy + (cos(phi) * radius).toFloat()
                val ringR = (sin(phi) * radius).toFloat()
                for (slice in 0 until slices) {
                    val theta0 = 2 * Math.PI * slice / slices
                    val theta1 = 2 * Math.PI * (slice + 1) / slices
                    vc.emitSeg(
                        cx + (cos(theta0) * ringR).toFloat(), ringY, cz + (sin(theta0) * ringR).toFloat(),
                        cx + (cos(theta1) * ringR).toFloat(), ringY, cz + (sin(theta1) * ringR).toFloat(),
                    )
                }
            }
            for (slice in 0 until slices) {
                val theta = 2 * Math.PI * slice / slices
                val cosT = cos(theta); val sinT = sin(theta)
                for (stack in 0 until stacks) {
                    val phi0 = Math.PI * stack / stacks
                    val phi1 = Math.PI * (stack + 1) / stacks
                    vc.emitSeg(
                        cx + (sin(phi0) * cosT * radius).toFloat(), cy + (cos(phi0) * radius).toFloat(), cz + (sin(phi0) * sinT * radius).toFloat(),
                        cx + (sin(phi1) * cosT * radius).toFloat(), cy + (cos(phi1) * radius).toFloat(), cz + (sin(phi1) * sinT * radius).toFloat(),
                    )
                }
            }
        }

        if (seeThroughBlocks) {
            PipelineRenderer.drawLines(ScRenderPipelines.LINE_XRAY) { doRings(this) }
        } else {
            val layer = RenderLayer.LINES
            val buf = vertexConsumerProvider.getBuffer(layer)
            doRings(buf)
            vertexConsumerProvider.draw(layer)
        }

        matrices.pop()
    }
}
