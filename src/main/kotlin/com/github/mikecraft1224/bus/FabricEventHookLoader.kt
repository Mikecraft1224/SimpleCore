package com.github.mikecraft1224.bus

import com.github.mikecraft1224.bus.api.Event
import java.lang.reflect.Method

object FabricEventHookLoader {
    fun hookUsedEvents(bus: EventBus) {
        for (eventClass in bus.getRegisteredEventClasses()) {
            EventRegistry.addBus(eventClass, bus)

            val hookMethod = findHookMethod(eventClass) ?: run {
                continue
            }

            try {
                hookMethod.invoke(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun findHookMethod(eventClass: Class<out Event<*>>): Method? {
        return runCatching {
            eventClass.getDeclaredMethod("registerEvents").apply { isAccessible = true }
        }.getOrNull()
    }
}