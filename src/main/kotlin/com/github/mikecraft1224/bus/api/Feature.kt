package com.github.mikecraft1224.bus.api

import kotlin.reflect.KClass

/**
 * Annotates a class to be registered as a feature in the event bus.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Feature

/**
 * A predicate evaluated once during classpath scanning to decide whether a [Feature]-annotated
 * class should be registered with the event bus.
 *
 * Implementations must have a no-arg constructor (or be a Kotlin `object`). They are
 * instantiated once during scanning and must not depend on world state, player state, or any
 * other runtime context that is unavailable at mod initialisation time.
 *
 * Reference the implementing class via [@ConditionalFeature][ConditionalFeature] on a
 * [@Feature][Feature]-annotated class.
 *
 * Built-in implementations:
 * - [com.github.mikecraft1224.bus.api.conditions.ModPresentCondition]
 * - [com.github.mikecraft1224.bus.api.conditions.PhysicalClientCondition]
 */
fun interface FeatureCondition {
    /**
     * @return `true` if the feature should be registered, `false` to skip it entirely.
     */
    fun shouldLoad(): Boolean
}

/**
 * Guards a [Feature]-annotated class behind a [FeatureCondition].
 *
 * [FeatureAutoLoader][com.github.mikecraft1224.bus.FeatureAutoLoader] instantiates [condition]
 * once and calls [FeatureCondition.shouldLoad]. If it returns `false` the class is skipped.
 *
 * ```kotlin
 * @Feature
 * @ConditionalFeature(PhysicalClientCondition::class)
 * object MyClientOnlyFeature {
 *     @Subscribe
 *     fun onTick(event: ClientTickEvent) { ... }
 * }
 * ```
 *
 * @param condition A [FeatureCondition] class with a no-arg constructor (or a Kotlin object).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ConditionalFeature(val condition: KClass<out FeatureCondition>)
