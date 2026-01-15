package com.github.mikecraft1224.bus.events

import com.github.mikecraft1224.bus.EventRegistry
import com.github.mikecraft1224.bus.api.Event
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient

class ClientTickEvent(val client: MinecraftClient, val tickCount: Int, val phase: Phase) : Event<ClientTickEvent>() {
    enum class Phase {
        START,
        END
    }

    /**
     * This companion object is responsible for registering the client tick events and handling everything related to it.
     */
    companion object {
        private var registered = false
        private var totalStartTicks = 0
        private var totalEndTicks = 0

        /**
         * Registers the client tick events to the Fabric event system.
         * This method should be called during the mod initialization phase to ensure that the events are properly registered.
         * This ensures that not required events are not registered at all if there are no handlers for them.
         */
        fun registerEvents() {
            if (registered) return

            ClientTickEvents.START_CLIENT_TICK.register { client ->
                totalStartTicks++
                EventRegistry.post { ClientTickEvent(client, totalStartTicks, Phase.START)}
            }
            ClientTickEvents.END_CLIENT_TICK.register { client ->
                totalEndTicks++
                EventRegistry.post { ClientTickEvent(client, totalEndTicks, Phase.END) }
            }

            registered = true
        }
    }
}