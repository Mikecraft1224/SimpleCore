package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Annotates a field inside a config class as a category page in the GUI.
 * When used inside another [Category] it becomes a sub-category.
 * Sub-categories cannot be nested further.
 *
 * To control category visibility, wrap the category value in [com.github.mikecraft1224.simplecore.config.api.values.Visible]:
 * ```kotlin
 * @Entry var showAdvanced = false
 *
 * @Category("Advanced")
 * var advanced = Visible(this::showAdvanced, AdvancedSettings())
 * ```
 * For cross-class visibility using an external property reference:
 * ```kotlin
 * object Flags { var expertMode = false }
 *
 * @Category("Advanced")
 * var advanced = Visible(Flags::expertMode, AdvancedSettings())
 * ```
 * For complex conditions not expressible as a single property, combine with [@Conditional][Conditional]:
 * ```kotlin
 * @Conditional(BetaEnabled::class)
 * @Category("Beta Features")
 * var beta = BetaSettings()
 * ```
 *
 * @param name The display name of the category.
 * @param description Short description shown at the top of the category page.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Category(
    val name: String,
    val description: String = "",
)
