package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Marks a [String] config field as a read-only info row (no editing widget).
 *
 * The field value is displayed as static text.
 *
 * ```kotlin
 * @Entry("Build")
 * @Info
 * var buildInfo = "MyMod ${BuildConfig.MOD_VERSION}"
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Info
