package com.github.mikecraft1224.simplecore.config.api.values

import kotlin.reflect.KProperty0

/**
 * Wraps a config field value and ties it to a visibility condition expressed as a property reference.
 * When the property returns `false`, the entry or category is hidden from the config screen.
 *
 * Evaluated every render frame - visibility changes are live without reopening the screen.
 *
 * ### Entry example
 * ```kotlin
 * @Entry("Experimental threshold")
 * @Slider(0.0, 1.0, 0.05)
 * var experimentalThreshold = Visible(FeatureFlags::experimentalEnabled, 0.5)
 * ```
 *
 * ### Category example
 * ```kotlin
 * @Category("Advanced", "Fine-grained controls")
 * var advanced = Visible(this::advancedEnabled, AdvancedSettings())
 * ```
 *
 * Access the wrapped value via [value]:
 * ```kotlin
 * config.experimentalThreshold.value  // 0.5
 * ```
 *
 * @param condition Property reference whose Boolean value gates visibility.
 * @param value The wrapped config value. Serialized and deserialized as if it were the field directly.
 */
class Visible<T>(val condition: KProperty0<Boolean>, var value: T)
