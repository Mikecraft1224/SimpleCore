@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents

/**
 * Fired when the client joins or leaves a world.
 *
 * @property joining `true` when joining, `false` when disconnecting.
 */
class WorldChangeEvent(val joining: Boolean) : Event() {
    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return
            ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
                EventRegistry.post { WorldChangeEvent(joining = true) }
            }
            ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
                EventRegistry.post { WorldChangeEvent(joining = false) }
            }
            registered = true
        }
    }
}
