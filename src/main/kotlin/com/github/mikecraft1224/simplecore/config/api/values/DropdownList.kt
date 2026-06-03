@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.config.api.values

import kotlin.reflect.KProperty0

/**
 * A config field value type representing a mutable list where each element is a dropdown selection.
 *
 * Assign a [DropdownList] to a config field - the config processor infers the entry type
 * automatically from the field type.
 *
 * ```kotlin
 * // Static options:
 * @Entry("Priority list")
 * var priorities = DropdownList("Low", "Normal", "High")
 *
 * // Dynamic options from a live property:
 * @Entry("Server list")
 * var servers = DropdownList(ServerManager::availableServers)
 * ```
 *
 * Each element in [indices] is an Int storing the selected index. Serialized as a JSON array
 * of integers. On load, [indices] is updated in-place.
 *
 * @param options Provider called on every render frame.
 * @param indices Currently selected indices. Mutated in-place by the GUI.
 */
class DropdownList(
    val options: () -> List<String>,
    val indices: MutableList<Int> = mutableListOf(),
) {
    /** Static options from `vararg` strings. */
    constructor(vararg options: String) : this({ options.toList() })

    /** Dynamic options read from a property on each render frame. */
    constructor(property: KProperty0<List<String>>) : this({ property.get() })

    /** Returns the selected label at [position], or `null` if out of range. */
    fun labelAt(position: Int): String? = options().getOrNull(indices.getOrNull(position) ?: -1)
}
