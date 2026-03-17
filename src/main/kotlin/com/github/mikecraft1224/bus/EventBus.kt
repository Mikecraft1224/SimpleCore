package com.github.mikecraft1224.bus

import com.github.mikecraft1224.Logger
import com.github.mikecraft1224.bus.api.CancellableEvent
import com.github.mikecraft1224.bus.api.DefaultEventExceptionHandler
import com.github.mikecraft1224.bus.api.Event
import com.github.mikecraft1224.bus.api.EventExceptionHandler
import com.github.mikecraft1224.bus.api.EventFilter
import com.github.mikecraft1224.bus.api.EventPriority
import com.github.mikecraft1224.bus.api.NoFilter
import com.github.mikecraft1224.bus.api.RegistrationHandle
import com.github.mikecraft1224.bus.api.Subscribe
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * An event bus that dispatches [Event] instances to methods annotated with [Subscribe].
 *
 * Features:
 * - Priority-ordered dispatch ([EventPriority])
 * - Cancellation support for [CancellableEvent] subclasses
 * - Polymorphic dispatch via [Subscribe.polymorphic]
 * - Per-handler invocation filtering via [Subscribe.filter]
 * - Pluggable exception handling via [exceptionHandler]
 * - Debug mode with per-invocation logging and [EventBusMonitor] recording
 * - Selective handler removal via [RegistrationHandle]
 *
 * Notes for EventBus implementors:
 * Load Fabric-side hooks lazily - only when at least one handler is registered for a given
 * event type. Check [existHandlers] or [getRegisteredEventClasses] before registering hooks.
 * See [FeatureAutoLoader] and [com.github.mikecraft1224.bus.events.ClientTickEvent] for examples.
 */
