@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import com.github.mikecraft1224.simplecore.input.internal.ScreenTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

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
    val mc: Minecraft get() = Minecraft.getInstance()
    val player: LocalPlayer? get() = mc.player
    val world: ClientLevel? get() = mc.level
    /** `true` when both [player] and [world] are non-null. */
    val isInGame: Boolean get() = player != null && world != null
    val isScreenOpen: Boolean get() = ScreenTracker.currentScreen != null
    val playerPos: BlockPos? get() = player?.blockPosition()
    /** Foot-level position of the local player as [Vec3]. */
    val playerVec: Vec3? get() = player?.let { Vec3(it.x, it.y, it.z) }
    /** Eye-level position of the local player. */
    val playerEyePos: Vec3? get() = player?.eyePosition
    val isFirstPerson: Boolean get() = mc.options.cameraType.isFirstPerson
}
