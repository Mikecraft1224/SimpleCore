package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be collapsible when inside a category in the config GUI.
 *
 * This may only be used on non-primitive custom objects.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Collapsible()
