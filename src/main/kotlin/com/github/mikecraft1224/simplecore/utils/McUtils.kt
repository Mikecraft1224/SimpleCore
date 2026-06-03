@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.client.world.ClientWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

/**
 * Null-safe accessors for common Minecraft client singletons.
 *
 * All properties must be read from the client thread only. Event handlers registered
 * via `@Subscribe` always run on the client thread.
 *
 * Check [isInGame] at the top of any handler that reads world or player state:
 * ```kotlin
 * @Subscribe
 * fun onTick(event: ClientTickEvent) {
 *     if (!McUtils.isInGame) return
 *     val pos = McUtils.playerPos ?: return
 *     // safe to access world and player here
 * }
 * ```
 */
object McUtils {
    val mc: MinecraftClient get() = MinecraftClient.getInstance()
    val player: ClientPlayerEntity? get() = mc.player
    val world: ClientWorld? get() = mc.world
    /** `true` when both [player] and [world] are non-null. */
    val isInGame: Boolean get() = player != null && world != null
    /** `true` when a GUI screen (inventory, chat, etc.) is currently open. */
    val isScreenOpen: Boolean get() = mc.currentScreen != null
    val playerPos: BlockPos? get() = player?.blockPos
    /** Foot-level position of the local player as [Vec3d]. */
    val playerVec: Vec3d? get() = player?.let { Vec3d(it.x, it.y, it.z) }
    /** Eye-level position of the local player. */
    val playerEyePos: Vec3d? get() = player?.eyePos
    val isFirstPerson: Boolean get() = mc.options.perspective.isFirstPerson
}
