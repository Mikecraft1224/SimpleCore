package com.github.mikecraft1224.bus.api

/**
 * Base class for all events dispatched through the event bus.
 *
 * Events that support cancellation should extend [CancellableEvent] instead.
 * Events that are purely informational and cannot be cancelled should extend this class directly.
 */
abstract class Event

/**
 * An [Event] that supports cancellation.
 *
 * Handlers that wish to suppress further processing should call [cancel].
 * Other handlers can check [isCancelled] and opt in to receiving cancelled events
 * by setting [Subscribe.receiveCancelled] to `true`.
 */
abstract class CancellableEvent : Event() {
    var isCancelled: Boolean = false
        private set

    /** Marks this event as cancelled. Idempotent. */
    fun cancel() {
        isCancelled = true
    }
}
