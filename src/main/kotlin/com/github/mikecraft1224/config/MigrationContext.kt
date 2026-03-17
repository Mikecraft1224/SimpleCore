package com.github.mikecraft1224.config

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * DSL context for transforming a raw config [JsonObject] during a migration step.
 *
 * Passed to the block registered via [ConfigManager.migration].
 *
 * All operations act on **top-level** keys by default. Use [nested] to descend into
 * a sub-object (e.g. a `@Category` field serialized as a JSON object).
 *
 * ### Example
 * ```kotlin
 * manager
 *     .migration(fromVersion = 0) {
 *         rename("oldField", "newField")        // renamed in code
 *         remove("deprecatedFlag")              // removed field
 *     }
 *     .migration(fromVersion = 1) {
 *         nested("advanced") {
 *             rename("multiplierOld", "multiplier")
 *         }
 *         transformValue("quality") { old ->    // changed from String to Int index
 *             val idx = listOf("Low", "Medium", "High").indexOf(old.asString)
 *             ConfigSerializer.gson.toJsonTree(idx.coerceAtLeast(0))
 *         }
 *     }
 * ```
 */
class MigrationContext internal constructor(internal var json: JsonObject) {

    /**
     * Renames a top-level key.  No-op if [old] is absent.
     */
    fun rename(old: String, new: String) {
        val value = json.remove(old) ?: return
        json.add(new, value)
    }

    /**
     * Removes one or more top-level keys.  Missing keys are silently ignored.
     */
    fun remove(vararg keys: String) = keys.forEach { json.remove(it) }

    /**
     * Replaces the value at [key] by passing the current value through [transform].
     * No-op if [key] is absent.
     */
    fun transformValue(key: String, transform: (JsonElement) -> JsonElement) {
        val current = json.get(key) ?: return
        json.add(key, transform(current))
    }

    /**
     * Navigates into a nested JSON object (a serialized sub-config or `@Category` field)
     * and applies further migrations within that scope.  No-op if [key] is absent or not
     * a JSON object.
     */
    fun nested(key: String, block: MigrationContext.() -> Unit) {
        val nested = json.getAsJsonObject(key) ?: return
        val ctx = MigrationContext(nested)
        ctx.block()
        json.add(key, ctx.json)
    }
}
