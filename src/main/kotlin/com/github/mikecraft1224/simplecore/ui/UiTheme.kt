package com.github.mikecraft1224.simplecore.ui

/**
 * Shared color and font constants for the SimpleCore UI framework.
 *
 * Defaults are Catppuccin Mocha (matching the config screen palette).
 * Override individual properties to apply a partial theme.
 *
 * ```kotlin
 * val customTheme = UiTheme(accentColor = 0xFFA6E3A1.toInt())
 * val screen = MyScreen(theme = customTheme)
 * ```
 */
data class UiTheme(
    // Backgrounds
    /** Darkest background - used for panels and overlays. */
    val mantle:   Int = 0xFF181825.toInt(),
    /** Main background color. */
    val base:     Int = 0xFF1E1E2E.toInt(),
    /** Slightly lighter surface, used for inactive controls. */
    val surface0: Int = 0xFF313244.toInt(),
    /** Hover/focus surface. */
    val surface1: Int = 0xFF45475A.toInt(),

    // Text
    /** Primary text color. */
    val text:     Int = 0xFFCDD6F4.toInt(),
    /** Secondary/muted text. */
    val subtext:  Int = 0xFFBAC2DE.toInt(),
    /** Overlay/placeholder text. */
    val overlay0: Int = 0xFF6C7086.toInt(),

    // Accents
    /** Accent/highlight color - used for selected items, active toggles, slider fill. */
    val accent:   Int = 0xFF89B4FA.toInt(),
    /** Success/on-state color - used for enabled toggles. */
    val green:    Int = 0xFF40A02B.toInt(),
    /** Destructive/error color. */
    val red:      Int = 0xFFF38BA8.toInt(),

    // Dividers
    /** Border/divider line color. */
    val divider:  Int = 0xFF45475A.toInt(),
) {
    companion object {
        /** Default Catppuccin Mocha theme instance. */
        val DEFAULT = UiTheme()
    }
}

/**
 * Global current theme used as the default by all widget constructors.
 *
 * Set this before constructing widgets to avoid passing a theme instance to every constructor.
 * Use [withTheme] to scope theme changes to a block.
 *
 * ```kotlin
 * currentTheme = UiTheme(accent = 0xFFA6E3A1.toInt())
 * val label = Label("Hello")  // picks up the new currentTheme automatically
 * ```
 */
var currentTheme: UiTheme = UiTheme.DEFAULT

/**
 * Sets [currentTheme] to [theme] for the duration of [block], then restores the previous value.
 *
 * Wrap your [UiScreen.buildRoot] body in this call to give all constructed widgets the same theme
 * without passing it to every constructor.
 *
 * ```kotlin
 * override fun buildRoot(): Panel {
 *     return withTheme(myTheme) {
 *         column(4) {
 *             +Label("Title").height(20)        // receives myTheme implicitly
 *             +Button("OK", onClick = { ... })   // receives myTheme implicitly
 *         }
 *     }
 * }
 * ```
 *
 * @param theme the theme to activate for the duration of [block]
 * @param block the widget-construction block
 * @return the value returned by [block]
 */
inline fun <T> withTheme(theme: UiTheme, block: () -> T): T {
    val prev = currentTheme
    currentTheme = theme
    return try {
        block()
    } finally {
        currentTheme = prev
    }
}
