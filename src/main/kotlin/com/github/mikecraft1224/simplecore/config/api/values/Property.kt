@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.config.api.values

/**
 * An observable config field wrapper. When the stored [value] changes, all registered
 * listeners are notified with the old and new values.
 *
 * Use this instead of a bare field when other code needs to react to config changes
 * without polling on every tick.
 *
 * ```kotlin
 * @Entry("Speed multiplier")
 * @Slider(0.5, 3.0, 0.1)
 * var speed = Property(1.0)
 *
 * // Somewhere in init:
 * config.speed.onChange { _, new -> motionModule.setSpeedMultiplier(new) }
 * ```
 *
 * The config processor infers the entry widget type from [value]'s type, exactly as it
 * would for a bare field. The serializer stores the inner value directly.
 *
 * @param initialValue The starting value.
 */
class Property<T>(initialValue: T) {
    var value: T = initialValue
        set(new) {
            val old = field
            field = new
            if (old != new) listeners.forEach { it(old, new) }
        }

    private val listeners = mutableListOf<(T, T) -> Unit>()

    /** Registers a listener called whenever [value] changes. Returns `this` for chaining. */
    fun onChange(listener: (old: T, new: T) -> Unit): Property<T> {
        listeners.add(listener)
        return this
    }

    companion object {
        fun <T> of(value: T): Property<T> = Property(value)
    }
}
