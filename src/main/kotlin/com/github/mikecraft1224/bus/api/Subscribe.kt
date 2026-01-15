package com.github.mikecraft1224.bus.api

/**
 * Annotates a method to be registered as an event listener in the event bus.
 *
 * This may only be used on methods with a single parameter that is a subclass of [Event].
 *
 * @param priority The priority of the event listener. Listeners with higher priority will be called first.
 * @param receiveCancelled Whether this listener should receive events that have been cancelled.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Subscribe(
    val priority: EventPriority = EventPriority.NORMAL,
    val receiveCancelled: Boolean = false,
)
