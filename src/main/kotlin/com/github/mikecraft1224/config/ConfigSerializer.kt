package com.github.mikecraft1224.config

import com.github.mikecraft1224.Logger
import com.github.mikecraft1224.config.api.Excluded
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.awt.Color
import java.lang.reflect.Modifier
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Suppress("Unused")
internal object ConfigSerializer {

    private val colorAdapter = object : TypeAdapter<Color>() {
        override fun write(out: JsonWriter, value: Color?) {
            if (value == null) out.nullValue() else out.value(value.rgb)
        }

        override fun read(reader: JsonReader): Color? {
            if (reader.peek() == JsonToken.NULL) { reader.nextNull(); return null }
            return Color(reader.nextInt(), true)
        }
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(Color::class.java, colorAdapter)
        .create()

    private fun shouldSkip(field: java.lang.reflect.Field): Boolean {
        if (Modifier.isStatic(field.modifiers)) return true
        if (field.isSynthetic) return true
        if (Runnable::class.java.isAssignableFrom(field.type)) return true
        val excluded = field.getAnnotation(Excluded::class.java)
        return excluded != null && excluded.config
    }

    private fun isLeaf(type: Class<*>): Boolean =
        type.isPrimitive
            || type == String::class.java
            || Number::class.java.isAssignableFrom(type)
            || type == Boolean::class.javaObjectType
            || type.isEnum
            || Color::class.java.isAssignableFrom(type)

    private fun fields(clazz: Class<*>): List<java.lang.reflect.Field> = buildList {
        var c: Class<*>? = clazz
        while (c != null && c != Any::class.java) {
            addAll(c.declaredFields)
            c = c.superclass
        }
    }

    fun toJson(instance: Any): JsonObject {
        val obj = JsonObject()
        for (field in fields(instance.javaClass)) {
            if (shouldSkip(field)) continue
            field.isAccessible = true
            val value = field.get(instance) ?: continue
            val element: JsonElement = when {
                isLeaf(field.type) -> gson.toJsonTree(value, field.genericType)
                List::class.java.isAssignableFrom(field.type) -> gson.toJsonTree(value, field.genericType)
                else -> toJson(value)
            }
            obj.add(field.name, element)
        }
        return obj
    }

    fun mergeFromJson(instance: Any, json: JsonObject) {
        for (field in fields(instance.javaClass)) {
            if (shouldSkip(field)) continue
            field.isAccessible = true
            val element = json.get(field.name) ?: continue
            when {
                isLeaf(field.type) || List::class.java.isAssignableFrom(field.type) ->
                    runCatching { field.set(instance, gson.fromJson(element, field.genericType)) }
                        .onFailure { e ->
                            Logger.warn("[Config] Failed to deserialize '${field.name}': ${e.message}")
                        }

                element.isJsonObject -> {
                    val nested = field.get(instance) ?: continue
                    mergeFromJson(nested, element.asJsonObject)
                }
            }
        }
    }

    /**
     * Serializes [instance] to [file] using an atomic write:
     * writes to a sibling `.tmp` file first, validates it deserializes cleanly,
     * then atomically replaces the target. Throws on any failure so [ConfigManager] can route it.
     */
    fun save(instance: Any, file: Path) = saveJson(toJson(instance), file)

    /**
     * Atomically writes a pre-built [JsonObject] to [file].
     * Callers (e.g. [ConfigManager]) may add metadata fields (like `_version`) to the object
     * before calling this.
     */
    fun saveJson(json: JsonObject, file: Path) {
        file.parent?.createDirectories()
        val tmp  = file.resolveSibling("${file.fileName}.tmp")
        val text = gson.toJson(json)
        tmp.writeText(text)
        // Validate before overwriting - catches serialization bugs before data loss
        gson.fromJson(tmp.readText(), JsonObject::class.java)
            ?: throw IllegalStateException("Serialized JSON could not be parsed back")
        Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    /** Deserializes [file] and merges values into [instance] in-place. Throws on failure. */
    fun load(instance: Any, file: Path) {
        if (!file.exists()) return
        val json = gson.fromJson(file.readText(), JsonObject::class.java) ?: return
        mergeFromJson(instance, json)
    }
}
