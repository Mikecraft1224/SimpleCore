package com.github.mikecraft1224.bus.api

import kotlin.reflect.KClass

/**
 * Annotates a method to be registered as an event listener in the event bus.
 *
 * The annotated method must have exactly one parameter that is a subclass of [Event].
 *
 * @param priority The priority of this listener relative to others for the same event type.
 *   Higher priority listeners are called first.
 * @param receiveCancelled Whether this listener should be invoked even when the event has
 *   already been cancelled by a higher-priority handler. Only meaningful for [CancellableEvent].
 * @param polymorphic When `true`, this listener also receives events whose runtime type is a
 *   *subtype* of the declared parameter type. Handlers registered with `polymorphic = true`
 *   are inserted into the handler lists of all currently-known supertypes at registration time.
 *   Within a priority level, exact-type handlers fire before polymorphic ones.
 * @param filter An [EventFilter] class (must have a no-arg constructor) that is instantiated
 *   once at registration time. The filter's [EventFilter.test] is called before each handler
 *   invocation; returning `false` skips the invocation. Use [NoFilter] (the default) for no
 *   filtering.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Subscribe(
    val priority: EventPriority = EventPriority.NORMAL,
    val receiveCancelled: Boolean = false,
    val polymorphic: Boolean = false,
    val filter: KClass<out EventFilter<*>> = NoFilter::class,
)
