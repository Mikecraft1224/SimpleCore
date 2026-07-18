@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.resources.Identifier
import net.minecraft.world.phys.Vec3

/**
 * Fired when the server sends a sound play packet to the client.
 *
 * Cancelling suppresses the sound from playing.
 */
class PlaySoundEvent(
    val soundId: Identifier,
    val pos: Vec3,
    val volume: Float,
    val pitch: Float,
) : CancellableEvent()
