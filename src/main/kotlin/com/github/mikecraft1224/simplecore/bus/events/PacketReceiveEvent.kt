@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.network.protocol.Packet

/**
 * Fired for every packet the client receives from the server, before vanilla handles it -
 * covers packet types with no dedicated SimpleCore event (e.g. server-specific protocol
 * extensions) without needing a new mixin per packet type.
 *
 * Cancelling drops the packet entirely; vanilla never sees it. Fires for every single packet
 * (movement, chunk data, etc.) - filter by [packet]'s type early and return quickly for types
 * you don't care about.
 * ```kotlin
 * @Subscribe
 * fun onPacket(event: PacketReceiveEvent) {
 *     val packet = event.packet as? ClientboundCustomPacket ?: return
 *     // ...
 * }
 * ```
 */
class PacketReceiveEvent(val packet: Packet<*>) : CancellableEvent()
