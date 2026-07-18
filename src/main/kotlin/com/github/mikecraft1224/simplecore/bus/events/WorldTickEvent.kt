@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.multiplayer.ClientLevel

class WorldTickEvent(val world: ClientLevel, val tickCount: Int, val phase: Phase) : Event() {
    enum class Phase {
        START,
        END
    }

    companion object : EventCompanion {
        private var registered = false
        private var totalStartTicks = 0
        private var totalEndTicks = 0

        override fun registerEvents() {
            if (registered) return

            ClientTickEvents.START_LEVEL_TICK.register { world ->
                totalStartTicks++
                EventRegistry.post { WorldTickEvent(world, totalStartTicks, Phase.START) }
            }
            ClientTickEvents.END_LEVEL_TICK.register { world ->
                totalEndTicks++
                EventRegistry.post { WorldTickEvent(world, totalEndTicks, Phase.END) }
            }

            registered = true
        }
    }
}