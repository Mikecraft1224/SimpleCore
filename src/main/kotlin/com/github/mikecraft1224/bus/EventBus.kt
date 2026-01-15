package com.github.mikecraft1224.bus

import com.github.mikecraft1224.Logger
import com.github.mikecraft1224.bus.api.Event
import com.github.mikecraft1224.bus.api.EventPriority
import com.github.mikecraft1224.bus.api.Subscribe
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A simple event bus implementation that allows registering and unregistering event handlers,
 * as well as posting events to those handlers.
 *
 * Notes for creating your own EventBus:
 * Consider loading events dynamically only if there are handlers registered for them!
 * This can be checked using the `existHandlers` method or `getRegisteredEventClasses` method.
 * You can look up how this can be done inside the `FeatureAutoLoader` and `ClientTickEvent` classes.
 */
class EventBus {
    private data class Handler(
        val owner: Any,
        val method: Method,
        val paramType: Class<out Event<*>>,
        val priority: EventPriority,
        val receiveCancelled: Boolean
    )

    private val handlers: ConcurrentHashMap<Class<out Event<*>>, Array<CopyOnWriteArrayList<Handler>>> = ConcurrentHashMap()
    private val priorities: Array<EventPriority> = EventPriority.entries.toTypedArray()

    private fun bucketsFor(eventClass: Class<out Event<*>>): Array<CopyOnWriteArrayList<Handler>> =
        handlers.computeIfAbsent(eventClass) { Array(priorities.size) { CopyOnWriteArrayList<Handler>() } }

    fun registerFeature(instance: Any) {
        val clazz = instance.javaClass

        for (m in clazz.declaredMethods) {
            val ann = m.getAnnotation(Subscribe::class.java) ?: continue
            if (m.parameterCount != 1 || !Event::class.java.isAssignableFrom(m.parameterTypes[0])) {
                Logger.warn("Method ${m.name} in class ${clazz.name} is annotated with @Subscribe but does not have a single parameter of type Event.")
            }

            @Suppress("UNCHECKED_CAST")
            val param = m.parameterTypes[0] as Class<out Event<*>>
            m.isAccessible = true

            val handler = Handler(instance, m, param, ann.priority, ann.receiveCancelled)
            bucketsFor(param)[handler.priority.ordinal].add(handler)
        }
    }

    fun unregisterFeature(instance: Any) {
        for ((_, buckets) in handlers) {
            for (bucket in buckets) {
                bucket.removeIf { it.owner == instance }
            }
        }

        handlers.entries.removeIf { (_, buckets) -> buckets.all { it.isEmpty() } }
    }

    fun post(event: Event<*>) {
        val eventClass = event.javaClass
        val buckets = handlers[eventClass] ?: return

        for (p in buckets.indices.reversed()) {
            val bucket = buckets[p]
            if (bucket.isEmpty()) continue

            val cancelledAtStart = event.isCancelled

            for (handler in bucket) {
                if (cancelledAtStart && !handler.receiveCancelled) continue

                try {
                    handler.method.invoke(handler.owner, event)
                } catch (e: Exception) {
                    Logger.error("Error invoking event handler ${handler.method.name} in class ${handler.owner.javaClass.name}: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    fun existHandlers(eventClass: Class<out Event<*>>): Boolean {
        return handlers[eventClass]?.any { bucket -> bucket.isNotEmpty() } == true
    }

    fun getRegisteredEventClasses(): Set<Class<out Event<*>>> {
        return handlers.keys
    }
}