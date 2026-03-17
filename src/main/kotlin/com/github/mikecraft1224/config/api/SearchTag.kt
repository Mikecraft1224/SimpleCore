package com.github.mikecraft1224.config.api

/**
 * Optional hidden search aliases for a config entry.
 *
 * When the user types in the config search box, entries are matched against their display
 * name AND all [aliases] declared here. Aliases are never shown in the UI.
 *
 * Example:
 * ```kotlin
 * @Entry("Accent color", "Primary highlight color")
 * @SearchTag("highlight", "theme", "hue")
 * @EditorColor
 * var accentColor = Color(0x5865F2)
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SearchTag(vararg val aliases: String)
