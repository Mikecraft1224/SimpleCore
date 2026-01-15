package com.github.mikecraft1224.config.api

/**
 * Annotates a field to be represented as a mutable list in the config GUI.
 * This means that the user can add and remove elements from the list.
 * For lists of primitive types, the default value can be specified.
 * For non-primitive types, the default values of the type will be used.
 *
 * This may only be used on fields of type [List] or [MutableList].
 *
 * @param defaultString The default value for lists of [String].
 * @param defaultInt The default value for lists of [Int].
 * @param defaultBoolean The default value for lists of [Boolean].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class EditorMutable(
    val defaultString: String = "",
    val defaultInt: Int = 0,
    val defaultBoolean: Boolean = false,
)
