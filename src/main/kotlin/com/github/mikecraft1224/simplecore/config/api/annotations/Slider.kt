package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Marks a numeric config field ([Int], [Float], or [Double]) as a slider widget.
 *
 * The field type is inferred automatically from the field declaration.
 *
 * ```kotlin
 * @Entry("Speed")
 * @Slider(0.5, 5.0, 0.5)
 * var speed = 1.0
 * ```
 *
 * When [step] is `0.0` (default) a sensible default step of `(max - min) / 100` is used.
 *
 * @param min  Minimum slider value.
 * @param max  Maximum slider value.
 * @param step Snap interval. `0.0` = auto.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Slider(
    val min: Double,
    val max: Double,
    val step: Double = 0.0,
)
