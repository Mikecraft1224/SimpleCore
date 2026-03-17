package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a dropdown in the config GUI.
 *
 * **Enum field** (no `values` needed - uses each enum's `toString()`):
 * ```kotlin
 * @Entry("Mode") @EditorDropdown
 * var mode = Mode.NORMAL
 * ```
 *
 * **Int field** with explicit labels:
 * ```kotlin
 * @Entry("Quality") @EditorDropdown(values = ["Low", "Medium", "High"])
 * var quality = 1   // stores the selected index
 * ```
 *
 * **String field** with explicit labels:
 * ```kotlin
 * @Entry("Theme") @EditorDropdown(values = ["Dark", "Light"])
 * var theme = "Dark"   // stores the selected label string
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorDropdown(
    /**
     * Explicit option labels. Required when the backing field is [Int] or [String].
     * Leave empty for [Enum] fields - their `toString()` values are used automatically.
     */
    val values: Array<String> = [],
)
