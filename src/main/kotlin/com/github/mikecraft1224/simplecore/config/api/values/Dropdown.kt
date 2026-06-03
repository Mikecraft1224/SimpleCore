package com.github.mikecraft1224.simplecore.config.api.values

import kotlin.reflect.KProperty0

/**
 * A config field value type representing a single selection from a list of options.
 *
 * Assign a [Dropdown] to a config field - the config processor infers the entry type
 * automatically from the field type.
 *
 * ```kotlin
 * // Static options:
 * @Entry("Quality")
 * var quality = Dropdown("Low", "Medium", "High")
 *
 * // Dynamic options from a live property:
 * @Entry("Priority")
 * var priority = Dropdown(PriorityData::options)
 * ```
 *
 * The selected index is serialized as an [Int] in the config file.
 *
 * @param options Provider called on every render frame. Use `{ list }` for static options.
 * @param index   The currently selected index (default 0).
 */
class Dropdown(
    val options: () -> List<String>,
    var index: Int = 0,
) {
    /** Static options from `vararg` strings. */
    constructor(vararg options: String) : this({ options.toList() })

    /** Dynamic options read from a property on each render frame. */
    constructor(property: KProperty0<List<String>>) : this({ property.get() })

    /** The currently selected option label, or `null` if the list is empty. */
    val selected: String? get() = options().getOrNull(index)
}
