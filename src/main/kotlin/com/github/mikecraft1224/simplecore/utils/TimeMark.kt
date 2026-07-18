@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A point in time, backed by [System.currentTimeMillis]. Use for cooldowns, debouncing, and
 * "how long has it been since X" checks without hand-rolling a raw millis field everywhere:
 * ```kotlin
 * var lastUse = TimeMark.FAR_PAST
 *
 * fun tryUse() {
 *     if (lastUse.passedSince() < 2.seconds) return
 *     lastUse = TimeMark.now()
 *     // ... do the thing
 * }
 * ```
 */
@JvmInline
value class TimeMark(val millis: Long) {
    /** How much time has elapsed between this mark and now. Negative if this mark is in the future. */
    fun passedSince(): Duration = (System.currentTimeMillis() - millis).milliseconds

    /** `true` if this mark is at or before the current time. */
    fun isInPast(): Boolean = System.currentTimeMillis() >= millis

    /** `true` if this mark is after the current time. */
    fun isInFuture(): Boolean = !isInPast()

    operator fun plus(duration: Duration): TimeMark = TimeMark(millis + duration.inWholeMilliseconds)
    operator fun minus(duration: Duration): TimeMark = TimeMark(millis - duration.inWholeMilliseconds)

    /** Difference between two marks, positive when [other] is earlier than this one. */
    operator fun minus(other: TimeMark): Duration = (millis - other.millis).milliseconds

    companion object {
        /** A mark for the current instant. */
        fun now(): TimeMark = TimeMark(System.currentTimeMillis())

        /** A mark [duration] from now. */
        fun future(duration: Duration): TimeMark = now() + duration

        /** Always in the past - a safe initial value for cooldown fields so the first check passes. */
        val FAR_PAST = TimeMark(0L)
    }
}
