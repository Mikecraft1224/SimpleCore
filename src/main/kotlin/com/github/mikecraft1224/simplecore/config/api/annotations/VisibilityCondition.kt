package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Evaluated every render frame to decide whether a field annotated with
 * [@Conditional][Conditional] should currently be visible.
 *
 * Works uniformly for both entry fields and category fields. The instance is created once at
 * process-time via reflection; [shouldShow] is invoked on every render frame.
 *
 * Implement with a no-arg constructor or as a Kotlin `object`:
 *
 * ```kotlin
 * object BetaEnabled : VisibilityCondition {
 *     override fun shouldShow(): Boolean = MyMod.betaEnabled
 * }
 *
 * @Conditional(BetaEnabled::class)
 * @Entry("Experimental threshold")
 * @Slider(0.0, 1.0, 0.05)
 * var experimentalThreshold = 0.5
 * ```
 *
 * For field-based visibility (linking to another config property), use the runtime API instead:
 * `model.bindVisible(config::threshold, config::enabled)`
 */
fun interface VisibilityCondition {
    fun shouldShow(): Boolean
}
