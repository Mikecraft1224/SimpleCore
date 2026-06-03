package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Annotates a field to be represented as an entry in the config GUI.
 *
 * The widget type is inferred automatically from the field type - no separate `@Editor*`
 * annotation is needed in most cases. Numeric fields that should render as sliders require
 * a `@Slider(min, max)` annotation; read-only string fields require `@Info`.
 *
 * To control entry visibility, wrap the field value in [com.github.mikecraft1224.simplecore.config.api.values.Visible]:
 * ```kotlin
 * @Entry var enabled = true
 *
 * @Entry("Experimental threshold")
 * @Slider(0.0, 1.0, 0.05)
 * var experimentalThreshold = Visible(this::enabled, 0.5)
 * ```
 * For complex conditions not expressible as a single property, combine with [@Conditional][Conditional]:
 * ```kotlin
 * @Conditional(BetaEnabled::class)
 * @Entry var speed = 1.0
 * ```
 *
 * @param name Display name. Empty (default) auto-derives from the field name via camelCase -> Title Case.
 * @param description Tooltip description shown on hover.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Entry(
    val name: String = "",
    val description: String = "",
)
