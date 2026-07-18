@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

// -- Vec3 extensions ----------------------------------------------------------

fun Vec3.up(n: Double = 1.0): Vec3 = Vec3(x, y + n, z)
fun Vec3.down(n: Double = 1.0): Vec3 = Vec3(x, y - n, z)

fun Vec3.roundToBlock(): Vec3 = Vec3(floor(x), floor(y), floor(z))

fun Vec3.blockCenter(): Vec3 = Vec3(floor(x) + 0.5, floor(y), floor(z) + 0.5)

fun Vec3.distanceToPlayer(): Double {
    val pos = McUtils.playerVec ?: return Double.MAX_VALUE
    return distanceTo(pos)
}

fun Vec3.distanceSqToPlayer(): Double {
    val pos = McUtils.playerVec ?: return Double.MAX_VALUE
    return distanceToSqr(pos)
}

fun Vec3.distanceIgnoreY(other: Vec3): Double {
    val dx = x - other.x; val dz = z - other.z
    return sqrt(dx * dx + dz * dz)
}

fun Vec3.distanceSqIgnoreY(other: Vec3): Double {
    val dx = x - other.x; val dz = z - other.z
    return dx * dx + dz * dz
}

fun Vec3.middle(other: Vec3): Vec3 = Vec3((x + other.x) * 0.5, (y + other.y) * 0.5, (z + other.z) * 0.5)

fun Vec3.interpolate(other: Vec3, t: Double): Vec3 = lerp(other, t)

fun Vec3.boundingToOffset(dx: Double, dy: Double, dz: Double): AABB =
    AABB(x, y, z, x + dx, y + dy, z + dz)

fun Vec3.axisAlignedTo(other: Vec3): AABB = AABB(
    min(x, other.x), min(y, other.y), min(z, other.z),
    max(x, other.x), max(y, other.y), max(z, other.z),
)

fun Vec3.expandBlock(n: Int = 1): AABB {
    val bx = floor(x); val by = floor(y); val bz = floor(z)
    val e = n / 16.0
    return AABB(bx - e, by - e, bz - e, bx + 1 + e, by + 1 + e, bz + 1 + e)
}

fun Vec3.getBlockStateAt(): BlockState? =
    McUtils.world?.getBlockState(BlockPos.containing(this))

fun Vec3.getBlockAt(): Block? = getBlockStateAt()?.block

fun Vec3.isInLoadedChunk(): Boolean {
    val world = McUtils.world ?: return false
    val bx = floor(x).toInt() shr 4
    val bz = floor(z).toInt() shr 4
    return world.hasChunk(bx, bz)
}

fun Vec3.distanceToLine(start: Vec3, end: Vec3): Double {
    val ab = end.subtract(start)
    val abLen2 = ab.lengthSqr()
    if (abLen2 == 0.0) return distanceTo(start)
    val t = (subtract(start).dot(ab) / abLen2).coerceIn(0.0, 1.0)
    return distanceTo(start.add(ab.scale(t)))
}

// -- BlockPos extensions -------------------------------------------------------

fun BlockPos.toVec3d(): Vec3 = Vec3(x + 0.5, y.toDouble(), z + 0.5)

fun BlockPos.getBlockStateAt(): BlockState? = McUtils.world?.getBlockState(this)

fun BlockPos.getBlockAt(): Block? = getBlockStateAt()?.block

// -- AABB extensions ------------------------------------------------------------

fun AABB.expandBlock(n: Int = 1): AABB = inflate(n / 16.0)

fun AABB.center(): Vec3 = Vec3((minX + maxX) * 0.5, (minY + maxY) * 0.5, (minZ + maxZ) * 0.5)

fun AABB.topCenter(): Vec3 = Vec3((minX + maxX) * 0.5, maxY, (minZ + maxZ) * 0.5)

fun AABB.isPlayerInside(): Boolean {
    val pos = McUtils.playerVec ?: return false
    return contains(pos)
}

fun AABB.minVec(): Vec3 = Vec3(minX, minY, minZ)

fun AABB.maxVec(): Vec3 = Vec3(maxX, maxY, maxZ)

fun AABB.corners(): List<Vec3> = listOf(
    Vec3(minX, minY, minZ), Vec3(maxX, minY, minZ),
    Vec3(minX, maxY, minZ), Vec3(maxX, maxY, minZ),
    Vec3(minX, minY, maxZ), Vec3(maxX, minY, maxZ),
    Vec3(minX, maxY, maxZ), Vec3(maxX, maxY, maxZ),
)
