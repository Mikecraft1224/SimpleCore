package com.github.mikecraft1224.bus.api

/**
 * Represents a cancellable event in the event bus system.
 */
@Suppress("UNUSED")
abstract class Event<T: Event<T>> {
    var isCancelled: Boolean = false

    fun cancel() {
        isCancelled = true
    }
}