package com.github.mikecraft1224.config

import com.github.mikecraft1224.config.api.Category
import com.github.mikecraft1224.config.api.Collapsible
import com.github.mikecraft1224.config.api.Config
import com.github.mikecraft1224.config.api.DefaultCollapsed
import com.github.mikecraft1224.config.api.EditorBoolean
import com.github.mikecraft1224.config.api.EditorButton
import com.github.mikecraft1224.config.api.EditorColor
import com.github.mikecraft1224.config.api.EditorDropdown
import com.github.mikecraft1224.config.api.EditorInfo
import com.github.mikecraft1224.config.api.EditorKeybind
import com.github.mikecraft1224.config.api.EditorMutable
import com.github.mikecraft1224.config.api.EditorSlider
import com.github.mikecraft1224.config.api.EditorText
import com.github.mikecraft1224.config.api.Entry
import com.github.mikecraft1224.config.api.Excluded
import com.github.mikecraft1224.config.api.SearchTag
import com.github.mikecraft1224.config.api.Separator
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

data class ProcessedConfig(
    val categories: List<ProcessedCategory>,
    val title: String = "",
    val subtitle: String = "",
)

data class ProcessedCategory(
    val name: String,
    val description: String,
    val entries: List<ProcessedEntry>,
    val subcategories: List<ProcessedCategory> = emptyList(),
)

sealed class ProcessedEntry {
    abstract val name: String
    abstract val description: String
    /** Hidden search aliases set from [@SearchTag][com.github.mikecraft1224.config.api.SearchTag]. */
    var searchTags: List<String> = emptyList()

    data class BoolEntry(
        override val name: String,
        override val description: String,
        val get: () -> Boolean,
        val set: (Boolean) -> Unit,
    ) : ProcessedEntry()

    data class SliderEntry(
        override val name: String,
        override val description: String,
        val min: Double,
        val max: Double,
        val step: Double,
        val get: () -> Double,
        val set: (Double) -> Unit,
    ) : ProcessedEntry()

    data class DropdownEntry(
        override val name: String,
        override val description: String,
        val options: List<String>,
        val get: () -> Int,
        val set: (Int) -> Unit,
    ) : ProcessedEntry()

    data class TextEntry(
        override val name: String,
        override val description: String,
        val get: () -> String,
        val set: (String) -> Unit,
    ) : ProcessedEntry()

    data class ButtonEntry(
        override val name: String,
        override val description: String,
        val buttonText: String,
        val action: () -> Unit,
    ) : ProcessedEntry()

    data class ColorEntry(
        override val name: String,
        override val description: String,
        val get: () -> java.awt.Color,
        val set: (java.awt.Color) -> Unit,
    ) : ProcessedEntry()

    data class InfoEntry(
        override val name: String,
        override val description: String,
        val getText: () -> String,
    ) : ProcessedEntry()

    data class KeybindEntry(
        override val name: String,
        override val description: String,
        val fieldName: String,
        val get: () -> Int,
        val set: (Int) -> Unit,
    ) : ProcessedEntry() {
        /**
         * Optional callback invoked by the config screen after the new key is stored.
         * Set via [com.github.mikecraft1224.input.api.KeybindHandle.bindConfigEntry] to update
         * a virtual keybind's runtime key without a restart.
         */
        var onChanged: ((Int) -> Unit)? = null
    }

    /**
     * A mutable list field backed by [EditorMutable].
     * The list is always returned as a [MutableList]; if the field held an immutable list,
     * it is replaced in-place on first access.
     */
    data class MutableListEntry(
        override val name: String,
        override val description: String,
        val elementType: ElementType,
        val defaultElement: Any,
        val getList: () -> MutableList<Any>,
    ) : ProcessedEntry() {
        enum class ElementType { STRING, INT, BOOLEAN }
    }

    class CollapsibleGroup(
        override val name: String,
        override val description: String,
        val children: List<ProcessedEntry>,
        var collapsed: Boolean = false,
    ) : ProcessedEntry()

    /** Visual divider line with an optional text label. */
    class SeparatorEntry(val label: String = "") : ProcessedEntry() {
        override val name = ""
        override val description = ""
    }
}

/**
 * Reflects over a config object and produces a [ProcessedConfig] ready for GUI rendering.
 *
 * Fields are walked in declaration order. Static, synthetic, and `@Excluded(gui=true)` fields
 * are always skipped. `@Category` fields become top-level category pages; `@Collapsible` fields
 * become inline collapsible groups. Bare `@Entry` fields with a recognized editor annotation at
 * the top level are collected into a synthetic "General" category prepended to the list.
 * A `@Separator` annotation on any field inserts a divider line before that field.
 */
object ConfigProcessor {

