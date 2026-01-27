package com.github.mikecraft1224.bus.events

import com.github.mikecraft1224.bus.EventRegistry
import com.github.mikecraft1224.bus.api.Event
import com.github.mikecraft1224.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient

class ClientTickEvent(val client: MinecraftClient, val tickCount: Int, val phase: Phase) : Event<ClientTickEvent>() {
    enum class Phase {
        START,
        END
    }

    // Registration
    companion object : EventCompanion<ClientTickEvent> {
        private var registered = false
        private var totalStartTicks = 0
        private var totalEndTicks = 0

        override fun registerEvents() {
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