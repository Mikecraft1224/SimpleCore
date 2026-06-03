@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.particle.ParticleEffect
import net.minecraft.util.math.Vec3d

/**
 * Fired when the client receives a particle spawn packet from the server.
 *
 * Cancelling suppresses the particle from being spawned on the client.
 */
class ReceiveParticleEvent(
    val parameters: ParticleEffect,
    val pos: Vec3d,
    val count: Int,
    val speed: Float,
    val offset: Vec3d,
) : CancellableEvent()
