package com.github.mikecraft1224.bus

import com.github.mikecraft1224.bus.api.Event
import java.util.concurrent.ConcurrentHashMap

object EventRegistry {
    private val busesByEventClass = ConcurrentHashMap<Class<out Event<*>>, MutableSet<EventBus>>()

    fun addBus(eventClass: Class<out Event<*>>, bus: EventBus) {
        val buses = busesByEventClass.computeIfAbsent(eventClass) { mutableSetOf() }
        buses.add(bus)
    }

    fun getBuses(eventClass: Class<out Event<*>>): Set<EventBus> {
        return busesByEventClass[eventClass] ?: emptySet()
    }

    inline fun <reified E : Event<*>> post(factory: () -> E) {
        @Suppress("UNCHECKED_CAST")
        val buses = getBuses(E::class.java as Class<out Event<*>>)
        for (bus in buses) {
            bus.post(factory())
        }
    }
}