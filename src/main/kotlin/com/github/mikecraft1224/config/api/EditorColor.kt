package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a color picker in the config GUI.
 *
 * This may only be used on fields of type [java.awt.Color].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorColor()
