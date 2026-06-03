package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Optional hidden search aliases for a config entry.
 *
 * When the user types in the config search box, entries are matched against their display
 * name AND all [aliases] declared here. Aliases are never shown in the UI.
 *
 * ```kotlin
 * @Entry("Accent color")
 * @SearchTag("highlight", "theme", "hue")
 * var accentColor = java.awt.Color(0x5865F2)
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SearchTag(vararg val aliases: String)
