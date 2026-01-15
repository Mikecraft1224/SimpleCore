package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a text input in the config GUI.
 *
 * This may only be used on fields of type [String].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorText()
