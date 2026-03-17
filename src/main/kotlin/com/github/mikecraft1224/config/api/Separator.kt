package com.github.mikecraft1224.config.api

/**
 * Draws a horizontal divider line above the annotated field in the config GUI.
 * Has no effect on serialization.
 *
 * @param label Optional text label centered on the divider line. Empty string = plain line.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Separator(
    val label: String = "",
)
