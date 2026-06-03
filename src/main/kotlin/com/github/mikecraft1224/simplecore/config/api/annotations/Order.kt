package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Sets the display order of a config field within its category.
 *
 * Lower values appear first. Fields without this annotation default to 0 and appear in
 * declaration order relative to other unordered fields (stable sort).
 *
 * ```kotlin
 * @Order(-1)
 * @Entry("Important setting")
 * var importantSetting = true  // appears before unordered fields
 *
 * @Order(10)
 * @Entry("Advanced setting")
 * var advancedSetting = false  // appears after unordered fields
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Order(val value: Int)
