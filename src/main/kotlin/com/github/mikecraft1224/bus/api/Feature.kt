package com.github.mikecraft1224.bus.api

/**
 * Annotates a class to be registered as a feature in the event bus.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Feature()
