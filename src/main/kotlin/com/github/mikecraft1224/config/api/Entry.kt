package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as an entry in the config GUI.
 *
 * @param name The name of the entry.
 * @param description The description of the entry.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class Entry(
    val name: String,
    val description: String = "",
)
