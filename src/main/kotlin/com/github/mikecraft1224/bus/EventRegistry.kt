package com.github.mikecraft1224.bus

import com.github.mikecraft1224.bus.api.CancellableEvent
import com.github.mikecraft1224.bus.api.Event
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

@Suppress("UNUSED")
object EventRegistry {
    private val busesByEventClass = ConcurrentHashMap<KClass<out Event>, MutableSet<EventBus>>()

    fun addBus(eventClass: KClass<out Event>, bus: EventBus) {
        val buses = busesByEventClass.computeIfAbsent(eventClass) { ConcurrentHashMap.newKeySet() }
        buses.add(bus)
    }

    fun getBuses(eventClass: KClass<out Event>): Set<EventBus> {
        return busesByEventClass[eventClass] ?: emptySet()
    }

    /**
     * Posts an event to every [EventBus] registered for that event's class.
     *
     * **Multi-bus isolation:** The first bus receives the instance returned by [factory]. Every
     * subsequent bus receives a *fresh* instance produced by calling [factory] again. This is
     * intentional - cancellation or mutation by one bus cannot bleed into another bus's handlers.
     *
     * Cancellation is only tracked when [E] is a [CancellableEvent]. For non-cancellable events
     * this always returns `false`.
     *
     * @param factory Produces event instances. Called once per bus (once for the first bus, then
     *   once per additional bus).
     * @return `true` if the event was cancelled by *any* bus's handlers; `false` otherwise.
     *   Note that because each bus receives its own event instance, cancellation is tracked
     *   per-instance and the results are OR-ed together.
     */
    fun <E : Event> post(factory: () -> E): Boolean {
        val sampleEvent = factory()
        val buses = getBuses(sampleEvent::class)
        if (buses.isEmpty()) return false

        var anyCancelled = (sampleEvent as? CancellableEvent)?.isCancelled ?: false
        val iterator = buses.iterator()

        if (iterator.hasNext()) {
            iterator.next().post(sampleEvent)
            anyCancelled = anyCancelled || (sampleEvent as? CancellableEvent)?.isCancelled ?: false
        }

        while (iterator.hasNext()) {
            val event = factory()
            iterator.next().post(event)
            anyCancelled = anyCancelled || (event as? CancellableEvent)?.isCancelled ?: false
        }

        return anyCancelled
    }

    fun post(event: Event) {
        val buses = getBuses(event::class)
        for (bus in buses) {
            bus.post(event)
        }
    }
}