package com.github.mikecraft1224.bus.events

import com.github.mikecraft1224.bus.EventRegistry
import com.github.mikecraft1224.bus.api.Event
import com.github.mikecraft1224.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.world.ClientWorld

@Suppress("UNUSED")
class WorldTickEvent(val world: ClientWorld, val tickCount: Int, val phase: Phase) : Event<WorldTickEvent>() {
    enum class Phase {
        START,
        END
    }

    companion object : EventCompanion<WorldTickEvent> {
        var registered = false
        private var totalStartTicks = 0
        private var totalEndTicks = 0

        override fun registerEvents() {
            if (registered) return

            ClientTickEvents.START_WORLD_TICK.register { world ->
                totalStartTicks++
                EventRegistry.post { WorldTickEvent(world, totalStartTicks, Phase.START) }
            }
            ClientTickEvents.END_WORLD_TICK.register { world ->
                totalEndTicks++
                EventRegistry.post { WorldTickEvent(world, totalEndTicks, Phase.END) }
            }

            registered = true
        }
    }
}