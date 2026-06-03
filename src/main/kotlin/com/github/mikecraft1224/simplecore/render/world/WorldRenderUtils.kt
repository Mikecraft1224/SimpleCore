@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.render.world

import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.render.api.ScRenderPipelines
import com.github.mikecraft1224.simplecore.render.internal.PipelineRenderer
import com.github.mikecraft1224.simplecore.utils.Color
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer.TextLayerType
import net.minecraft.client.render.LightmapTextureManager
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexRendering
import net.minecraft.client.render.debug.DebugRenderer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.sqrt

enum class BoxStyle { FILLED, OUTLINED, BOTH }

/**
 * Draws a box with the given [style] (filled faces, outline edges, or both).
 *
 * @param box Axis-aligned bounding box in world space.
 * @param color Fill/line color.
 * @param style Whether to draw filled faces, outline edges, or both.
 * @param seeThroughBlocks When true, geometry is visible through terrain.
 */
fun RenderWorldEvent.drawBox(
    box: Box,
    color: Color,
    style: BoxStyle = BoxStyle.OUTLINED,
    seeThroughBlocks: Boolean = false,
) {
    when (style) {
        BoxStyle.FILLED -> drawFilledBox(box, color, seeThroughBlocks)
        BoxStyle.OUTLINED -> drawOutlinedBox(box, color, seeThroughBlocks)
        BoxStyle.BOTH -> {
            drawFilledBox(box, color, seeThroughBlocks)
            drawOutlinedBox(box, color, seeThroughBlocks)
        }
    }
}

/** Draws a solid-filled axis-aligned box. */
fun RenderWorldEvent.drawFilledBox(
    box: Box,
    color: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    val cam = camera.pos
    val r = color.r / 255f; val g = color.g / 255f
    val b = color.b / 255f; val a = color.a / 255f

    enqueue(priority) {
        if (seeThroughBlocks) {
            matrices.push()
            matrices.translate(-cam.x, -cam.y, -cam.z)
            val m = matrices.peek().positionMatrix
            PipelineRenderer.drawQuads(ScRenderPipelines.FILLED_XRAY) {
                val minX = box.minX.toFloat(); val minY = box.minY.toFloat(); val minZ = box.minZ.toFloat()
                val maxX = box.maxX.toFloat(); val maxY = box.maxY.toFloat(); val maxZ = box.maxZ.toFloat()
                // Front
                vertex(m, minX, minY, maxZ).color(r, g, b, a)
                vertex(m, maxX, minY, maxZ).color(r, g, b, a)
                vertex(m, maxX, maxY, maxZ).color(r, g, b, a)
                vertex(m, minX, maxY, maxZ).color(r, g, b, a)
                // Back
                vertex(m, maxX, minY, minZ).color(r, g, b, a)
                vertex(m, minX, minY, minZ).color(r, g, b, a)
                vertex(m, minX, maxY, minZ).color(r, g, b, a)
                vertex(m, maxX, maxY, minZ).color(r, g, b, a)
                // Left
                vertex(m, minX, minY, minZ).color(r, g, b, a)
                vertex(m, minX, minY, maxZ).color(r, g, b, a)
                vertex(m, minX, maxY, maxZ).color(r, g, b, a)
                vertex(m, minX, maxY, minZ).color(r, g, b, a)
                // Right
                vertex(m, maxX, minY, maxZ).color(r, g, b, a)
                vertex(m, maxX, minY, minZ).color(r, g, b, a)
                vertex(m, maxX, maxY, minZ).color(r, g, b, a)
                vertex(m, maxX, maxY, maxZ).color(r, g, b, a)
                // Top
                vertex(m, minX, maxY, maxZ).color(r, g, b, a)
                vertex(m, maxX, maxY, maxZ).color(r, g, b, a)
                vertex(m, maxX, maxY, minZ).color(r, g, b, a)
                vertex(m, minX, maxY, minZ).color(r, g, b, a)
                // Bottom
                vertex(m, minX, minY, minZ).color(r, g, b, a)
                vertex(m, maxX, minY, minZ).color(r, g, b, a)
                vertex(m, maxX, minY, maxZ).color(r, g, b, a)
                vertex(m, minX, minY, maxZ).color(r, g, b, a)
            }
            matrices.pop()
        } else {
            val layer = RenderLayer.getDebugFilledBox()
            val buf = vertexConsumerProvider.getBuffer(layer)
            matrices.push()
            matrices.translate(-cam.x, -cam.y, -cam.z)
            VertexRendering.drawFilledBox(
                matrices, buf,
                box.minX.toFloat(), box.minY.toFloat(), box.minZ.toFloat(),
                box.maxX.toFloat(), box.maxY.toFloat(), box.maxZ.toFloat(),
                r, g, b, a,
            )
            matrices.pop()
            vertexConsumerProvider.draw(layer)
        }
    }
}

