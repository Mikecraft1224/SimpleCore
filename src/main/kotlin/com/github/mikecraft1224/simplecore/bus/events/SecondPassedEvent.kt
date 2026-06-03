@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents

/** Fired once per second (every 20 client ticks). */
class SecondPassedEvent : Event() {
    companion object : EventCompanion {
        private var registered = false
        private var tickCounter = 0

        override fun registerEvents() {
            if (registered) return
            ClientTickEvents.END_CLIENT_TICK.register {
                tickCounter++
                if (tickCounter >= 20) {
                    tickCounter = 0
                    EventRegistry.post { SecondPassedEvent() }
                }
            }
            registered = true
        }
    }
}
