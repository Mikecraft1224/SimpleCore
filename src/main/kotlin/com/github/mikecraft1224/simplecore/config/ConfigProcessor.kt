package com.github.mikecraft1224.simplecore.config

import com.github.mikecraft1224.simplecore.config.api.annotations.Category
import com.github.mikecraft1224.simplecore.config.api.annotations.Collapsible
import com.github.mikecraft1224.simplecore.config.api.annotations.Conditional
import com.github.mikecraft1224.simplecore.config.api.annotations.Config
import com.github.mikecraft1224.simplecore.config.api.annotations.DefaultCollapsed
import com.github.mikecraft1224.simplecore.config.api.annotations.Entry
import com.github.mikecraft1224.simplecore.config.api.annotations.Excluded
import com.github.mikecraft1224.simplecore.config.api.annotations.Info
import com.github.mikecraft1224.simplecore.config.api.annotations.ListEditor
import com.github.mikecraft1224.simplecore.config.api.annotations.Order
import com.github.mikecraft1224.simplecore.config.api.annotations.SearchTag
import com.github.mikecraft1224.simplecore.config.api.annotations.Separator
import com.github.mikecraft1224.simplecore.config.api.annotations.Slider
import com.github.mikecraft1224.simplecore.config.api.annotations.VisibilityCondition
import com.github.mikecraft1224.simplecore.config.api.values.Button
import com.github.mikecraft1224.simplecore.config.api.values.Dropdown
import com.github.mikecraft1224.simplecore.config.api.values.DropdownList
import com.github.mikecraft1224.simplecore.config.api.values.Keybind as ConfigKeybind
import com.github.mikecraft1224.simplecore.config.api.values.MultiSelect
import com.github.mikecraft1224.simplecore.config.api.values.Property
import com.github.mikecraft1224.simplecore.config.api.values.Reference
import com.github.mikecraft1224.simplecore.config.api.values.Visible
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0

data class ProcessedConfig(
    val categories: List<ProcessedCategory>,
    val title: String = "",
    val subtitle: String = "",
    /** Accent color in ARGB. 0 means use the screen default (C_BLUE). */
    val accentColor: Int = 0,
    /** Whether the search bar should be shown on the config screen. */
    val searchEnabled: Boolean = true,
    /** Name of the category to open by default. Empty string selects the first category. */
    val defaultCategory: String = "",
    /** Whether the search bar should be focused automatically when the screen opens. */
    val autoFocusSearch: Boolean = false,
)

data class ProcessedCategory(
    val name: String,
    val description: String,
    val entries: List<ProcessedEntry>,
    val subcategories: List<ProcessedCategory> = emptyList(),
    /**
     * Evaluated on every render frame by the config screen to decide whether this
     * category (or subcategory) is currently visible in the sidebar and selectable.
     * Defaults to `{ true }` (always visible). Set by [ConfigProcessor] from the
     * [@Category][com.github.mikecraft1224.simplecore.config.api.annotations.Category] `condition` parameter,
     * or overridden at runtime via [bindCategoryWhen].
     */
    var condition: () -> Boolean = { true },
    /** The Kotlin/Java field name this category was built from. Used by [bindCategoryWhen]. */
    val fieldName: String = "",
)

