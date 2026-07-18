@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.world.entity.player.Input

/**
 * Simulates the player's real movement input (WASD/jump/sprint/sneak).
 *
 * This drives the same [net.minecraft.client.player.ClientInput] state real key presses do, so it
 * behaves identically to genuine input for movement physics, server-side validation, and anti-cheat -
 * there's no position/velocity teleportation helper here, only input simulation.
 */
object MovementUtils {
    /**
     * Sets the player's held movement keys directly. Persists until changed again or
     * [stopMovement] is called - this is "holding down" the given keys, not a one-shot action.
     */
    fun setInput(
        forward: Boolean = false,
        backward: Boolean = false,
        left: Boolean = false,
        right: Boolean = false,
        jump: Boolean = false,
        sneak: Boolean = false,
        sprint: Boolean = false,
    ) {
        val input = McUtils.player?.input ?: return
        input.keyPresses = Input(forward, backward, left, right, jump, sneak, sprint)
    }

    /** Releases all movement keys - equivalent to letting go of every key. */
    fun stopMovement() = setInput()

    /** Starts walking forward, optionally sprinting. Call [stopMovement] to stop. */
    fun walkForward(sprint: Boolean = false) = setInput(forward = true, sprint = sprint)

    /** Triggers a single jump - same as tapping the jump key once. */
    fun jump() {
        McUtils.player?.input?.makeJump()
    }
}