    fun process(instance: Any): ProcessedConfig {
        val configAnn = instance.javaClass.getAnnotation(Config::class.java)
        val generalEntries = mutableListOf<ProcessedEntry>()
        val categories = mutableListOf<ProcessedCategory>()

        for (field in fields(instance.javaClass)) {
            if (shouldSkipGui(field)) continue
            field.isAccessible = true

            val category = field.getAnnotation(Category::class.java)
            val collapsible = field.getAnnotation(Collapsible::class.java)

            when {
                category != null -> {
                    val nested = field.get(instance) ?: continue
                    categories += ProcessedCategory(
                        name = category.name,
                        description = category.description,
                        entries = processFields(nested),
                        subcategories = collectSubcategories(nested),
                    )
                }

                collapsible != null -> {
                    val entry = field.getAnnotation(Entry::class.java)
                    val name = entry?.name ?: field.name
                    val description = entry?.description ?: ""
                    val nested = field.get(instance) ?: continue
                    field.getAnnotation(Separator::class.java)?.let { sep ->
                        generalEntries += ProcessedEntry.SeparatorEntry(sep.label)
                    }
                    generalEntries += ProcessedEntry.CollapsibleGroup(
                        name = name,
                        description = description,
                        children = processFields(nested),
                        collapsed = field.isAnnotationPresent(DefaultCollapsed::class.java),
                    )
                }

                else -> {
                    field.getAnnotation(Separator::class.java)?.let { sep ->
                        generalEntries += ProcessedEntry.SeparatorEntry(sep.label)
                    }
                    buildEntry(instance, field)?.let { generalEntries += it }
                }
            }
        }

        val result = mutableListOf<ProcessedCategory>()
        if (generalEntries.isNotEmpty()) result += ProcessedCategory("General", "", generalEntries)
        result += categories
        return ProcessedConfig(
            categories = result,
            title = configAnn?.title ?: "",
            subtitle = configAnn?.subtitle ?: "",
        )
    }

    /**
     * Scans [instance] for `@Category`-annotated fields and builds them as sub-categories.
     * Sub-categories cannot be nested further (their own `@Category` fields are ignored).
     */
    private fun collectSubcategories(instance: Any): List<ProcessedCategory> =
        fields(instance.javaClass).mapNotNull { field ->
            if (Modifier.isStatic(field.modifiers) || field.isSynthetic) return@mapNotNull null
            val cat = field.getAnnotation(Category::class.java) ?: return@mapNotNull null
            field.isAccessible = true
            val nested = field.get(instance) ?: return@mapNotNull null
            ProcessedCategory(
                name = cat.name,
                description = cat.description,
                entries = processFields(nested),
            )
        }

    fun processFields(instance: Any): List<ProcessedEntry> {
        val entries = mutableListOf<ProcessedEntry>()

        for (field in fields(instance.javaClass)) {
            if (shouldSkipGui(field)) continue
            field.isAccessible = true

            val collapsible = field.getAnnotation(Collapsible::class.java)
            if (collapsible != null) {
                val entry = field.getAnnotation(Entry::class.java)
                val name = entry?.name ?: field.name
                val description = entry?.description ?: ""
                val nested = field.get(instance) ?: continue
                field.getAnnotation(Separator::class.java)?.let { sep ->
                    entries += ProcessedEntry.SeparatorEntry(sep.label)
                }
                entries += ProcessedEntry.CollapsibleGroup(
                    name = name,
                    description = description,
                    children = processFields(nested),
                    collapsed = field.isAnnotationPresent(DefaultCollapsed::class.java),
                )
                continue
            }

            field.getAnnotation(Separator::class.java)?.let { sep ->
                entries += ProcessedEntry.SeparatorEntry(sep.label)
            }
            buildEntry(instance, field)?.let { entries += it }
        }

        return entries
    }

