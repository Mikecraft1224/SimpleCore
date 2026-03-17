package com.github.mikecraft1224.config.api

/**
 * Optional annotation on a config class to provide display metadata.
 *
 * The [title] is shown as the header of the config screen instead of the generic "Config".
 * If [subtitle] is non-empty, a second line of smaller text is shown beneath the title.
 *
 * ```kotlin
 * @Config(title = "My Mod", subtitle = "Settings")
 * class MyConfig { ... }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Config(
    val title: String = "",
    val subtitle: String = "",
)
