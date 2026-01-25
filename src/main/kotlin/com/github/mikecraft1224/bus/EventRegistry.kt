package com.github.mikecraft1224.bus

import com.github.mikecraft1224.bus.api.Event
import java.util.concurrent.ConcurrentHashMap

object EventRegistry {
    private val busesByEventClass = ConcurrentHashMap<Class<out Event<*>>, MutableSet<EventBus>>()

    fun addBus(eventClass: Class<out Event<*>>, bus: EventBus) {
        val buses = busesByEventClass.computeIfAbsent(eventClass) { ConcurrentHashMap.newKeySet() }
        buses.add(bus)
    }

    fun getBuses(eventClass: Class<out Event<*>>): Set<EventBus> {
        return busesByEventClass[eventClass] ?: emptySet()
    }

    fun <E : Event<*>> post(factory: () -> E): Boolean {
        val sampleEvent = factory()
        @Suppress("UNCHECKED_CAST")
        val buses = getBuses(sampleEvent.javaClass as Class<out Event<*>>)
        if (buses.isEmpty()) return false

        var anyCancelled = sampleEvent.isCancelled
        val iterator = buses.iterator()

        if (iterator.hasNext()) {
            iterator.next().post(sampleEvent)
            anyCancelled = anyCancelled || sampleEvent.isCancelled
        }

        while (iterator.hasNext()) {
            val event = factory()
            iterator.next().post(event)
            anyCancelled = anyCancelled || event.isCancelled
        }

        return anyCancelled
    }

    fun post(event: Event<*>) {
        val buses = getBuses(event.javaClass)
        for (bus in buses) {
            bus.post(event)
        }
    }
}