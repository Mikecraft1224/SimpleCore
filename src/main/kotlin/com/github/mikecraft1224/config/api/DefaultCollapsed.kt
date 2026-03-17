package com.github.mikecraft1224.config.api

/**
 * Marks a [@Collapsible][Collapsible] field as collapsed by default in the config GUI.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class DefaultCollapsed
