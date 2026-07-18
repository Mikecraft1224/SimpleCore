@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.Minecraft

/**
 * Fired once per client tick at both the [START][Phase.START] and [END][Phase.END] of each tick.
 *
 * For most game-state reads (world, player, entities), subscribe to [Phase.END] since those
 * are fully updated by that point. Use [Phase.START] only when you need to act before the tick runs.
 *
 * Use an [EventFilter][com.github.mikecraft1224.simplecore.bus.api.EventFilter] to avoid the phase check inside the handler:
 * ```kotlin
 * object EndOnly : EventFilter<ClientTickEvent> {
 *     override fun test(e: ClientTickEvent) = e.phase == ClientTickEvent.Phase.END
 * }
 *
 * @Subscribe(filter = EndOnly::class)
 * fun onTick(event: ClientTickEvent) {
 *     if (!McUtils.isInGame) return
 *     // game state is fully updated here
 * }
 * ```
 *
 * @property client    The Minecraft client instance.
 * @property tickCount Monotonically increasing counter. Incremented independently per phase.
 * @property phase     Whether this is a START or END tick.
 */
class ClientTickEvent(val client: Minecraft, val tickCount: Int, val phase: Phase) : Event() {
    enum class Phase { START, END }

    /** Returns `true` every [i] ticks, offset by [offset]. Useful for rate-limiting handlers. */
    fun isMod(i: Int, offset: Int = 0): Boolean = (tickCount + offset) % i == 0

    companion object : EventCompanion {
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
