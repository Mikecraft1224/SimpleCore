package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be excluded from the config file and/or config GUI.
 *
 * @param config Whether to exclude this field from the config file. Default is false.
 * @param gui Whether to exclude this field from the config GUI. Default is true.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Excluded(
    val config: Boolean = false,
    val gui: Boolean = true,
)
