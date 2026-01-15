package com.github.mikecraft1224.config.api

/**
 * Annotates a numeric field to be represented as a slider in the config GUI.
 *
 * This may only be used on fields of type [Int], [Float], or [Double].
 *
 * @param min The minimum value of the slider (inclusive).
 * @param max The maximum value of the slider (inclusive).
 * @param step The step value of the slider.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorSlider(
    val min: Double = 0.0,
    val max: Double = 100.0,
    val step: Double = 1.0,
)
