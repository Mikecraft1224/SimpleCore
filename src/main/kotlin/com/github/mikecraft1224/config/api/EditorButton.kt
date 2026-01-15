package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a button in the config GUI.
 * When clicked, the [Runnable] assigned to the field will be executed.
 *
 * This may only be used on fields of type [Runnable].
 *
 * @param buttonText The text to display on the button.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorButton(
    val buttonText: String = "Click",
)