package com.github.mikecraft1224.config

import com.github.mikecraft1224.Logger
import com.google.gson.JsonObject
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Manages persistence of a config object to/from a JSON file.
 *
 * Fields are serialized via reflection. Fields annotated with
 * [@Excluded(config = true)][com.github.mikecraft1224.config.api.Excluded] or of type [Runnable]
 * are skipped. Nested objects are serialized recursively. Static and synthetic fields are always skipped.
 *
 * Loading merges values into the existing [config] instance in-place, so held references remain
 * valid and unrecognized keys are silently ignored (defaults are preserved).
 *
 * Saves are atomic: written to a `.tmp` sibling, validated, then moved into place.
 *
 * ### Migrations
 * Use [migration] to register version-based migration steps that run automatically on load
 * when the saved `_version` is older than the current schema:
 * ```kotlin
 * val manager = ConfigManager.of(MyConfig(), "mymod")
 *     .migration(fromVersion = 0) {
 *         rename("oldName", "newName")
 *         remove("removedField")
 *     }
 *     .migration(fromVersion = 1) {
 *         nested("advanced") { rename("speedOld", "speed") }
 *     }
 * manager.load()
 * ```
 *
 * ### Basic usage
 * ```kotlin
 * val manager = ConfigManager.of(MyConfig(), "mymod")
 *     .onLoadFailed { e -> /* handle corrupt file */ }
 * manager.load()
 * manager.config.someField
 * manager.save()
 * ```
 */
class ConfigManager<T : Any>(
    val config: T,
    val file: Path,
) {
    private val reloadListeners = mutableListOf<(T) -> Unit>()
    private var saveFailedHandler: ((Exception) -> Unit)? = null
    private var loadFailedHandler: ((Exception) -> Unit)? = null

    /** Registered migration steps sorted by [fromVersion]. */
    private val migrations = mutableListOf<Pair<Int, MigrationContext.() -> Unit>>()

    /**
     * The schema version that will be written on the next [save].
     * Equal to `max(fromVersion) + 1` across all registered migrations, or 0 if none.
     */
    val currentVersion: Int get() = migrations.maxOfOrNull { it.first }?.let { it + 1 } ?: 0

    // ── Migrations ────────────────────────────────────────────────────────

    /**
     * Registers a migration that runs when the saved config's `_version` is ≤ [fromVersion].
     *
     * Steps are executed in ascending [fromVersion] order during [load].  The [block] receives
     * a [MigrationContext] that exposes helpers for renaming, removing, and transforming keys.
     *
     * Returns `this` for chaining.
     */
    fun migration(fromVersion: Int, block: MigrationContext.() -> Unit): ConfigManager<T> {
        migrations += fromVersion to block
        migrations.sortBy { it.first }
        return this
    }

    // ── Persistence ───────────────────────────────────────────────────────

    /**
     * Serializes [config] to [file] atomically.
     *
     * If any migrations are registered, a `_version` metadata key is added to the JSON
     * so future loads can determine which migrations still need to run.
     */
    fun save() {
        runCatching {
            val json = ConfigSerializer.toJson(config)
            if (migrations.isNotEmpty()) json.addProperty("_version", currentVersion)
            ConfigSerializer.saveJson(json, file)
        }.onFailure { e ->
            val ex = e as? Exception ?: RuntimeException(e)
            Logger.error("[Config] Failed to save '${file.fileName}'", e)
            saveFailedHandler?.invoke(ex)
        }
    }

    /**
     * Loads [file] into [config] in-place, running any pending migrations first.
     *
     * If [migrations] are registered and the saved `_version` is less than [currentVersion],
     * each migration whose [fromVersion] ≥ savedVersion is executed in order before
     * the JSON is merged into the config object.
     */
    fun load() {
        runCatching {
            if (!file.exists()) return@runCatching
            var json = ConfigSerializer.gson.fromJson(file.readText(), JsonObject::class.java)
                ?: return@runCatching

            if (migrations.isNotEmpty()) {
                val savedVersion = json.get("_version")?.asInt ?: 0
                for ((fromVersion, block) in migrations) {
                    if (savedVersion <= fromVersion) {
                        val ctx = MigrationContext(json)
                        ctx.block()
                        json = ctx.json
                    }
                }
            }

            ConfigSerializer.mergeFromJson(config, json)
        }.onFailure { e ->
            val ex = e as? Exception ?: RuntimeException(e)
            Logger.warn("[Config] Failed to load '${file.fileName}': ${e.message}")
            loadFailedHandler?.invoke(ex)
        }
    }

    /** Re-reads [file] into [config] and notifies all [onReload] listeners. */
    fun reload() {
        load()
        reloadListeners.forEach { it(config) }
    }

    // ── Listener registration (fluent) ────────────────────────────────────

    /** Registers [listener] to be called after every [reload]. Returns `this` for chaining. */
    fun onReload(listener: (T) -> Unit): ConfigManager<T> {
        reloadListeners.add(listener)
        return this
    }

    /** Registers a handler called when [save] fails. Returns `this` for chaining. */
    fun onSaveFailed(handler: (Exception) -> Unit): ConfigManager<T> {
        saveFailedHandler = handler
        return this
    }

    /** Registers a handler called when [load] fails (e.g. corrupt file). Returns `this` for chaining. */
    fun onLoadFailed(handler: (Exception) -> Unit): ConfigManager<T> {
        loadFailedHandler = handler
        return this
    }

    companion object {
        /** Creates a [ConfigManager] storing the config at `<configDir>/<modId>.json`. */
        fun <T : Any> of(config: T, modId: String): ConfigManager<T> =
            ConfigManager(config, FabricLoader.getInstance().configDir.resolve("$modId.json"))

        /** Creates a [ConfigManager] storing the config at [file]. */
        fun <T : Any> of(config: T, file: Path): ConfigManager<T> =
            ConfigManager(config, file)
    }
}
