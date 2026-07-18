@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.render.world

import com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent
import com.github.mikecraft1224.simplecore.utils.Color
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gizmos.GizmoStyle
import net.minecraft.gizmos.Gizmos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

enum class BoxStyle { FILLED, OUTLINED, BOTH }

private fun BoxStyle.toGizmoStyle(color: Color, lineWidth: Float): GizmoStyle = when (this) {
    BoxStyle.FILLED -> GizmoStyle.fill(color.argb)
    BoxStyle.OUTLINED -> GizmoStyle.stroke(color.argb, lineWidth)
    BoxStyle.BOTH -> GizmoStyle.strokeAndFill(color.argb, lineWidth, color.argb)
}

/**
 * Draws a box with the given [style] (filled faces, outline edges, or both) via
 * [net.minecraft.gizmos.Gizmos.cuboid].
 *
 * @param box Axis-aligned bounding box in world space.
 * @param color Fill/line color.
 * @param style Whether to draw filled faces, outline edges, or both.
 * @param seeThroughBlocks When true, the box is visible through terrain.
 * @param lineWidth Outline width in pixels; ignored when [style] is [BoxStyle.FILLED].
 */
fun RenderWorldEvent.drawBox(
    box: AABB,
    color: Color,
    style: BoxStyle = BoxStyle.OUTLINED,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
) {
    val props = Gizmos.cuboid(box, style.toGizmoStyle(color, lineWidth))
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/** Draws a solid-filled axis-aligned box. */
fun RenderWorldEvent.drawFilledBox(box: AABB, color: Color, seeThroughBlocks: Boolean = false) =
    drawBox(box, color, BoxStyle.FILLED, seeThroughBlocks)

/** Draws the edges of an axis-aligned box as lines. */
fun RenderWorldEvent.drawOutlinedBox(
    box: AABB,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
) = drawBox(box, color, BoxStyle.OUTLINED, seeThroughBlocks, lineWidth)

/** Draws a filled highlight over a single block, inset slightly to avoid z-fighting. */
fun RenderWorldEvent.drawBlockHighlight(
    pos: BlockPos,
    color: Color,
    seeThroughBlocks: Boolean = false,
) {
    val inset = 0.002
    val box = AABB(
        pos.x + inset,     pos.y + inset,     pos.z + inset,
        pos.x + 1 - inset, pos.y + 1 - inset, pos.z + 1 - inset,
    )
    drawFilledBox(box, color, seeThroughBlocks)
}

/**
 * Outlines just the four edges of one face of [box] - e.g. [Direction.UP] for a top-only
 * outline, useful for area/plot markers.
 */
fun RenderWorldEvent.outlineTopFace(
    box: AABB,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
    face: Direction = Direction.UP,
) {
    val props = Gizmos.rect(
        Vec3(box.minX, box.minY, box.minZ),
        Vec3(box.maxX, box.maxY, box.maxZ),
        face,
        GizmoStyle.stroke(color.argb, lineWidth),
    )
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/** Fills a single face of [box] - e.g. to highlight the face a player is looking at. */
fun RenderWorldEvent.fillFace(
    box: AABB,
    face: Direction,
    color: Color,
    seeThroughBlocks: Boolean = false,
) {
    val props = Gizmos.rect(
        Vec3(box.minX, box.minY, box.minZ),
        Vec3(box.maxX, box.maxY, box.maxZ),
        face,
        GizmoStyle.fill(color.argb),
    )
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/**
 * Draws a thin quad extending [length] blocks out from [origin] in [face]'s direction -
 * a directional indicator (e.g. showing which way a block face points).
 */
fun RenderWorldEvent.drawFaceRayWorld(
    origin: Vec3,
    face: Direction,
    color: Color,
    length: Double = 0.5,
    seeThroughBlocks: Boolean = false,
) {
    val dir = Vec3(face.stepX.toDouble(), face.stepY.toDouble(), face.stepZ.toDouble())
    val end = origin.add(dir.scale(length))
    val props = Gizmos.line(origin, end, color.argb, 3f)
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/** Draws a single line segment between two world-space points. */
fun RenderWorldEvent.draw3DLine(
    from: Vec3,
    to: Vec3,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
) {
    val props = Gizmos.line(from, to, color.argb, lineWidth)
    if (seeThroughBlocks) props.setAlwaysOnTop()
}

/**
 * Draws a tracer line from the player's crosshair to [to].
 *
 * The line starts [forwardOffset] blocks in front of the player's eyes along their look
 * direction, **not** along the camera-to-[to] vector - a line drawn directly toward its own
 * endpoint is collinear with the view axis and therefore invisible (every point along it
 * projects to the same pixel). Starting from the look direction instead means the line is
 * only collinear with the view axis when [to] happens to be dead-center in view, so it reads
 * as a normal on-screen line the rest of the time.
 */
fun RenderWorldEvent.drawTracer(
    to: Vec3,
    color: Color,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
    forwardOffset: Double = 2.0,
) {
    val player = Minecraft.getInstance().player ?: return
    val eyePos = player.getEyePosition(tickDelta)
    val lookVec = player.getViewVector(tickDelta)
    draw3DLine(eyePos.add(lookVec.scale(forwardOffset)), to, color, seeThroughBlocks, lineWidth)
}

/**
 * Returns the interpolated (partial-tick-correct) bounding box of [entity], correcting for the
 * gap between the entity's last-tick and current-tick position so the box doesn't lag behind
 * the entity's own (already-interpolated) rendered model.
 */
fun RenderWorldEvent.exactBoundingBox(entity: Entity): AABB {
    if (!entity.isAlive) return entity.boundingBox
    val lerped = entity.getPosition(tickDelta)
    val delta = lerped.subtract(entity.x, entity.y, entity.z)
    return entity.boundingBox.move(delta)
}

/** Draws a box around [entity]'s interpolated bounding box (see [exactBoundingBox]). */
fun RenderWorldEvent.drawEntityBox(
    entity: Entity,
    color: Color,
    style: BoxStyle = BoxStyle.OUTLINED,
    seeThroughBlocks: Boolean = false,
    lineWidth: Float = 1f,
) {
    drawBox(exactBoundingBox(entity), color, style, seeThroughBlocks, lineWidth)
}