sealed class ProcessedEntry {
    abstract val name: String
    abstract val description: String
    /** Hidden search aliases set from [@SearchTag][com.github.mikecraft1224.simplecore.config.api.annotations.SearchTag]. */
    var searchTags: List<String> = emptyList()
    /**
     * Runtime predicate checked on every render frame. When this returns `false` the entry
     * is hidden from the row list without modifying the underlying model.
     */
    var visible: () -> Boolean = { true }
    /**
     * The Kotlin / Java field name this entry was built from. Used by the runtime binding
     * extensions ([bindVisible], [bindDropdownOptions]) to locate entries by property name.
     */
    var fieldName: String = ""

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
        val options: () -> List<String>,
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
        val get: () -> Int,
        val set: (Int) -> Unit,
        /** The packed value at config construction time; used by the config screen's reset button. */
        val defaultPacked: Int = 0,
    ) : ProcessedEntry() {
        /**
         * Optional callback invoked by the config screen after the new key is stored.
         * Set via [com.github.mikecraft1224.simplecore.input.api.KeybindHandle.bindConfigEntry] to update
         * a virtual keybind's runtime key without a restart.
         */
        var onChanged: ((Int) -> Unit)? = null
    }

    /**
     * A mutable list field.
     *
     * When [elementType] is [ElementType.DROPDOWN], each list element is an [Int] (selected index)
     * and [dropdownOptions] provides the shared option labels for every item.
     */
    data class MutableListEntry(
        override val name: String,
        override val description: String,
        val elementType: ElementType,
        val defaultElement: Any,
        val getList: () -> MutableList<Any>,
        /** Non-null only for [ElementType.DROPDOWN]; provides the shared options for all list items. */
        val dropdownOptions: (() -> List<String>)? = null,
        /** When true, the delete button is disabled when only one item remains. */
        val requireNonEmpty: Boolean = false,
    ) : ProcessedEntry() {
        enum class ElementType { STRING, INT, BOOLEAN, DROPDOWN }
    }

    /**
     * A list of structured objects, each with its own annotated fields.
     */
    data class ObjectListEntry(
        override val name: String,
        override val description: String,
        val getList: () -> MutableList<Any>,
        /** Creates a new element instance with default field values. */
        val createElement: () -> Any,
        /** Builds [ProcessedEntry] for a specific element instance (called when Edit is clicked). */
        val buildElementEntries: (instance: Any) -> List<ProcessedEntry>,
        /** Short label for the list row (e.g. first String field value or toString). */
        val elementLabel: (instance: Any) -> String,
    ) : ProcessedEntry()

    class CollapsibleGroup(
        override val name: String,
        override val description: String,
        val children: List<ProcessedEntry>,
        var collapsed: Boolean = false,
    ) : ProcessedEntry()

    /**
     * A multi-select field backed by a [MultiSelect] value.
     */
    data class MultiSelectEntry(
        override val name: String,
        override val description: String,
        val options: () -> List<String>,
        val getSelected: () -> MutableList<String>,
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
 * become inline collapsible groups. Bare `@Entry` fields at the top level are collected into a
 * synthetic "General" category prepended to the list. A `@Separator` annotation inserts a
 * divider line before that field.
 *
 * Widget type is inferred from the field's value type - no `@Editor*` annotations required.
 * Numeric fields that should render as sliders need `@Slider(min, max)`; read-only string
 * fields need `@Info`.
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
                    val raw = field.get(instance) ?: continue
                    val vis = raw as? Visible<*>
                    val nested = vis?.value ?: raw
                    val condFn: () -> Boolean = if (vis != null) {
                        { vis.condition.get() }
                    } else {
                        resolveConditional(field, instance)
                    }
                    categories += ProcessedCategory(
                        name = category.name,
                        description = category.description,
                        entries = processFields(nested),
                        subcategories = collectSubcategories(nested),
                        condition = condFn,
                        fieldName = field.name,
                    )
                }

                collapsible != null -> {
                    val entry = field.getAnnotation(Entry::class.java)
                    val name = entry?.name?.ifEmpty { camelToTitle(field.name) } ?: camelToTitle(field.name)
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
            accentColor = configAnn?.accentColor ?: 0,
            searchEnabled = configAnn?.searchEnabled ?: true,
            defaultCategory = configAnn?.defaultCategory ?: "",
            autoFocusSearch = configAnn?.autoFocusSearch ?: false,
        )
    }

    private fun collectSubcategories(instance: Any): List<ProcessedCategory> =
        fields(instance.javaClass).mapNotNull { field ->
            if (Modifier.isStatic(field.modifiers) || field.isSynthetic) return@mapNotNull null
            val cat = field.getAnnotation(Category::class.java) ?: return@mapNotNull null
            field.isAccessible = true
            val raw = field.get(instance) ?: return@mapNotNull null
            val vis = raw as? Visible<*>
            val nested = vis?.value ?: raw
            val condFn: () -> Boolean = if (vis != null) {
                { vis.condition.get() }
            } else {
                resolveConditional(field, instance)
            }
            ProcessedCategory(
                name = cat.name,
                description = cat.description,
                entries = processFields(nested),
                condition = condFn,
                fieldName = field.name,
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
                val name = entry?.name?.ifEmpty { camelToTitle(field.name) } ?: camelToTitle(field.name)
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
        val name = entry.name.ifEmpty { camelToTitle(field.name) }
        val desc = entry.description
        val tags = field.getAnnotation(SearchTag::class.java)?.aliases?.toList() ?: emptyList()
        field.isAccessible = true

        // Detect Visible<T> and Property<T> wrappers - extract the real value underneath.
        val rawValue = field.get(instance)
        val vis = rawValue as? Visible<*>
        val prop = rawValue as? Property<*>
        val effectiveValue = prop?.value ?: vis?.value ?: rawValue

        // Unified accessors that transparently read/write through the wrapper when present.
        val visValueField: Field? = vis?.let {
            Visible::class.java.getDeclaredField("value").also { f -> f.isAccessible = true }
        }
        val getEff: () -> Any? = when {
            prop != null -> ({ prop.value })
            vis  != null -> ({ vis.value })
            else         -> ({ field.get(instance) })
        }
        @Suppress("UNCHECKED_CAST")
        val setEff: (Any?) -> Unit = when {
            prop != null -> ({ v -> (prop as Property<Any?>).value = v })
            vis  != null -> ({ v -> visValueField!!.set(vis, v) })
            else         -> ({ v -> field.set(instance, v) })
        }

        fun <T : ProcessedEntry> T.withMeta(): T {
            searchTags = tags
            fieldName = field.name
            val annCond = resolveConditional(field, instance)
            visible = if (vis != null) ({ annCond() && vis.condition.get() }) else annCond
            return this
        }

        // @Slider - must come first; numeric fields alone are not auto-inferred
        field.getAnnotation(Slider::class.java)?.let { ann ->
            val step = if (ann.step > 0.0) ann.step else (ann.max - ann.min) / 100.0
            return ProcessedEntry.SliderEntry(
                name = name, description = desc,
                min = ann.min, max = ann.max, step = step,
                get = { (getEff() as Number).toDouble() },
                set = { v ->
                    when (getEff()) {
                        is Int   -> setEff(v.toInt())
                        is Float -> setEff(v.toFloat())
                        else     -> setEff(v)
                    }
                },
            ).withMeta()
        }

        // Value types - inferred from effectiveValue (unwrapped from Visible if needed)
        when (val v = effectiveValue) {
            is Dropdown -> return ProcessedEntry.DropdownEntry(
                name = name, description = desc,
                options = v.options,
                get = { v.index.coerceAtLeast(0) },
                set = { idx -> v.index = idx },
            ).withMeta()

            is MultiSelect -> return ProcessedEntry.MultiSelectEntry(
                name = name, description = desc,
                options = v.options,
                getSelected = { v.selected },
            ).withMeta()

            is Button -> return ProcessedEntry.ButtonEntry(
                name = name, description = desc,
                buttonText = v.label,
                action = v.action,
            ).withMeta()

            is ConfigKeybind -> return ProcessedEntry.KeybindEntry(
                name = name, description = desc,
                get = { v.packed },
                set = { p -> v.packed = p },
                defaultPacked = v.defaultPacked,
            ).withMeta()

            is DropdownList -> return ProcessedEntry.MutableListEntry(
                name = name, description = desc,
                elementType = ProcessedEntry.MutableListEntry.ElementType.DROPDOWN,
                defaultElement = 0,
                getList = {
                    @Suppress("UNCHECKED_CAST")
                    v.indices as MutableList<Any>
                },
                dropdownOptions = v.options,
                requireNonEmpty = field.getAnnotation(ListEditor::class.java)?.requireNonEmpty ?: false,
            ).withMeta()
        }

        // Scalar type inference - check effectiveValue type for Visible<T>, field type otherwise
        val effType = effectiveValue?.javaClass
        val isBoolean = effType == Boolean::class.javaObjectType
            || field.type == Boolean::class.javaPrimitiveType
            || field.type == Boolean::class.javaObjectType
        if (isBoolean) {
            return ProcessedEntry.BoolEntry(
                name = name, description = desc,
                get = { getEff() as Boolean },
                set = { b -> setEff(b) },
            ).withMeta()
        }

        val isString = effType == String::class.java || field.type == String::class.java
        if (isString) {
            return if (field.isAnnotationPresent(Info::class.java)) {
                ProcessedEntry.InfoEntry(name = name, description = desc,
                    getText = { getEff() as? String ?: "" }).withMeta()
            } else {
                ProcessedEntry.TextEntry(name = name, description = desc,
                    get = { getEff() as? String ?: "" },
                    set = { s -> setEff(s) }).withMeta()
            }
        }

        val isEnum = effType?.isEnum == true || field.type.isEnum
        if (isEnum) {
            @Suppress("UNCHECKED_CAST")
            val constants = (effType?.takeIf { it.isEnum } ?: field.type).enumConstants as Array<Enum<*>>
            return ProcessedEntry.DropdownEntry(
                name = name, description = desc,
                options = { constants.map { it.toString() } },
                get = { (getEff() as Enum<*>).ordinal },
                set = { idx -> setEff(constants[idx]) },
            ).withMeta()
        }

        val isColor = effType?.let { java.awt.Color::class.java.isAssignableFrom(it) } == true
            || java.awt.Color::class.java.isAssignableFrom(field.type)
        if (isColor) {
            return ProcessedEntry.ColorEntry(
                name = name, description = desc,
                get = { getEff() as java.awt.Color },
                set = { c -> setEff(c) },
            ).withMeta()
        }

        // MutableList / Reference<MutableList<T>> / Visible<Reference<MutableList<T>>>
        val isList = MutableList::class.java.isAssignableFrom(field.type)
                  || List::class.java.isAssignableFrom(field.type)
                  || Reference::class.java.isAssignableFrom(field.type)
                  || effectiveValue is Reference<*>
        if (isList) {
            val typeArg = listElementType(field) ?: return null
            val requireNonEmpty = field.getAnnotation(ListEditor::class.java)?.requireNonEmpty ?: false
            when (typeArg) {
                String::class.java -> return ProcessedEntry.MutableListEntry(
                    name = name, description = desc,
                    elementType = ProcessedEntry.MutableListEntry.ElementType.STRING,
                    defaultElement = "",
                    getList = buildGetListLambda(field, instance),
                    requireNonEmpty = requireNonEmpty,
                ).withMeta()
                Int::class.javaObjectType, Int::class.javaPrimitiveType -> return ProcessedEntry.MutableListEntry(
                    name = name, description = desc,
                    elementType = ProcessedEntry.MutableListEntry.ElementType.INT,
                    defaultElement = 0,
                    getList = buildGetListLambda(field, instance),
                    requireNonEmpty = requireNonEmpty,
                ).withMeta()
                Boolean::class.javaObjectType, Boolean::class.javaPrimitiveType -> return ProcessedEntry.MutableListEntry(
                    name = name, description = desc,
                    elementType = ProcessedEntry.MutableListEntry.ElementType.BOOLEAN,
                    defaultElement = false,
                    getList = buildGetListLambda(field, instance),
                    requireNonEmpty = requireNonEmpty,
                ).withMeta()
                else -> {
                    // Non-primitive element type -> ObjectListEntry (auto-inferred)
                    val elemClass = typeArg as? Class<*> ?: return null
                    try { elemClass.getDeclaredConstructor() } catch (_: NoSuchMethodException) { return null }
                    val getListLambda = buildGetListLambda(field, instance)
                    val elementLabel: (Any) -> String = { elem ->
                        elemClass.declaredFields
                            .firstOrNull { f -> !Modifier.isStatic(f.modifiers) && !f.isSynthetic && f.type == String::class.java }
                            ?.also { f -> f.isAccessible = true }
                            ?.get(elem) as? String ?: elem.toString()
                    }
                    return ProcessedEntry.ObjectListEntry(
                        name = name, description = desc,
                        getList = getListLambda,
                        createElement = { elemClass.getDeclaredConstructor().newInstance() },
                        buildElementEntries = { elem -> processFields(elem) },
                        elementLabel = elementLabel,
                    ).withMeta()
                }
            }
        }

        return null
    }

    private fun camelToTitle(name: String): String =
        name.replace(Regex("([A-Z])"), " $1")
            .replaceFirstChar { it.uppercase() }
            .trim()

    /**
     * Resolves a `() -> Boolean` predicate from a [@Conditional][Conditional] annotation on
     * [field]. Returns `{ true }` when the annotation is absent.
     *
     * Used for both entry visibility and category visibility so the logic is identical.
     * Field-based visibility is handled at runtime via [bindVisible] / [bindCategoryWhen].
     */
    private fun resolveConditional(field: Field, @Suppress("UNUSED_PARAMETER") containingInstance: Any): () -> Boolean {
        val ann = field.getAnnotation(Conditional::class.java) ?: return { true }
        @Suppress("UNCHECKED_CAST")
        val inst = instantiate(ann.condition as KClass<VisibilityCondition>)
        return { inst.shouldShow() }
    }

    /**
     * Builds a lambda that always returns the [MutableList] held by [field] on [instance],
     * transparently following [Visible] and [Reference] wrappers and promoting immutable lists in-place.
     */
    private fun buildGetListLambda(field: Field, instance: Any): () -> MutableList<Any> {
        val raw = field.get(instance)
        val vis = raw as? Visible<*>
        val listRef = (vis?.value ?: raw) as? Reference<*>
        val visValueField: Field? = vis?.let {
            Visible::class.java.getDeclaredField("value").also { f -> f.isAccessible = true }
        }
        fun writeBack(mut: MutableList<Any>) {
            @Suppress("UNCHECKED_CAST")
            when {
                listRef != null -> (listRef as Reference<MutableList<Any>>).set(mut)
                vis != null     -> visValueField!!.set(vis, mut)
                else            -> field.set(instance, mut)
            }
        }
        return {
            val current: Any? = when {
                listRef != null -> listRef.get()
                vis != null     -> vis.value
                else            -> field.get(instance)
            }
            @Suppress("UNCHECKED_CAST")
            when (current) {
                is MutableList<*> -> current as MutableList<Any>
                is List<*> -> {
                    val mut = current.toMutableList() as MutableList<Any>
                    writeBack(mut)
                    mut
                }
                else -> {
                    val mut = mutableListOf<Any>()
                    writeBack(mut)
                    mut
                }
            }
        }
    }

    /**
     * Returns the element type of a list field, transparently unwrapping
     * [Visible] and [Reference] so that e.g.
     * `Visible<Reference<MutableList<String>>>` -> `String::class.java`.
     */
    private fun listElementType(field: Field): Type? {
        val gt = field.genericType as? ParameterizedType ?: return null
        val rawClass = gt.rawType as? Class<*> ?: return null
        // Unwrap Visible<T> if present
        val afterVisible: ParameterizedType = if (Visible::class.java.isAssignableFrom(rawClass)) {
            gt.actualTypeArguments.firstOrNull() as? ParameterizedType ?: return null
        } else gt
        val afterVisibleRaw = afterVisible.rawType as? Class<*> ?: return null
        // Unwrap Reference<T> if present
        val listType: ParameterizedType = if (Reference::class.java.isAssignableFrom(afterVisibleRaw)) {
            afterVisible.actualTypeArguments.firstOrNull() as? ParameterizedType ?: return null
        } else afterVisible
        return listType.actualTypeArguments.firstOrNull()
    }

    private fun shouldSkipGui(field: Field): Boolean {
        if (Modifier.isStatic(field.modifiers)) return true
        if (field.isSynthetic) return true
        val excluded = field.getAnnotation(Excluded::class.java)
        return excluded != null && excluded.gui
    }

    private fun <T : Any> instantiate(clazz: KClass<T>): T =
        clazz.objectInstance ?: clazz.java.getDeclaredConstructor().newInstance()

    private fun fields(clazz: Class<*>): List<Field> = buildList {
        var c: Class<*>? = clazz
        while (c != null && c != Any::class.java) {
            addAll(c.declaredFields)
            c = c.superclass
        }
    }.sortedBy { it.getAnnotation(Order::class.java)?.value ?: 0 }
}

