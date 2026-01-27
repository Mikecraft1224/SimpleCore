package com.github.mikecraft1224.bus

import com.github.mikecraft1224.Logger
import com.github.mikecraft1224.bus.api.Event
import com.github.mikecraft1224.bus.api.EventPriority
import com.github.mikecraft1224.bus.api.Subscribe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass

/**
 * A simple event bus implementation that allows registering and unregistering event handlers,
 * as well as posting events to those handlers.
 *
 * Notes for creating your own EventBus:
 * Consider loading events dynamically only if there are handlers registered for them!
 * This can be checked using the `existHandlers` method or `getRegisteredEventClasses` method.
 * You can look up how this can be done inside the `FeatureAutoLoader` and `ClientTickEvent` classes.
 */
@Suppress("UNUSED")
class EventBus {
    private data class Handler(
        val owner: Any,
        val handle: MethodHandle,
        val priority: EventPriority,
        val receiveCancelled: Boolean
    )

    private val handlers: ConcurrentHashMap<KClass<out Event<*>>, CopyOnWriteArrayList<Handler>> = ConcurrentHashMap()
    private val lookup = MethodHandles.lookup()

    private fun handlersFor(eventClass: KClass<out Event<*>>): CopyOnWriteArrayList<Handler> =
        handlers.computeIfAbsent(eventClass) { CopyOnWriteArrayList() }

    fun registerFeature(instance: Any) {
        val clazz = instance.javaClass

        for (m in clazz.declaredMethods) {
            val ann = m.getAnnotation(Subscribe::class.java) ?: continue
            if (m.parameterCount != 1 || !Event::class.java.isAssignableFrom(m.parameterTypes[0])) {
                Logger.warn("Method ${m.name} in class ${clazz.name} is annotated with @Subscribe but does not have a single parameter of type Event.")
                continue
            }

            @Suppress("UNCHECKED_CAST")
            val param = (m.parameterTypes[0] as Class<out Event<*>>).kotlin
            m.isAccessible = true

            val handle = lookup.unreflect(m).bindTo(instance)
            val handler = Handler(instance, handle, ann.priority, ann.receiveCancelled)
            val list = handlersFor(param)
            list.add(handler)
            list.sortByDescending { it.priority.ordinal }
        }
    }

    fun unregisterFeature(instance: Any) {
        for ((_, list) in handlers) {
            list.removeIf { it.owner == instance }
        }

        handlers.entries.removeIf { (_, list) -> list.isEmpty() }
    }

    fun post(event: Event<*>) {
        val eventClass = event::class
        val list = handlers[eventClass] ?: return

        var currentPriority: EventPriority? = null
        var cancelledAtStart = event.isCancelled

        for (handler in list) {
            if (handler.priority != currentPriority) {
                currentPriority = handler.priority
                cancelledAtStart = event.isCancelled
            }

            if (cancelledAtStart && !handler.receiveCancelled) continue

            try {
                handler.handle.invoke(event)
            } catch (e: Throwable) {
                Logger.error("Error invoking event handler in class ${handler.owner.javaClass.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun existHandlers(eventClass: KClass<out Event<*>>): Boolean {
        return handlers[eventClass]?.isNotEmpty() == true
    }

    fun existHandlers(javaEventClass: Class<out Event<*>>): Boolean {
        return existHandlers(javaEventClass.kotlin)
    }

    fun getRegisteredEventClasses(): Set<KClass<out Event<*>>> {
        return handlers.keys
    }
}