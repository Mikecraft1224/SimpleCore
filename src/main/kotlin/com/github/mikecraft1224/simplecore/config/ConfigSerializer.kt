package com.github.mikecraft1224.simplecore.config

import com.github.mikecraft1224.simplecore.Logger
import com.github.mikecraft1224.simplecore.config.api.annotations.Excluded
import com.github.mikecraft1224.simplecore.config.api.values.Button
import com.github.mikecraft1224.simplecore.config.api.values.Dropdown
import com.github.mikecraft1224.simplecore.config.api.values.DropdownList
import com.github.mikecraft1224.simplecore.config.api.values.Keybind
import com.github.mikecraft1224.simplecore.config.api.values.MultiSelect
import com.github.mikecraft1224.simplecore.config.api.values.Property
import com.github.mikecraft1224.simplecore.config.api.values.Reference
import com.github.mikecraft1224.simplecore.config.api.values.Visible
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.awt.Color
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
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

    private val listOfStringType = object : TypeToken<List<String>>() {}.type
    private val listOfIntType    = object : TypeToken<List<Int>>() {}.type

    private fun shouldSkip(field: java.lang.reflect.Field): Boolean {
        if (Modifier.isStatic(field.modifiers)) return true
        if (field.isSynthetic) return true
        if (Runnable::class.java.isAssignableFrom(field.type)) return true
        if (Button::class.java.isAssignableFrom(field.type)) return true
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
                value is Dropdown     -> gson.toJsonTree(value.index)
                value is MultiSelect  -> gson.toJsonTree(value.selected, listOfStringType)
                value is DropdownList -> gson.toJsonTree(value.indices, listOfIntType)
                value is Keybind      -> gson.toJsonTree(value.packed)
                value is Reference<*> -> serializeReference(field, value)
                value is Visible<*>   -> serializeVisible(value)
                value is Property<*>  -> serializeProperty(value)
                isLeaf(field.type)  -> gson.toJsonTree(value, field.genericType)
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
            val existing = field.get(instance)
            when {
                existing is Dropdown -> runCatching {
                    existing.index = gson.fromJson(element, Int::class.javaObjectType) ?: 0
                }.onFailure { Logger.warn("[Config] Failed to deserialize '${field.name}': ${it.message}") }

                existing is MultiSelect -> runCatching {
                    @Suppress("UNCHECKED_CAST")
                    val list = gson.fromJson(element, listOfStringType) as? List<String> ?: return@runCatching
                    existing.selected.clear()
                    existing.selected.addAll(list)
                }.onFailure { Logger.warn("[Config] Failed to deserialize '${field.name}': ${it.message}") }

                existing is DropdownList -> runCatching {
                    @Suppress("UNCHECKED_CAST")
                    val list = gson.fromJson(element, listOfIntType) as? List<Int> ?: return@runCatching
                    existing.indices.clear()
                    existing.indices.addAll(list)
                }.onFailure { Logger.warn("[Config] Failed to deserialize '${field.name}': ${it.message}") }

                existing is Keybind -> runCatching {
                    existing.packed = gson.fromJson(element, Int::class.javaObjectType) ?: 0
                }.onFailure { Logger.warn("[Config] Failed to deserialize '${field.name}': ${it.message}") }

                existing is Reference<*> -> runCatching {
                    deserializeReference(existing, element, field)
                }.onFailure { Logger.warn("[Config] Failed to deserialize Reference '${field.name}': ${it.message}") }

                existing is Visible<*> -> runCatching {
                    deserializeVisible(existing, element, field)
                }.onFailure { Logger.warn("[Config] Failed to deserialize Visible '${field.name}': ${it.message}") }

                existing is Property<*> -> runCatching {
                    deserializeProperty(existing, element)
                }.onFailure { Logger.warn("[Config] Failed to deserialize Property '${field.name}': ${it.message}") }

                isLeaf(field.type) ->
                    runCatching { field.set(instance, gson.fromJson(element, field.genericType)) }
                        .onFailure { e -> Logger.warn("[Config] Failed to deserialize '${field.name}': ${e.message}") }

                List::class.java.isAssignableFrom(field.type) -> {
                    // For object-element lists, create instances and merge field-by-field so that
                    // value types (Dropdown, Keybind, etc.) inside element classes are preserved.
                    val elemType = (field.genericType as? ParameterizedType)
                        ?.actualTypeArguments?.firstOrNull() as? Class<*>
                    if (elemType != null && !isLeaf(elemType) && !elemType.isEnum && element.isJsonArray) {
                        val newList = mutableListOf<Any>()
                        for (item in element.asJsonArray) {
                            if (!item.isJsonObject) continue
                            runCatching {
                                val elem = elemType.getDeclaredConstructor().newInstance()
                                mergeFromJson(elem, item.asJsonObject)
                                newList.add(elem)
                            }.onFailure { Logger.warn("[Config] Failed to load element '${elemType.simpleName}': ${it.message}") }
                        }
                        runCatching { field.set(instance, newList) }
                            .onFailure { Logger.warn("[Config] Failed to set '${field.name}': ${it.message}") }
                    } else {
                        runCatching { field.set(instance, gson.fromJson(element, field.genericType)) }
                            .onFailure { e -> Logger.warn("[Config] Failed to deserialize '${field.name}': ${e.message}") }
                    }
                }

                element.isJsonObject -> {
                    val nested = existing ?: continue
                    mergeFromJson(nested, element.asJsonObject)
                }
            }
        }
    }

    /**
     * Serializes a [Reference] field. Reads the referenced value via [Reference.get] and
     * serializes it using the type argument `T` extracted from the field's generic type.
     */
    private fun serializeReference(field: java.lang.reflect.Field, ref: Reference<*>): JsonElement {
        val value = ref.get() ?: return gson.toJsonTree(null as Any?)
        val typeArg = (field.genericType as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
        return if (typeArg != null) gson.toJsonTree(value, typeArg) else gson.toJsonTree(value)
    }

    /**
     * Deserializes a [Reference] field. Reads the type argument `T` from the field's generic
     * type, deserializes the JSON element as `T`, and calls [Reference.set] to write back.
     */
    @Suppress("UNCHECKED_CAST")
    private fun deserializeReference(ref: Reference<*>, element: JsonElement, field: java.lang.reflect.Field) {
        val typeArg = (field.genericType as? ParameterizedType)?.actualTypeArguments?.firstOrNull()
            ?: return
        val value: Any? = gson.fromJson(element, typeArg)
        (ref as Reference<Any?>).set(value)
    }

    /**
     * Serializes a [Visible] field by serializing only the wrapped [Visible.value],
     * ignoring the [Visible.condition] reference (not persistable).
     */
    private fun serializeVisible(vis: Visible<*>): JsonElement {
        val inner = vis.value ?: return gson.toJsonTree(null as Any?)
        // If the inner value is itself a Reference, serialize what it points to
        val effective: Any = (if (inner is Reference<*>) inner.get() else inner)
            ?: return gson.toJsonTree(null as Any?)
        return when {
            effective is Dropdown     -> gson.toJsonTree(effective.index)
            effective is MultiSelect  -> gson.toJsonTree(effective.selected, listOfStringType)
            effective is DropdownList -> gson.toJsonTree(effective.indices, listOfIntType)
            effective is Keybind      -> gson.toJsonTree(effective.packed)
            isLeaf(effective.javaClass) -> gson.toJsonTree(effective, effective.javaClass)
            effective is List<*>      -> gson.toJsonTree(effective)
            else -> toJson(effective)
        }
    }

    /**
     * Deserializes a [Visible] field by restoring the wrapped [Visible.value] in-place.
     * The [Visible.condition] is a property reference and is never persisted.
     */
    private fun deserializeVisible(vis: Visible<*>, element: JsonElement, field: java.lang.reflect.Field) {
        val inner = vis.value ?: return
        val vf = Visible::class.java.getDeclaredField("value").also { it.isAccessible = true }
        // If the inner value is itself a Reference, deserialize into what it points to
        if (inner is Reference<*>) {
            deserializeIntoReference(inner, element)
            return
        }
        when {
            inner is Dropdown     -> inner.index  = gson.fromJson(element, Int::class.javaObjectType) ?: 0
            inner is MultiSelect  -> {
                @Suppress("UNCHECKED_CAST")
                val list = gson.fromJson(element, listOfStringType) as? List<String> ?: return
                inner.selected.clear(); inner.selected.addAll(list)
            }
            inner is DropdownList -> {
                @Suppress("UNCHECKED_CAST")
                val list = gson.fromJson(element, listOfIntType) as? List<Int> ?: return
                inner.indices.clear(); inner.indices.addAll(list)
            }
            inner is Keybind      -> inner.packed = gson.fromJson(element, Int::class.javaObjectType) ?: 0
            isLeaf(inner.javaClass) -> vf.set(vis, gson.fromJson(element, inner.javaClass))
            element.isJsonObject  -> mergeFromJson(inner, element.asJsonObject)
        }
    }

    private fun serializeProperty(prop: Property<*>): JsonElement {
        val inner = prop.value ?: return gson.toJsonTree(null as Any?)
        return when {
            inner is Dropdown     -> gson.toJsonTree(inner.index)
            inner is MultiSelect  -> gson.toJsonTree(inner.selected, listOfStringType)
            inner is DropdownList -> gson.toJsonTree(inner.indices, listOfIntType)
            inner is Keybind      -> gson.toJsonTree(inner.packed)
            isLeaf(inner.javaClass) -> gson.toJsonTree(inner, inner.javaClass)
            inner is List<*>      -> gson.toJsonTree(inner)
            else -> toJson(inner)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeProperty(prop: Property<*>, element: JsonElement) {
        val inner = prop.value ?: return
        when {
            inner is Dropdown -> inner.index = gson.fromJson(element, Int::class.javaObjectType) ?: 0
            inner is MultiSelect -> {
                val list = gson.fromJson(element, listOfStringType) as? List<String> ?: return
                inner.selected.clear(); inner.selected.addAll(list)
            }
            inner is DropdownList -> {
                val list = gson.fromJson(element, listOfIntType) as? List<Int> ?: return
                inner.indices.clear(); inner.indices.addAll(list)
            }
            inner is Keybind -> inner.packed = gson.fromJson(element, Int::class.javaObjectType) ?: 0
            isLeaf(inner.javaClass) -> (prop as Property<Any?>).value = gson.fromJson(element, inner.javaClass)
            element.isJsonObject -> mergeFromJson(inner, element.asJsonObject)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun deserializeIntoReference(ref: Reference<*>, element: JsonElement) {
        val current = ref.get() ?: return
        val refAny = ref as Reference<Any?>
        when {
            isLeaf(current.javaClass) -> refAny.set(gson.fromJson(element, current.javaClass))
            current is MutableList<*> && element.isJsonArray -> {
                current.clear()
                element.asJsonArray.mapNotNullTo(current as MutableList<Any?>) { item ->
                    when {
                        item.isJsonPrimitive -> gson.fromJson(item, Any::class.java)
                        item.isJsonObject    -> gson.fromJson(item, Any::class.java)
                        else -> null
                    }
                }
            }
            element.isJsonObject -> mergeFromJson(current, element.asJsonObject)
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
