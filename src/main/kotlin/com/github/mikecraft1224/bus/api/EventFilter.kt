package com.github.mikecraft1224.bus.api

/**
 * A stateless predicate evaluated before a handler is invoked.
 *
 * Implement this interface with a no-arg constructor and reference the class in
 * [Subscribe.filter] to gate handler invocation without polluting the handler body.
 *
 * ```kotlin
 * class EndPhaseOnly : EventFilter<ClientTickEvent> {
 *     override fun test(event: ClientTickEvent) = event.phase == ClientTickEvent.Phase.END
 * }
 *
 * @Subscribe(filter = EndPhaseOnly::class)
 * fun onTick(event: ClientTickEvent) { ... }
 * ```
 *
 * Use [NoFilter] (the default) to opt out of filtering entirely.
 */
fun interface EventFilter<in E : Event> {
    /**
     * @return `true` if the handler should be invoked for [event], `false` to skip it.
     */
    fun test(event: E): Boolean
}

/**
 * Sentinel [EventFilter] that always passes. Used as the default value for [Subscribe.filter].
 *
 * When the bus sees this class as the filter, it skips the test call entirely on the hot path.
 */
object NoFilter : EventFilter<Event> {
    override fun test(event: Event): Boolean = true
}
