package com.github.mikecraft1224.simplecore.config.api.values

import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

/**
 * A transparent two-way reference to a property defined elsewhere.
 *
 * Assign a [Reference] to a config field to bind it to an external property.
 * The config system reads and writes through the reference - the config class holds no
 * data of its own for this field; the external property is the source of truth.
 *
 * ```kotlin
 * object PriorityData {
 *     var options: MutableList<String> = mutableListOf("Low", "Normal", "High")
 * }
 *
 * @Config(title = "My Mod")
 * class MyConfig {
 *     // GUI edits PriorityData.options directly - no duplication:
 *     @Entry
 *     var priorityOptions = Reference(PriorityData::options)
 *
 *     // Each slot picks from the same live options list:
 *     @Entry
 *     var priorityList = DropdownList(PriorityData::options)
 * }
 * ```
 *
 * **Serialization:** [Reference] fields are serialized bidirectionally. On save, [get] is called
 * and the result is written to the config file. On load, the stored value is passed to [set],
 * updating the external property directly. If the reference is read-only (created from a
 * [KProperty0]), [set] is a no-op and the value is not restored from the file.
 *
 * **Cross-class visibility:** declare a [Reference] field pointing at an external boolean, then
 * use [@Conditional][com.github.mikecraft1224.simplecore.config.api.annotations.Conditional] pointing at it -
 * the processor dereferences the Reference automatically:
 *
 * ```kotlin
 * object FeatureFlags { var enabled = false }
 *
 * class MyConfig {
 *     var featureEnabled = Reference(FeatureFlags::enabled)
 *
 *     @Conditional("featureEnabled")
 *     @Entry @Slider(0.0, 1.0, 0.1)
 *     var threshold = 0.5
 * }
 * ```
 *
 * @param T the type of the referenced property.
 */
class Reference<T>(
    private val getter: () -> T,
    private val setter: (T) -> Unit = {},
) {
    /** Creates a read-write reference to [property]. */
    constructor(property: KMutableProperty0<T>) : this({ property.get() }, { property.set(it) })

    /** Creates a read-only reference to [property]. [set] is a no-op. */
    constructor(property: KProperty0<T>) : this({ property.get() })

    /** Returns the current value of the referenced property. */
    fun get(): T = getter()

    /** Writes [value] back to the referenced property. No-op for read-only references. */
    fun set(value: T) = setter(value)
}