@Suppress("UNUSED")
class EventBus(
    val exceptionHandler: EventExceptionHandler = DefaultEventExceptionHandler()
) {
    /**
     * When `true`, every handler invocation is logged at DEBUG level and all posts are
     * recorded in [EventBusMonitor]. Disable in production.
     */
    var debugMode: Boolean = false

    // -----------------------------------------------------------------------------------------
    // Internal handler record
    // -----------------------------------------------------------------------------------------

    private data class Handler(
        val owner: Any,
        val handle: MethodHandle,
        val methodName: String,
        val priority: EventPriority,
        val receiveCancelled: Boolean,
        val polymorphic: Boolean,
        val filter: EventFilter<Event>?   // null == NoFilter sentinel, no test call on hot path
    )

    // -----------------------------------------------------------------------------------------
    // Handler summary - safe read-only snapshot for introspection
    // -----------------------------------------------------------------------------------------

    /**
     * A read-only snapshot of a single registered handler, safe to expose to external callers.
     *
     * @property ownerClass Fully-qualified name of the class that owns the handler method.
     * @property methodName Name of the annotated method.
     * @property priority Dispatch priority of this handler.
     * @property receiveCancelled Whether this handler receives already-cancelled events.
     * @property polymorphic Whether this handler was registered with polymorphic dispatch.
     * @property hasFilter Whether a non-trivial [EventFilter] is active for this handler.
     */
    data class HandlerSummary(
        val ownerClass: String,
        val methodName: String,
        val priority: EventPriority,
        val receiveCancelled: Boolean,
        val polymorphic: Boolean,
        val hasFilter: Boolean
    )

    // -----------------------------------------------------------------------------------------
    // Internal RegistrationHandle implementation
    // -----------------------------------------------------------------------------------------

    private inner class RegistrationHandleImpl(
        private val list: CopyOnWriteArrayList<Handler>,
        private val handler: Handler
    ) : RegistrationHandle {
        @Volatile
        override var isRegistered: Boolean = true
            private set

        override fun unregister() {
            if (!isRegistered) return
            list.removeIf { it === handler }
            isRegistered = false
        }
    }

    // -----------------------------------------------------------------------------------------
    // Handler storage
    // -----------------------------------------------------------------------------------------

    private val handlers: ConcurrentHashMap<KClass<out Event>, CopyOnWriteArrayList<Handler>> =
        ConcurrentHashMap()
    private val lookup = MethodHandles.lookup()

    private fun handlersFor(eventClass: KClass<out Event>): CopyOnWriteArrayList<Handler> =
        handlers.computeIfAbsent(eventClass) { CopyOnWriteArrayList() }

    // -----------------------------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------------------------

    /**
     * Scans [instance] for methods annotated with [Subscribe] and registers them as handlers.
     * Equivalent to [registerFeatureWithHandles] but discards the returned handles.
     *
     * @param instance The feature object to scan. Must be fully initialized.
     */
    fun registerFeature(instance: Any) {
        registerFeatureWithHandles(instance)
    }

    /**
     * Scans [instance] for methods annotated with [Subscribe], registers them as handlers, and
     * returns one [RegistrationHandle] per successfully registered method.
     *
     * Use the returned handles to selectively unregister individual handlers without tearing
     * down the entire feature. Handles are returned in the order the methods were discovered
     * by reflection, which is JVM-dependent and should not be relied upon.
     *
     * @param instance The feature object to scan. Must be fully initialized.
     * @return A list of handles, one per registered handler. Empty if no [Subscribe] methods
     *   were found or all failed validation.
     */
    fun registerFeatureWithHandles(instance: Any): List<RegistrationHandle> {
        val clazz = instance.javaClass
        val handles = mutableListOf<RegistrationHandle>()

        for (m in clazz.declaredMethods) {
            val ann = m.getAnnotation(Subscribe::class.java) ?: continue
            if (m.parameterCount != 1 || !Event::class.java.isAssignableFrom(m.parameterTypes[0])) {
                Logger.warn(
                    "Method ${m.name} in ${clazz.name} is annotated with @Subscribe but does " +
                    "not have a single parameter of type Event - skipping."
                )
                continue
            }

            @Suppress("UNCHECKED_CAST")
            val param = (m.parameterTypes[0] as Class<out Event>).kotlin
            m.isAccessible = true

            val filter: EventFilter<Event>? = if (ann.filter == NoFilter::class) {
                null
            } else {
                @Suppress("UNCHECKED_CAST")
                instantiateFilter(ann.filter as KClass<out EventFilter<Event>>)
            }

            val methodHandle = lookup.unreflect(m).bindTo(instance)
            val handler = Handler(
                owner = instance,
                handle = methodHandle,
                methodName = m.name,
                priority = ann.priority,
                receiveCancelled = ann.receiveCancelled,
                polymorphic = ann.polymorphic,
                filter = filter
            )

            // Register under the exact declared event type
            val exactList = handlersFor(param)
            exactList.add(handler)
            exactList.sortByDescending { it.priority.ordinal }
            handles.add(RegistrationHandleImpl(exactList, handler))

            // Polymorphic: also insert into all known ancestor event types
            if (ann.polymorphic) {
                for (supertype in collectEventSupertypes(param)) {
                    val supertypeList = handlersFor(supertype)
                    supertypeList.add(handler)
                    supertypeList.sortByDescending { it.priority.ordinal }
                    // We intentionally share the same handler object; the handle above already
                    // covers unregistration from the exact list. For supertypes we add
                    // additional handles so the caller can also remove those slots.
                    handles.add(RegistrationHandleImpl(supertypeList, handler))
                }
            }
        }

        return handles
    }

    /**
     * Removes all handlers belonging to [instance] from every event list on this bus.
     * For selective removal, prefer [registerFeatureWithHandles] and use the returned handles.
     *
     * @param instance The feature instance to fully deregister.
     */
    fun unregisterFeature(instance: Any) {
        for ((_, list) in handlers) {
            list.removeIf { it.owner == instance }
        }
        handlers.entries.removeIf { (_, list) -> list.isEmpty() }
    }

    // -----------------------------------------------------------------------------------------
    // Dispatch
    // -----------------------------------------------------------------------------------------

    /**
     * Dispatches [event] to all registered handlers for `event::class`.
     *
     * Handler ordering: descending [EventPriority], then registration order within a priority.
     * Cancellation state is snapshotted at each priority boundary (see [CancellableEvent]).
     * When [debugMode] is `true`, each invocation is logged and recorded in [EventBusMonitor].
     *
     * @param event The event instance to dispatch.
     */
    fun post(event: Event) {
        val eventClass = event::class
        val list = handlers[eventClass] ?: return

        val cancellable = event as? CancellableEvent
        var currentPriority: EventPriority? = null
        var cancelledAtStart = cancellable?.isCancelled ?: false
        var handlerCount = 0

        for (handler in list) {
            if (cancellable != null && handler.priority != currentPriority) {
                currentPriority = handler.priority
                cancelledAtStart = cancellable.isCancelled
            }

            if (cancelledAtStart && !handler.receiveCancelled) continue

            if (handler.filter != null && !handler.filter.test(event)) continue

            if (debugMode) {
                Logger.debug(
                    "[EventBus] dispatching ${eventClass.simpleName} → " +
                    "${handler.owner.javaClass.simpleName}::${handler.methodName} " +
                    "(priority=${handler.priority}, cancelled=${cancellable?.isCancelled ?: false})"
                )
            }

            try {
                handler.handle.invoke(event)
                handlerCount++
            } catch (e: Throwable) {
                exceptionHandler.onException(handler.owner, event, e)
            }
        }

        if (debugMode) {
            EventBusMonitor.record(
                EventBusMonitor.PostRecord(
                    eventClass = eventClass.qualifiedName ?: eventClass.simpleName ?: "Unknown",
                    wasCancelled = cancellable?.isCancelled ?: false,
                    handlerCount = handlerCount,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // Introspection
    // -----------------------------------------------------------------------------------------

    /**
     * Returns a snapshot of handler metadata for [eventClass] in dispatch order.
     * No internal state or live references are exposed.
     *
     * @param eventClass The event type to inspect.
     * @return Ordered list of [HandlerSummary] records, or empty if no handlers are registered.
     */
    fun getHandlerSummary(eventClass: KClass<out Event>): List<HandlerSummary> {
        val list = handlers[eventClass] ?: return emptyList()
        return list.map { h ->
            HandlerSummary(
                ownerClass = h.owner.javaClass.name,
                methodName = h.methodName,
                priority = h.priority,
                receiveCancelled = h.receiveCancelled,
                polymorphic = h.polymorphic,
                hasFilter = h.filter != null
            )
        }
    }

    /**
     * @return `true` if at least one handler is registered for [eventClass].
     */
    fun existHandlers(eventClass: KClass<out Event>): Boolean =
        handlers[eventClass]?.isNotEmpty() == true

    /**
     * Java-friendly overload of [existHandlers].
     */
    fun existHandlers(javaEventClass: Class<out Event>): Boolean =
        existHandlers(javaEventClass.kotlin)

    /**
     * @return The set of event types that currently have at least one handler registered.
     */
    fun getRegisteredEventClasses(): Set<KClass<out Event>> = handlers.keys

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    /**
     * Walks the Kotlin supertype graph from [eventClass] up to (but not including) [Event]
     * and [Any], returning only types that are themselves subclasses of [Event].
     */
    private fun collectEventSupertypes(eventClass: KClass<out Event>): List<KClass<out Event>> {
        val result = mutableListOf<KClass<out Event>>()
        val queue = ArrayDeque<KClass<*>>()
        queue.addAll(eventClass.superclasses)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == Event::class || current == Any::class) continue
            if (Event::class.java.isAssignableFrom(current.java)) {
                @Suppress("UNCHECKED_CAST")
                result.add(current as KClass<out Event>)
                queue.addAll(current.superclasses)
            }
        }
        return result
    }

    private fun <E : Event> instantiateFilter(filterClass: KClass<out EventFilter<E>>): EventFilter<E>? {
        // Try Kotlin object (INSTANCE field)
        runCatching {
            val field = filterClass.java.getDeclaredField("INSTANCE")
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val obj = field.get(null) as? EventFilter<E>
            if (obj != null) return obj
        }
        // Fall back to no-arg constructor
        return runCatching {
            val ctor = filterClass.java.getDeclaredConstructor()
            ctor.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            ctor.newInstance() as EventFilter<E>
        }.getOrElse { e ->
            Logger.warn(
                "Could not instantiate EventFilter ${filterClass.qualifiedName}. " +
                "Ensure it has a no-arg constructor or is a Kotlin object. Filter will be ignored. " +
                "Cause: ${e.message}"
            )
            null
        }
    }
}