/** Draws the 12 edges of an axis-aligned box as lines. */
fun RenderWorldEvent.drawOutlinedBox(
    box: Box,
    color: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_WORLD,
) {
    val cam = camera.pos
    val r = color.r / 255f; val g = color.g / 255f
    val b = color.b / 255f; val a = color.a / 255f

    enqueue(priority) {
        matrices.push()
        matrices.translate(-cam.x, -cam.y, -cam.z)

        if (seeThroughBlocks) {
            val entry = matrices.peek()
            val x0 = box.minX.toFloat(); val y0 = box.minY.toFloat(); val z0 = box.minZ.toFloat()
            val x1 = box.maxX.toFloat(); val y1 = box.maxY.toFloat(); val z1 = box.maxZ.toFloat()
            PipelineRenderer.drawLines(ScRenderPipelines.LINE_XRAY) {
                val bb = this
                fun line(ax: Float, ay: Float, az: Float, bx: Float, by: Float, bz: Float) {
                    val ddx = bx - ax; val ddy = by - ay; val ddz = bz - az
                    val l = sqrt(ddx * ddx + ddy * ddy + ddz * ddz).coerceAtLeast(0.001f)
                    val nx = ddx / l; val ny = ddy / l; val nz = ddz / l
                    bb.vertex(entry, ax, ay, az).color(r, g, b, a).normal(entry, nx, ny, nz)
                    bb.vertex(entry, bx, by, bz).color(r, g, b, a).normal(entry, nx, ny, nz)
                }
                // Bottom face
                line(x0, y0, z0, x1, y0, z0); line(x1, y0, z0, x1, y0, z1)
                line(x1, y0, z1, x0, y0, z1); line(x0, y0, z1, x0, y0, z0)
                // Top face
                line(x0, y1, z0, x1, y1, z0); line(x1, y1, z0, x1, y1, z1)
                line(x1, y1, z1, x0, y1, z1); line(x0, y1, z1, x0, y1, z0)
                // Vertical edges
                line(x0, y0, z0, x0, y1, z0); line(x1, y0, z0, x1, y1, z0)
                line(x1, y0, z1, x1, y1, z1); line(x0, y0, z1, x0, y1, z1)
            }
        } else {
            DebugRenderer.drawBox(matrices, vertexConsumerProvider, box, r, g, b, a)
            vertexConsumerProvider.draw(RenderLayer.LINES)
        }

        matrices.pop()
    }
}

/** Draws a filled highlight over a single block, inset slightly to avoid z-fighting. */
fun RenderWorldEvent.drawBlockHighlight(
    pos: BlockPos,
    color: Color,
    seeThroughBlocks: Boolean = false,
) {
    val inset = 0.002
    val box = Box(
        pos.x + inset,     pos.y + inset,     pos.z + inset,
        pos.x + 1 - inset, pos.y + 1 - inset, pos.z + 1 - inset,
    )
    drawFilledBox(box, color, seeThroughBlocks)
}

/**
 * Draws a single line segment between two world-space points.
 *
 * @param from Start position.
 * @param to End position.
 * @param seeThroughBlocks When true, the line is visible through terrain.
 */
fun RenderWorldEvent.draw3DLine(
    from: Vec3d,
    to: Vec3d,
    color: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_LINE,
) {
    val cam = camera.pos
    val r = color.r / 255f; val g = color.g / 255f
    val b = color.b / 255f; val a = color.a / 255f

    val dx = (to.x - from.x).toFloat()
    val dy = (to.y - from.y).toFloat()
    val dz = (to.z - from.z).toFloat()
    val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001f)
    val nx = dx / len; val ny = dy / len; val nz = dz / len

    enqueue(priority) {
        matrices.push()
        matrices.translate(-cam.x, -cam.y, -cam.z)
        val entry = matrices.peek()

        if (seeThroughBlocks) {
            PipelineRenderer.drawLines(ScRenderPipelines.LINE_XRAY) {
                vertex(entry, from.x.toFloat(), from.y.toFloat(), from.z.toFloat()).color(r, g, b, a).normal(entry, nx, ny, nz)
                vertex(entry, to.x.toFloat(),   to.y.toFloat(),   to.z.toFloat()  ).color(r, g, b, a).normal(entry, nx, ny, nz)
            }
        } else {
            val layer = RenderLayer.LINES
            val buf = vertexConsumerProvider.getBuffer(layer)
            buf.vertex(entry, from.x.toFloat(), from.y.toFloat(), from.z.toFloat()).color(r, g, b, a).normal(entry, nx, ny, nz)
            buf.vertex(entry, to.x.toFloat(),   to.y.toFloat(),   to.z.toFloat()  ).color(r, g, b, a).normal(entry, nx, ny, nz)
            vertexConsumerProvider.draw(layer)
        }

        matrices.pop()
    }
}

/**
 * Draws a tracer line from the camera crosshair to [to].
 *
 * The line starts slightly in front of the camera to clear the near-clip plane.
 */
