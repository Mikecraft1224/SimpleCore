@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

/**
 * Returns the nearest entity of type [T] within [radius] blocks of the local player matching
 * [predicate], or `null` if the player/world aren't loaded or none match.
 * ```kotlin
 * val target = nearestEntity<Mob>(32.0) { !it.isRemoved }
 * ```
 */
inline fun <reified T : Entity> nearestEntity(radius: Double, noinline predicate: (T) -> Boolean = { true }): T? {
    val player = McUtils.player ?: return null
    val world = McUtils.world ?: return null
    return world.getEntitiesOfClass(T::class.java, player.boundingBox.inflate(radius), predicate)
        .minByOrNull { it.distanceToSqr(player) }
}

/** Returns all entities of type [T] within [radius] blocks of the local player matching [predicate]. */
inline fun <reified T : Entity> entitiesNear(radius: Double, noinline predicate: (T) -> Boolean = { true }): List<T> {
    val player = McUtils.player ?: return emptyList()
    val world = McUtils.world ?: return emptyList()
    return world.getEntitiesOfClass(T::class.java, player.boundingBox.inflate(radius), predicate)
}

/** Returns the nearest entity of type [T] to [pos] within [radius] blocks matching [predicate]. */
inline fun <reified T : Entity> nearestEntityTo(pos: Vec3, radius: Double, noinline predicate: (T) -> Boolean = { true }): T? {
    val world = McUtils.world ?: return null
    val box = AABB(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius, pos.z + radius)
    return world.getEntitiesOfClass(T::class.java, box, predicate)
        .minByOrNull { it.distanceToSqr(pos.x, pos.y, pos.z) }
}
