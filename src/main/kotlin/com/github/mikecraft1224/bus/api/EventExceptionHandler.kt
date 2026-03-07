package com.github.mikecraft1224.bus.api

import com.github.mikecraft1224.Logger

/**
 * Handles exceptions thrown by event handlers during dispatch.
 *
 * Supply a custom implementation to [EventBus][com.github.mikecraft1224.bus.EventBus] to
 * control error behaviour: log-and-continue (default), rethrow in tests, suppress after
 * repeated failures, etc.
 *
 * @see DefaultEventExceptionHandler
 * @see RethrowEventExceptionHandler
 */
fun interface EventExceptionHandler {
    /**
     * Called when [throwable] is thrown by a handler owned by [owner] while processing [event].
     *
     * @param owner The feature instance that owns the failing handler method.
     * @param event The event that was being dispatched when the exception occurred.
     * @param throwable The exception thrown by the handler.
     */
    fun onException(owner: Any, event: Event, throwable: Throwable)
}

/**
 * The default [EventExceptionHandler] used by [com.github.mikecraft1224.bus.EventBus].
 *
 * Logs the failure at ERROR level including the owner class, event type, and full stack trace,
 * then allows dispatch to continue for remaining handlers.
 */
class DefaultEventExceptionHandler : EventExceptionHandler {
    override fun onException(owner: Any, event: Event, throwable: Throwable) {
        Logger.error(
            "Exception in event handler [${owner.javaClass.name}] while dispatching [${event::class.simpleName}]: ${throwable.message}",
            throwable
        )
    }
}

/**
 * An [EventExceptionHandler] that wraps and rethrows any exception thrown by a handler.
 *
 * Intended for use in test environments where silent failure is unacceptable.
 * Do not use in production — this will crash the game on the first handler failure.
 */
class RethrowEventExceptionHandler : EventExceptionHandler {
    override fun onException(owner: Any, event: Event, throwable: Throwable) {
        throw RuntimeException(
            "Exception in event handler [${owner.javaClass.name}] while dispatching [${event::class.simpleName}]",
            throwable
        )
    }
}
