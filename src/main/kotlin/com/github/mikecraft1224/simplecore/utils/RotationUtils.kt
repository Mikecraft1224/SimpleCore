@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.core.Direction
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Reads and sets the local player's real look rotation (yaw/pitch).
 *
 * Every function here changes what's actually rendered and what's actually sent to the server,
 * exactly as if the player had moved the mouse - there is no "fake" rotation mode that desyncs
 * the client's view from what the server is told. That kind of rotation spoofing is a
 * hit-validation bypass most servers treat as cheating, and isn't something this framework provides.
 */
object RotationUtils {
    /** Current real yaw (left/right), in degrees. */
    val yaw: Float get() = McUtils.player?.yRot ?: 0f

    /** Current real pitch (up/down), in degrees. -90 = straight up, 90 = straight down. */
    val pitch: Float get() = McUtils.player?.xRot ?: 0f

    /** Instantly sets the real look rotation. */
    fun set(newYaw: Float, newPitch: Float) {
        val player = McUtils.player ?: return
        player.setYRot(normalizeYaw(newYaw))
        player.setXRot(newPitch.coerceIn(-90f, 90f))
    }

    /** Instantly sets yaw only, keeping the current pitch. */
    fun setYaw(newYaw: Float) = set(newYaw, pitch)

    /** Instantly sets pitch only, keeping the current yaw. */
    fun setPitch(newPitch: Float) = set(yaw, newPitch)

    /** Wraps [value] into the `(-180, 180]` range Minecraft uses internally for yaw. */
    fun normalizeYaw(value: Float): Float {
        var y = value % 360f
        if (y >= 180f) y -= 360f
        if (y < -180f) y += 360f
        return y
    }

    /** Snaps the current yaw to the nearest multiple of [increment] degrees (e.g. 45 or 90). */
    fun snapYaw(increment: Float = 45f) {
        setYaw(Math.round(yaw / increment) * increment)
    }

    /** Snaps the current pitch to the nearest multiple of [increment] degrees. */
    fun snapPitch(increment: Float = 45f) {
        setPitch(Math.round(pitch / increment) * increment)
    }

    /** Resets pitch to 0 - looking level with the horizon. */
    fun resetPitch() = setPitch(0f)

    /** Returns the horizontal [Direction] (N/S/E/W) closest to the current yaw. */
    fun nearestCardinal(): Direction = Direction.fromYRot(yaw.toDouble())

    /** Snaps yaw to the nearest cardinal direction (N/S/E/W). */
    fun snapToCardinal() = face(nearestCardinal())

    /** Instantly turns to face [direction] - must be one of the 4 horizontal directions. */
    fun face(direction: Direction) = setYaw(direction.toYRot())

    /** Computes the yaw/pitch that would look from [from] directly at [to]. Does not apply it - see [set]. */
    fun angleTo(from: Vec3, to: Vec3): Pair<Float, Float> {
        val dx = to.x - from.x
        val dy = to.y - from.y
        val dz = to.z - from.z
        val horizontalDist = sqrt(dx * dx + dz * dz)
        val computedYaw = (Math.toDegrees(atan2(dz, dx)) - 90.0).toFloat()
        val computedPitch = (-Math.toDegrees(atan2(dy, horizontalDist))).toFloat()
        return normalizeYaw(computedYaw) to computedPitch.coerceIn(-90f, 90f)
    }

    /** Instantly turns to face [target] from the player's current eye position. */
    fun lookAt(target: Vec3) {
        val eyePos = McUtils.playerEyePos ?: return
        val (targetYaw, targetPitch) = angleTo(eyePos, target)
        set(targetYaw, targetPitch)
    }

    /**
     * Smoothly turns from the current rotation to [targetYaw]/[targetPitch] over [ticks] client
     * ticks (linear interpolation, taking the shortest path around the yaw wraparound).
     *
     * Overrides the player's look every tick during the turn - if they move the mouse themselves
     * mid-turn, this will keep fighting for control until [ticks] elapses.
     */
    fun smoothTurnTo(targetYaw: Float, targetPitch: Float, ticks: Int = 10) {
        val player = McUtils.player ?: return
        val startYaw = player.yRot
        val startPitch = player.xRot
        val delta = normalizeYaw(targetYaw - startYaw)
        val steps = ticks.coerceAtLeast(1)
        for (i in 1..steps) {
            val t = i.toFloat() / steps
            Scheduler.runDelayed(i) {
                set(startYaw + delta * t, startPitch + (targetPitch - startPitch) * t)
            }
        }
    }

    /** Smoothly turns to face [target] over [ticks] client ticks - see [smoothTurnTo]. */
    fun smoothLookAt(target: Vec3, ticks: Int = 10) {
        val eyePos = McUtils.playerEyePos ?: return
        val (targetYaw, targetPitch) = angleTo(eyePos, target)
        smoothTurnTo(targetYaw, targetPitch, ticks)
    }
}
