@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import kotlin.time.Duration

/**
 * Runs a task after a tick or time delay, without hand-rolling a tick counter in a
 * [com.github.mikecraft1224.simplecore.bus.events.ClientTickEvent] handler every time:
 * ```kotlin
 * Scheduler.runDelayed(20) { ChatUtils.print("1 second later") }
 * Scheduler.runDelayed(2.seconds) { ChatUtils.print("also 1 second later, wall-clock based") }
 * ```
 *
 * Self-registers a single [ClientTickEvents.END_CLIENT_TICK] hook on first access - no explicit
 * setup required, unlike [com.github.mikecraft1224.simplecore.bus.events.RenderWorldEvent].
 */
object Scheduler {
    private class TickTask(val runAtTick: Long, val block: () -> Unit)
    private class TimeTask(val runAt: TimeMark, val block: () -> Unit)

    private var currentTick = 0L
    private val tickTasks = mutableListOf<TickTask>()
    private val timeTasks = mutableListOf<TimeTask>()

    init {
        ClientTickEvents.END_CLIENT_TICK.register {
            currentTick++

            val dueTicks = tickTasks.filter { it.runAtTick <= currentTick }
            if (dueTicks.isNotEmpty()) {
                tickTasks.removeAll(dueTicks)
                dueTicks.forEach { it.block() }
            }

            val dueTimes = timeTasks.filter { it.runAt.isInPast() }
            if (dueTimes.isNotEmpty()) {
                timeTasks.removeAll(dueTimes)
                dueTimes.forEach { it.block() }
            }
        }
    }

    /** Runs [block] after [ticks] client ticks have elapsed (minimum next-tick). */
    fun runDelayed(ticks: Int, block: () -> Unit) {
        tickTasks += TickTask(currentTick + ticks.coerceAtLeast(1), block)
    }

    /** Runs [block] once at least [duration] has passed, checked once per tick. */
    fun runDelayed(duration: Duration, block: () -> Unit) {
        timeTasks += TimeTask(TimeMark.future(duration), block)
    }

    /** Runs [block] on the very next client tick. */
    fun runNextTick(block: () -> Unit) = runDelayed(1, block)
}
