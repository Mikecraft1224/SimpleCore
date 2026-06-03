@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.Event

/**
 * Fired once per server tick, derived from the `WorldTimeUpdateS2CPacket`.
 *
 * [time] is the raw world time value from the packet.
 */
class ServerTickEvent(val time: Long) : Event()
