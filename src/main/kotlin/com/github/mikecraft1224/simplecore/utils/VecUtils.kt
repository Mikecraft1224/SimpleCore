@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// -- Vec3d extensions ----------------------------------------------------------

fun Vec3d.up(n: Double = 1.0): Vec3d = Vec3d(x, y + n, z)
fun Vec3d.down(n: Double = 1.0): Vec3d = Vec3d(x, y - n, z)

fun Vec3d.roundToBlock(): Vec3d = Vec3d(floor(x), floor(y), floor(z))

fun Vec3d.blockCenter(): Vec3d = Vec3d(floor(x) + 0.5, floor(y), floor(z) + 0.5)

fun Vec3d.distanceToPlayer(): Double {
    val pos = McUtils.playerVec ?: return Double.MAX_VALUE
    return distanceTo(pos)
}

fun Vec3d.distanceSqToPlayer(): Double {
    val pos = McUtils.playerVec ?: return Double.MAX_VALUE
    return squaredDistanceTo(pos)
}

fun Vec3d.distanceIgnoreY(other: Vec3d): Double {
    val dx = x - other.x; val dz = z - other.z
    return sqrt(dx * dx + dz * dz)
}

fun Vec3d.distanceSqIgnoreY(other: Vec3d): Double {
    val dx = x - other.x; val dz = z - other.z
    return dx * dx + dz * dz
}

fun Vec3d.middle(other: Vec3d): Vec3d = Vec3d((x + other.x) * 0.5, (y + other.y) * 0.5, (z + other.z) * 0.5)

fun Vec3d.interpolate(other: Vec3d, t: Double): Vec3d = lerp(other, t)

fun Vec3d.boundingToOffset(dx: Double, dy: Double, dz: Double): Box =
    Box(x, y, z, x + dx, y + dy, z + dz)

fun Vec3d.axisAlignedTo(other: Vec3d): Box = Box(
    min(x, other.x), min(y, other.y), min(z, other.z),
    max(x, other.x), max(y, other.y), max(z, other.z),
)

fun Vec3d.expandBlock(n: Int = 1): Box {
    val bx = floor(x); val by = floor(y); val bz = floor(z)
    val e = n / 16.0
    return Box(bx - e, by - e, bz - e, bx + 1 + e, by + 1 + e, bz + 1 + e)
}

fun Vec3d.getBlockStateAt(): BlockState? =
    McUtils.world?.getBlockState(BlockPos.ofFloored(this))

fun Vec3d.getBlockAt(): Block? = getBlockStateAt()?.block

fun Vec3d.isInLoadedChunk(): Boolean {
    val world = McUtils.world ?: return false
    val bx = floor(x).toInt() shr 4
    val bz = floor(z).toInt() shr 4
    return world.isChunkLoaded(bx, bz)
}

fun Vec3d.distanceToLine(start: Vec3d, end: Vec3d): Double {
    val ab = end.subtract(start)
    val abLen2 = ab.lengthSquared()
    if (abLen2 == 0.0) return distanceTo(start)
    val t = (subtract(start).dotProduct(ab) / abLen2).coerceIn(0.0, 1.0)
    return distanceTo(start.add(ab.multiply(t)))
}

// -- BlockPos extensions -------------------------------------------------------

fun BlockPos.toVec3d(): Vec3d = Vec3d(x + 0.5, y.toDouble(), z + 0.5)

fun BlockPos.getBlockStateAt(): BlockState? = McUtils.world?.getBlockState(this)

fun BlockPos.getBlockAt(): Block? = getBlockStateAt()?.block

// -- Box extensions ------------------------------------------------------------

fun Box.expandBlock(n: Int = 1): Box = expand(n / 16.0)

fun Box.center(): Vec3d = Vec3d((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5)

fun Box.topCenter(): Vec3d = Vec3d((minX + maxX) * 0.5, maxY, (minZ + maxZ) * 0.5)

fun Box.isPlayerInside(): Boolean {
    val pos = McUtils.playerVec ?: return false
    return contains(pos)
}

fun Box.minVec(): Vec3d = Vec3d(minX, minY, minZ)

fun Box.maxVec(): Vec3d = Vec3d(maxX, maxY, maxZ)

fun Box.corners(): List<Vec3d> = listOf(
    Vec3d(minX, minY, minZ), Vec3d(maxX, minY, minZ),
    Vec3d(minX, maxY, minZ), Vec3d(maxX, maxY, minZ),
    Vec3d(minX, minY, maxZ), Vec3d(maxX, minY, maxZ),
    Vec3d(minX, maxY, maxZ), Vec3d(maxX, maxY, maxZ),
)
