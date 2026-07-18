@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

/**
 * Casts a ray from [from] to [to] against terrain only (no entities), using the same collision
 * rules vanilla uses for its own crosshair block-picking ([ClipContext.Block.OUTLINE], no fluids).
 * Returns `null` only if the world isn't loaded; a miss is a non-null [BlockHitResult] whose
 * [BlockHitResult.getType] is [HitResult.Type.MISS].
 */
fun raycastBlocks(from: Vec3, to: Vec3): BlockHitResult? {
    val world = McUtils.world ?: return null
    val player = McUtils.player ?: return null
    return world.clip(ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player))
}

/** `true` if no block blocks a straight line between [from] and [to]. */
fun canSee(from: Vec3, to: Vec3): Boolean = raycastBlocks(from, to)?.type == HitResult.Type.MISS

/** `true` if the local player has a clear line of sight to [entity]'s hitbox center. */
fun canSee(entity: Entity): Boolean {
    val eyePos = McUtils.playerEyePos ?: return false
    return canSee(eyePos, entity.boundingBox.center())
}

/**
 * Returns the entity of type [T] the local player is looking directly at within [maxDistance]
 * blocks matching [predicate], or `null` if none. Ignores terrain occlusion between the player
 * and the entity - combine with [canSee] if line-of-sight matters too.
 *
 * Mirrors vanilla's own crosshair entity-picking ([ProjectileUtil.getEntityHitResult]) rather
 * than reimplementing ray-AABB intersection.
 */
inline fun <reified T : Entity> entityLookingAt(maxDistance: Double = 20.0, noinline predicate: (T) -> Boolean = { true }): T? {
    val player = McUtils.player ?: return null
    val lookVec = player.getViewVector(1f)
    val eyePos = player.eyePosition
    val reach = eyePos.add(lookVec.scale(maxDistance))
    val searchBox = player.boundingBox.expandTowards(lookVec.scale(maxDistance)).inflate(1.0)
    val hit = ProjectileUtil.getEntityHitResult(
        player, eyePos, reach, searchBox,
        { candidate: Entity -> candidate is T && predicate(candidate) },
        maxDistance * maxDistance,
    )
    return hit?.entity as? T
}
