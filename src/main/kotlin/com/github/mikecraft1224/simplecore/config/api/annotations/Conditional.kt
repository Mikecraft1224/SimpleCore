package com.github.mikecraft1224.simplecore.config.api.annotations

import kotlin.reflect.KClass

/**
 * Controls visibility of a config field or category at runtime via a [VisibilityCondition].
 *
 * The condition is re-evaluated every render frame so visibility changes are reflected
 * immediately without reopening the screen.
 *
 * ```kotlin
 * object BetaEnabled : VisibilityCondition {
 *     override fun shouldShow() = MyMod.betaEnabled
 * }
 *
 * @Conditional(BetaEnabled::class)
 * @Category("Beta Features")
 * var beta = BetaSettings()
 * ```
 *
 * To bind visibility to another config field, use the runtime API after processing:
 * ```kotlin
 * model.bindVisible(config::threshold, config::enabled)
 * model.bindCategoryWhen(config::advanced, config::advancedEnabled)
 * ```
 *
 * @param condition A [VisibilityCondition] class; its instance is created once at process-time
 *   and [VisibilityCondition.shouldShow] is called every render frame.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Conditional(
    val condition: KClass<out VisibilityCondition>,
)
