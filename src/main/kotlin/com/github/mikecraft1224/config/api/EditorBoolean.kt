package com.github.mikecraft1224.config.api

/**
 * Annotates a boolean field to be represented as a switch in the config GUI.
 *
 * This may only be used on [Boolean] fields.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorBoolean()
