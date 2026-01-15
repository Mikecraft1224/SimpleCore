package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a dropdown in the config GUI.
 *
 * This may only be used on fields of type [Enum] with a defined [toString] method.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorDropdown()