    fun buildEntry(instance: Any, field: Field): ProcessedEntry? {
        val entry = field.getAnnotation(Entry::class.java) ?: return null
        val name = entry.name
        val desc = entry.description
        val tags = field.getAnnotation(SearchTag::class.java)?.aliases?.toList() ?: emptyList()
        field.isAccessible = true

        // Attaches search tags before returning any built entry
        fun <T : ProcessedEntry> T.withTags(): T { searchTags = tags; return this }

        field.getAnnotation(EditorBoolean::class.java)?.let {
            return ProcessedEntry.BoolEntry(
                name = name, description = desc,
                get = { field.getBoolean(instance) },
                set = { v -> field.setBoolean(instance, v) },
            ).withTags()
        }

        field.getAnnotation(EditorSlider::class.java)?.let { ann ->
            return ProcessedEntry.SliderEntry(
                name = name, description = desc,
                min = ann.min, max = ann.max, step = ann.step,
                get = { (field.get(instance) as Number).toDouble() },
                set = { v ->
                    when {
                        field.type == Int::class.javaPrimitiveType || field.type == Int::class.javaObjectType ->
                            field.setInt(instance, v.toInt())
                        field.type == Float::class.javaPrimitiveType || field.type == Float::class.javaObjectType ->
                            field.setFloat(instance, v.toFloat())
                        else -> field.setDouble(instance, v)
                    }
                },
            ).withTags()
        }

        field.getAnnotation(EditorDropdown::class.java)?.let { ann ->
            // Enum - labels from toString()
            if (field.type.isEnum) {
                @Suppress("UNCHECKED_CAST")
                val constants = field.type.enumConstants as Array<Enum<*>>
                return ProcessedEntry.DropdownEntry(
                    name = name, description = desc,
                    options = constants.map { it.toString() },
                    get = { (field.get(instance) as Enum<*>).ordinal },
                    set = { idx -> field.set(instance, constants[idx]) },
                ).withTags()
            }
            // Int field with explicit labels - stores selected index
            if (ann.values.isNotEmpty() &&
                (field.type == Int::class.javaPrimitiveType || field.type == Int::class.javaObjectType)
            ) {
                return ProcessedEntry.DropdownEntry(
                    name = name, description = desc,
                    options = ann.values.toList(),
                    get = { field.getInt(instance).coerceIn(0, ann.values.lastIndex) },
                    set = { idx -> field.setInt(instance, idx) },
                ).withTags()
            }
            // String field with explicit labels - stores selected label
            if (ann.values.isNotEmpty() && field.type == String::class.java) {
                return ProcessedEntry.DropdownEntry(
                    name = name, description = desc,
                    options = ann.values.toList(),
                    get = { ann.values.indexOf(field.get(instance) as? String ?: "").coerceAtLeast(0) },
                    set = { idx -> field.set(instance, ann.values.getOrElse(idx) { ann.values[0] }) },
                ).withTags()
            }
            return null
        }

        field.getAnnotation(EditorText::class.java)?.let {
            return ProcessedEntry.TextEntry(
                name = name, description = desc,
                get = { field.get(instance) as? String ?: "" },
                set = { v -> field.set(instance, v) },
            ).withTags()
        }

        field.getAnnotation(EditorButton::class.java)?.let { ann ->
            return ProcessedEntry.ButtonEntry(
                name = name, description = desc,
                buttonText = ann.buttonText,
                action = { (field.get(instance) as? Runnable)?.run() },
            ).withTags()
        }

        field.getAnnotation(EditorColor::class.java)?.let {
            return ProcessedEntry.ColorEntry(
                name = name, description = desc,
                get = { field.get(instance) as java.awt.Color },
                set = { v -> field.set(instance, v) },
            ).withTags()
        }

        field.getAnnotation(EditorInfo::class.java)?.let {
            return ProcessedEntry.InfoEntry(
                name = name, description = desc,
                getText = { field.get(instance) as? String ?: "" },
            ).withTags()
        }

        field.getAnnotation(EditorKeybind::class.java)?.let {
            return ProcessedEntry.KeybindEntry(
                name = name, description = desc,
                fieldName = field.name,
                get = { field.getInt(instance) },
                set = { v -> field.setInt(instance, v) },
            ).withTags()
        }

        field.getAnnotation(EditorMutable::class.java)?.let { ann ->
            val generic = field.genericType as? ParameterizedType ?: return null
            val typeArg = generic.actualTypeArguments.firstOrNull() ?: return null
            val (elementType, defaultElement) = when (typeArg) {
                String::class.java ->
                    ProcessedEntry.MutableListEntry.ElementType.STRING to ann.defaultString
                Int::class.javaObjectType, Int::class.javaPrimitiveType ->
                    ProcessedEntry.MutableListEntry.ElementType.INT to ann.defaultInt
                Boolean::class.javaObjectType, Boolean::class.javaPrimitiveType ->
                    ProcessedEntry.MutableListEntry.ElementType.BOOLEAN to ann.defaultBoolean
                else -> return null
            }
            return ProcessedEntry.MutableListEntry(
                name = name, description = desc,
                elementType = elementType,
                defaultElement = defaultElement,
                getList = {
                    val current = field.get(instance)
                    @Suppress("UNCHECKED_CAST")
                    val mutable = when (current) {
                        is MutableList<*> -> current as MutableList<Any>
                        is List<*> -> @Suppress("UNCHECKED_CAST") (current.toMutableList() as MutableList<Any>).also { field.set(instance, it) }
                        else -> mutableListOf<Any>().also { field.set(instance, it) }
                    }
                    mutable
                },
            ).withTags()
        }

        return null
    }

    private fun shouldSkipGui(field: Field): Boolean {
        if (Modifier.isStatic(field.modifiers)) return true
        if (field.isSynthetic) return true
        val excluded = field.getAnnotation(Excluded::class.java)
        return excluded != null && excluded.gui
    }

    private fun fields(clazz: Class<*>): List<Field> = buildList {
        var c: Class<*>? = clazz
        while (c != null && c != Any::class.java) {
            addAll(c.declaredFields)
            c = c.superclass
        }
    }
}