// -----------------------------------------------------------------------------
// Runtime binding extensions on ProcessedConfig
// -----------------------------------------------------------------------------

private fun ProcessedConfig.findEntryByName(name: String): ProcessedEntry? {
    fun walk(entries: List<ProcessedEntry>): ProcessedEntry? {
        for (e in entries) {
            if (e is ProcessedEntry.CollapsibleGroup) {
                walk(e.children)?.let { return it }
            } else if (e.fieldName == name || e.name == name) {
                return e
            }
        }
        return null
    }
    for (cat in categories) {
        walk(cat.entries)?.let { return it }
        for (sub in cat.subcategories) walk(sub.entries)?.let { return it }
    }
    return null
}

fun ProcessedConfig.bindVisible(
    entryProp: KProperty0<*>,
    conditionProp: KProperty0<Boolean>,
) {
    val entry = findEntryByName(entryProp.name) ?: return
    entry.visible = { conditionProp.get() }
}

fun ProcessedConfig.bindCategoryWhen(
    categoryProp: KProperty0<*>,
    conditionProp: KProperty0<Boolean>,
) {
    val propName = categoryProp.name
    fun findCategory(cats: List<ProcessedCategory>): ProcessedCategory? {
        for (cat in cats) {
            if (cat.fieldName == propName) return cat
            findCategory(cat.subcategories)?.let { return it }
        }
        return null
    }
    val cat = findCategory(categories) ?: return
    val original = cat.condition
    cat.condition = { original() && conditionProp.get() }
}

fun ProcessedConfig.bindDropdownOptions(
    listProp: KProperty0<MutableList<Int>>,
    optionsProp: KProperty0<MutableList<String>>,
) {
    val entry = findEntryByName(listProp.name) as? ProcessedEntry.MutableListEntry ?: return
    if (entry.elementType != ProcessedEntry.MutableListEntry.ElementType.DROPDOWN) return
    val optField = ProcessedEntry.MutableListEntry::class.java.getDeclaredField("dropdownOptions").also {
        it.isAccessible = true
    }
    val newOpts: (() -> List<String>) = { optionsProp.get() }
    optField.set(entry, newOpts)
}
