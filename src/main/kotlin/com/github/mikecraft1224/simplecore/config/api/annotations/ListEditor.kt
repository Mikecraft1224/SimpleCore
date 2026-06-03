package com.github.mikecraft1224.simplecore.config.api.annotations

/**
 * Optional settings for list-type config entries ([MutableList] fields).
 *
 * ```kotlin
 * @Entry("Allowed players")
 * @ListEditor(requireNonEmpty = true)
 * var allowedPlayers: MutableList<String> = mutableListOf("default")
 * ```
 *
 * @param requireNonEmpty When true, the delete button is disabled when only one item remains.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class ListEditor(
    val requireNonEmpty: Boolean = false,
)
