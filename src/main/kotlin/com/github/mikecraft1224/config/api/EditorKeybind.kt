package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a keybind in the config GUI.
 *
 * This may only be used on fields of type [Int] representing key codes.
 *
 * @param defaultKey The default key code for the keybind.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorKeybind(
    val defaultKey: Int
)