fun RenderWorldEvent.drawTracer(
    to: Vec3d,
    color: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_LINE,
) {
    val from = camera.pos
    val dir = to.subtract(from)
    val len = dir.length()
    if (len < 0.001) return
    // Offset 0.15 units toward the target to clear the near-clip plane (Minecraft uses 0.05f).
    draw3DLine(from.add(dir.multiply(0.15 / len)), to, color, seeThroughBlocks, priority)
}

/**
 * Draws a line segment with a color gradient between the two endpoints.
 *
 * @param from Start position in world space.
 * @param to End position in world space.
 * @param colorFrom Color at [from].
 * @param colorTo Color at [to].
 * @param seeThroughBlocks When true, the line is visible through terrain.
 */
fun RenderWorldEvent.drawGradientLine(
    from: Vec3d,
    to: Vec3d,
    colorFrom: Color,
    colorTo: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_LINE,
) {
    val cam = camera.pos
    val r0 = colorFrom.r / 255f; val g0 = colorFrom.g / 255f; val b0 = colorFrom.b / 255f; val a0 = colorFrom.a / 255f
    val r1 = colorTo.r / 255f;   val g1 = colorTo.g / 255f;   val b1 = colorTo.b / 255f;   val a1 = colorTo.a / 255f
    val dx = (to.x - from.x).toFloat(); val dy = (to.y - from.y).toFloat(); val dz = (to.z - from.z).toFloat()
    val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.001f)
    val nx = dx / len; val ny = dy / len; val nz = dz / len

    enqueue(priority) {
        matrices.push()
        matrices.translate(-cam.x, -cam.y, -cam.z)
        val entry = matrices.peek()

        if (seeThroughBlocks) {
            PipelineRenderer.drawLines(ScRenderPipelines.LINE_XRAY) {
                vertex(entry, from.x.toFloat(), from.y.toFloat(), from.z.toFloat()).color(r0, g0, b0, a0).normal(entry, nx, ny, nz)
                vertex(entry, to.x.toFloat(),   to.y.toFloat(),   to.z.toFloat()  ).color(r1, g1, b1, a1).normal(entry, nx, ny, nz)
            }
        } else {
            val layer = RenderLayer.LINES
            val buf = vertexConsumerProvider.getBuffer(layer)
            buf.vertex(entry, from.x.toFloat(), from.y.toFloat(), from.z.toFloat()).color(r0, g0, b0, a0).normal(entry, nx, ny, nz)
            buf.vertex(entry, to.x.toFloat(),   to.y.toFloat(),   to.z.toFloat()  ).color(r1, g1, b1, a1).normal(entry, nx, ny, nz)
            vertexConsumerProvider.draw(layer)
        }

        matrices.pop()
    }
}

/**
 * Draws a gradient tracer line from the camera crosshair to [to].
 *
 * @param to Target position in world space.
 * @param colorFrom Color at the camera end.
 * @param colorTo Color at [to].
 * @param seeThroughBlocks When true, the line is visible through terrain.
 */
fun RenderWorldEvent.drawGradientTracer(
    to: Vec3d,
    colorFrom: Color,
    colorTo: Color,
    seeThroughBlocks: Boolean = false,
    priority: Int = RenderWorldEvent.PRIORITY_LINE,
) {
    val from = camera.pos
    val dir = to.subtract(from)
    val len = dir.length()
    if (len < 0.001) return
    drawGradientLine(from.add(dir.multiply(0.15 / len)), to, colorFrom, colorTo, seeThroughBlocks, priority)
}

/**
 * Renders a billboard text label at a world-space position.
 *
 * The text is always camera-facing. Scale 1f corresponds to roughly one block in height
 * at a distance of 10 blocks.
 *
 * @param pos World-space anchor (bottom-center of the text).
 * @param scale Text scale multiplier (1f = default size).
 * @param seeThroughBlocks When true, text is visible through terrain.
 */
fun RenderWorldEvent.drawText(
    pos: Vec3d,
    text: String,
    scale: Float = 1f,
    color: Color = Color.WHITE,
    shadow: Boolean = true,
    seeThroughBlocks: Boolean = true,
    priority: Int = RenderWorldEvent.PRIORITY_TEXT,
) {
    val cam = camera.pos
    val r = color.argb

    enqueue(priority) {
        val mc = MinecraftClient.getInstance()
        val tr = mc.textRenderer

        matrices.push()
        matrices.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z)
        matrices.multiply(camera.rotation)
        // Scale and flip Y: text renderer draws downward in local space, world Y is up
        val s = scale * 0.025f
        matrices.scale(s, -s, s)

        val halfWidth = tr.getWidth(text) / 2f
        val layerType = if (seeThroughBlocks) TextLayerType.SEE_THROUGH else TextLayerType.NORMAL

        tr.draw(
            text,
            -halfWidth,
            0f,
            r,
            shadow,
            matrices.peek().positionMatrix,
            vertexConsumerProvider,
            layerType,
            0,
            LightmapTextureManager.MAX_LIGHT_COORDINATE,
        )
        matrices.pop()
        vertexConsumerProvider.draw()
    }
}
