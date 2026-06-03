@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.config.api.values

import kotlin.reflect.KProperty0

/**
 * A config field value type for zero-or-more selection from a list of options.
 *
 * Assign a [MultiSelect] to a config field - the config processor infers the entry type
 * automatically from the field type.
 *
 * ```kotlin
 * // Static options with initial selection:
 * @Entry("Active modes")
 * var activeModes = MultiSelect("Normal", "Fast", "Stealth")
 *
 * // Dynamic options from a live property:
 * @Entry("Active features")
 * var activeFeatures = MultiSelect(FeatureFlags::availableFeatures)
 * ```
 *
 * Selected option labels are stored as a [MutableList<String>] and serialized as a JSON array.
 *
 * @param options   Provider called on every render frame.
 * @param selected  Initially selected labels. Mutated in-place by the GUI.
 */
class MultiSelect(
    val options: () -> List<String>,
    val selected: MutableList<String> = mutableListOf(),
) {
    /** Static options from `vararg` strings. */
    constructor(vararg options: String) : this({ options.toList() })

    /** Dynamic options read from a property on each render frame. */
    constructor(property: KProperty0<List<String>>) : this({ property.get() })

    fun isSelected(option: String): Boolean = option in selected

    fun toggle(option: String) {
        if (option in selected) selected.remove(option) else selected.add(option)
    }
}
