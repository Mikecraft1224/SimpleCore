@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.world.phys.Vec3

/**
 * Fired when the client receives a particle spawn packet from the server.
 *
 * Cancelling suppresses the particle from being spawned on the client.
 */
class ReceiveParticleEvent(
    val parameters: ParticleOptions,
    val pos: Vec3,
    val count: Int,
    val speed: Float,
    val offset: Vec3,
) : CancellableEvent()
