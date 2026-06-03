@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.util.Identifier
import net.minecraft.util.math.Vec3d

/**
 * Fired when the server sends a sound play packet to the client.
 *
 * Cancelling suppresses the sound from playing.
 */
class PlaySoundEvent(
    val soundId: Identifier,
    val pos: Vec3d,
    val volume: Float,
    val pitch: Float,
) : CancellableEvent()
