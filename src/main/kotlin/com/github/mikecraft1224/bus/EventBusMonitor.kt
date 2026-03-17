package com.github.mikecraft1224.bus

/**
 * A global ring-buffer monitor that records recent event dispatches from any [EventBus] running
 * with [EventBus.debugMode] enabled.
 *
 * Thread safety: [record] and [getRecent] are not synchronised - this monitor is intentionally
 * lightweight. Minor data races in the buffer boundary are acceptable for a debugging tool.
 *
 * Usage:
 * ```kotlin
 * bus.debugMode = true
 * // ... trigger some events ...
 * EventBusMonitor.getRecent().forEach { println(it) }
 * ```
 */
@Suppress("UNUSED")
object EventBusMonitor {
    /**
     * A record of a single [com.github.mikecraft1224.bus.api.Event] post.
     *
     * @property eventClass Fully-qualified (or simple) class name of the posted event.
     * @property wasCancelled Whether the event was in a cancelled state after dispatch completed.
     *   Always `false` for non-cancellable events.
     * @property handlerCount The number of handlers that were actually invoked (filters and
     *   cancellation skips are not counted).
     * @property timestampMs Wall-clock time in milliseconds when the post completed.
     */
    data class PostRecord(
        val eventClass: String,
        val wasCancelled: Boolean,
        val handlerCount: Int,
        val timestampMs: Long
    )

    private var capacity: Int = 256
    private val buffer = ArrayDeque<PostRecord>(capacity)

    /**
     * Sets the maximum number of records retained. Older records are evicted when the buffer
     * is full. Changes take effect on the next [record] call.
     *
     * @param cap Must be greater than zero.
     */
    fun setCapacity(cap: Int) {
        require(cap > 0) { "Capacity must be > 0, got $cap" }
        capacity = cap
        while (buffer.size > capacity) buffer.removeFirst()
    }

    /**
     * Adds a [record] to the buffer, evicting the oldest entry if the buffer is at capacity.
     * Called automatically by [EventBus.post] when [EventBus.debugMode] is `true`.
     */
    fun record(record: PostRecord) {
        if (buffer.size >= capacity) buffer.removeFirst()
        buffer.addLast(record)
    }

    /**
     * @return A snapshot of all currently buffered records, oldest first.
     */
    fun getRecent(): List<PostRecord> = buffer.toList()

    /** Clears all buffered records. */
    fun clear() = buffer.clear()
}