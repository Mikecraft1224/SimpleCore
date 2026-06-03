package com.github.mikecraft1224.simplecore.config.api.values

/**
 * A config field value type representing a clickable button.
 *
 * Assign a [Button] to a config field - the config processor infers the entry type
 * automatically from the field type.
 *
 * ```kotlin
 * @Entry("Reset")
 * var resetAction = Button("Reset") {
 *     speed = 1.0
 *     mode = TestMode.NORMAL
 * }
 * ```
 *
 * [Button] fields are not serialized (buttons carry no state).
 *
 * @param label  Text shown on the button.
 * @param action Invoked when the user clicks the button.
 */
class Button(val label: String, val action: () -> Unit)
