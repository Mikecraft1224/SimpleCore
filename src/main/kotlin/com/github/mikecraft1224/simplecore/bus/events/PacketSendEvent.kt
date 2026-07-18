@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.network.protocol.Packet

/**
 * Fired for every packet the client sends to the server, before it's written to the network -
 * the outgoing counterpart to [PacketReceiveEvent].
 *
 * Cancelling drops the packet entirely; the server never receives it. Fires for every single
 * packet (movement, keep-alive, etc.) - filter by [packet]'s type early and return quickly for
 * types you don't care about.
 */
class PacketSendEvent(val packet: Packet<*>) : CancellableEvent()
