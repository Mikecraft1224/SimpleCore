package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Optional annotation on a config class to provide display metadata.
 *
 * The [title] is shown as the header of the config screen instead of the generic "Config".
 * If [subtitle] is non-empty, a second line of smaller text is shown beneath the title.
 * [accentColor] overrides the default blue highlight color (0 = use default C_BLUE = 0xFF89B4FA).
 * [searchEnabled] controls whether the search bar is shown.
 * [defaultCategory] sets the category opened by default; empty string means the first category.
 *
 * ```kotlin
 * @Config(title = "My Mod", subtitle = "Settings", accentColor = 0xFFA6E3A1.toInt(), defaultCategory = "Advanced")
 * class MyConfig { ... }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Config(
    val title: String = "",
    val subtitle: String = "",
    /** Accent color in ARGB format. 0 means use the default blue (0xFF89B4FA). */
    val accentColor: Int = 0,
    /** Whether the search bar is shown on the config screen. Defaults to true. */
    val searchEnabled: Boolean = true,
    /** Name of the category to open by default. Empty string opens the first category. */
    val defaultCategory: String = "",
    /** Whether the search bar should be focused automatically when the config screen opens. */
    val autoFocusSearch: Boolean = false,
)
