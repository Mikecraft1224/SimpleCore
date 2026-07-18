[← Back to README](../README.md)

# Config System

SimpleCore provides an annotation-driven config system. Define a plain Kotlin class with annotated fields - the library infers the widget type from the field type and handles JSON persistence and GUI generation automatically. The GUI supports live updates: dynamic dropdown options, conditional categories, and entry visibility predicates are re-evaluated every render frame.

---

## Quick start

**1. Define a config class**

```kotlin
import com.github.mikecraft1224.simplecore.config.api.annotations.*
import com.github.mikecraft1224.simplecore.config.api.values.*

@Config(title = "My Mod", subtitle = "v1.0")
class MyConfig {
    // @Entry label is optional - omitting it auto-derives from the field name:
    // "enabled" -> "Enabled",  "speedMultiplier" -> "Speed Multiplier", etc.

    @Entry(description = "Turns the feature on or off")
    var enabled = true                              // Boolean -> pill toggle

    @Entry(description = "Movement speed multiplier")
    @Slider(0.5, 5.0, 0.5)
    var speed = 1.0                                 // @Slider -> draggable slider

    @Entry(description = "Operation mode")
    var mode = Mode.NORMAL                          // Enum -> dropdown

    @Entry("Quality", "Render quality preset")
    var quality = Dropdown("Low", "Medium", "High") // Dropdown value type

    @Separator
    @Entry(description = "Players to ignore")
    var blockedPlayers: MutableList<String> = mutableListOf()  // MutableList -> list editor

    @Category("Advanced", "Advanced options")
    var advanced = Visible(this::enabled, AdvancedSettings())
}

enum class Mode { NORMAL, FAST, STEALTH }

class AdvancedSettings {
    @Entry(description = "Player username")
    var username = "Player"                         // String -> text input

    @Entry("Accent color")
    var accentColor = java.awt.Color(0x5865F2)      // Color -> color picker

    @Collapsible
    @DefaultCollapsed
    @Entry("Extra")
    var extra = ExtraSettings()
}

class ExtraSettings {
    @Entry("Reset defaults")
    var resetAction = Button("Reset") { /* reset logic */ }  // Button value type
}
```

**2. Create a manager and load on startup**

```kotlin
val config = MyConfig()
val manager = ConfigManager.of(config, "mymod")  // saves to config/mymod.json

// In your ClientModInitializer:
manager.load()
manager.onReload { println("Config reloaded: enabled=${it.enabled}") }
```

**3. Open the config screen**

```kotlin
// Re-process on every open to pick up changed conditions and live state:
client.setScreenAndShow(ConfigScreen(client.screen, ConfigProcessor.process(config), manager))
```

---

## Type inference

The config processor infers the widget type from the field type. No `@Editor*` annotation is needed.

| Field type | Widget |
|---|---|
| `Boolean` | Pill toggle |
| `String` | Single-line text input |
| `String` + `@Info` | Read-only info label |
| `Enum` | Dropdown (uses `toString()`) |
| `java.awt.Color` | ARGB color picker (HSV spectrum + channel sliders + hex) |
| `Int / Float / Double` + `@Slider(min, max)` | Draggable slider |
| `Dropdown` | Single-selection dropdown |
| `DropdownList` | Per-item dropdown list editor |
| `MultiSelect` | Multi-selection checkbox popup |
| `Keybind` | Keybind capture widget |
| `Button` | Clickable button (not serialized) |
| `MutableList<String / Int / Boolean>` | Primitive list editor (add / remove / reorder) |
| `MutableList<T>` (non-leaf class with no-arg constructor) | Object list editor |
| `Reference<T>` | Widget inferred from T |
| `Visible<T>` | Widget inferred from T, hidden when its condition is false |

---

## Value types

These types carry their own behavior - assign one to a config field and the widget is inferred automatically.

**`Dropdown`** - single selection from a list of options; serialized as the selected index.

```kotlin
// Static options:
var quality = Dropdown("Low", "Medium", "High")

// Dynamic options from an external property (re-evaluated every frame):
var priority = Dropdown(PriorityData::options)
```

**`DropdownList`** - list where each element is a dropdown selection; serialized as a JSON array of indices.

```kotlin
var slots = DropdownList("Low", "Normal", "High")

// Dynamic options:
var slots = DropdownList(PriorityData::options)
```

**`MultiSelect`** - zero-or-more selection; selected labels stored in a `MutableList<String>`.

```kotlin
var modes = MultiSelect("Normal", "Fast", "Stealth")
```

**`Keybind`** - key code + modifier flags packed into a single Int; serialized as that Int.

```kotlin
var hotkey = Keybind(GLFW.GLFW_KEY_INSERT)
var combo  = Keybind.of(GLFW.GLFW_KEY_G, ctrl = true)
var mouseBtn = Keybind.mouse(GLFW.GLFW_MOUSE_BUTTON_4)
```

Convert to a `KeyDescriptor` for use with `KeybindRegistry` (see [Keybind System](keybind-system.md)):

```kotlin
KeyDescriptor.from(config.hotkey)
```

**`Button`** - clickable button with a label and an action lambda; never serialized.

```kotlin
var resetAction = Button("Reset") { speed = 1.0; enabled = true }
```

---

## Annotations reference

Annotations live in two subpackages - use star imports:

```kotlin
import com.github.mikecraft1224.simplecore.config.api.annotations.*  // all annotations + VisibilityCondition
import com.github.mikecraft1224.simplecore.config.api.values.*        // Dropdown, MultiSelect, Keybind, Button, Reference, DropdownList, Visible
```

**Entry and field annotations** (`config.api.annotations`):

| Annotation | Effect |
|---|---|
| `@Entry` | Shows the field in the GUI; label auto-derived from field name |
| `@Entry(name)` | Override the auto-derived label |
| `@Entry(description)` | Tooltip text shown on hover |
| `@Slider(min, max, step?)` | Makes an `Int / Float / Double` field a draggable slider |
| `@Info` | Makes a `String` field a read-only info label |
| `@Category(name, ...)` | Sidebar category or indented subcategory |
| `@Collapsible` | Inline collapsible group within the current page |
| `@DefaultCollapsed` | (with `@Collapsible`) Group starts collapsed |
| `@Separator` | Horizontal divider line above the field |
| `@Separator(label)` | Divider with a centered text label |
| `@SearchTag(vararg aliases)` | Hidden search keywords for the field |
| `@Order(value)` | Display order within the category; lower values first (default 0, stable sort) |
| `@ListEditor(requireNonEmpty)` | For `MutableList` fields - disables the delete button when only one item remains |
| `@Excluded(config, gui)` | `config=true` skips JSON; `gui=true` skips GUI (default when bare) |
| `@Conditional(Cls::class)` | Show/hide via a `VisibilityCondition` class - for conditions not expressible as a single property reference (mod detection, build flags, etc.) |

**Visibility wrapper** (preferred for most cases):

`Visible<T>` wraps any config value with a `KProperty0<Boolean>` condition that is re-evaluated every render frame. This keeps the condition at the field definition site - no annotation or runtime call needed.

| Pattern | When to use |
|---|---|
| `Visible(this::flag, value)` | Condition is a field in the same config class |
| `Visible(SomeObject::prop, value)` | Condition is a property on an external object |
| `@Conditional(MyCondition::class)` | Condition requires logic (mod detection, build flags, etc.) |

---

## Categories and subcategories

Any field annotated with `@Category` on a nested object becomes a sidebar category. Categories on fields inside another `@Category` class become indented subcategories. Subcategories cannot be nested further.

```kotlin
@Config(title = "My Mod")
class MyConfig {
    @Category("Network", "Connection settings")
    var network = NetworkSettings()
}

class NetworkSettings {
    @Entry("Timeout ms")
    @Slider(100.0, 10000.0, 100.0)
    var timeoutMs = 3000

    // Sub-category - shown indented below "Network" in the sidebar
    @Category("Proxy", "Proxy configuration")
    var proxy = ProxySettings()
}
```

Wrap the category value in `Visible` to make it conditionally shown:

```kotlin
@Entry var showAdvanced = false

@Category("Advanced")
var advanced = Visible(this::showAdvanced, AdvancedSettings())
```

---

## Conditional categories

Wrap the category value in `Visible<T>` to tie its visibility to a `Boolean` property. The condition is re-evaluated every render frame.

**Same-class condition:**
```kotlin
@Entry var showAdvanced = false

@Category("Advanced")
var advanced = Visible(this::showAdvanced, AdvancedSettings())
```

**External object condition:**
```kotlin
object FeatureFlags { var betaEnabled = false }

class MyConfig {
    @Category("Beta features")
    var beta = Visible(FeatureFlags::betaEnabled, BetaSettings())
}
```

For conditions not expressible as a single property (mod detection, build flags, etc.), implement `VisibilityCondition`:

<details>
<summary>Complex conditions via VisibilityCondition</summary>

```kotlin
object DebugOnly : VisibilityCondition {
    override fun shouldShow() = BuildConfig.DEBUG
}

object RequiresSodium : VisibilityCondition {
    override fun shouldShow() = FabricLoader.getInstance().isModLoaded("sodium")
}

@Conditional(DebugOnly::class)
@Category("Debug tools")
var debug = DebugSettings()

@Conditional(RequiresSodium::class)
@Category("Sodium options")
var sodium = SodiumSettings()
```

</details>

---

<details>
<summary>Collapsible groups</summary>

`@Collapsible` renders a nested object as an expandable inline group rather than a separate sidebar page. Add `@DefaultCollapsed` to start it collapsed. Only valid on non-primitive custom objects.

```kotlin
class AdvancedSettings {
    @Entry(description = "Fine-grained multiplier")
    @Slider(0.1, 2.0, 0.1)
    var multiplier = 1.0

    @Collapsible
    @DefaultCollapsed
    @Entry("Debug")
    var debug = DebugSettings()
}

class DebugSettings {
    @Entry(description = "Enable verbose logging")
    var debugMode = false
}
```

Collapsible groups can be nested to arbitrary depth. Each level gets a distinct depth-colored header with a left accent bar.

</details>

---

## List entries

`MutableList<String>`, `MutableList<Int>`, and `MutableList<Boolean>` are automatically rendered as list editors with add / remove / drag-to-reorder.

```kotlin
@Entry("Blocked players")
var blockedPlayers: MutableList<String> = mutableListOf()

@Entry("Flag slots")
var flags: MutableList<Boolean> = mutableListOf()

// Prevent deleting the last remaining entry:
@Entry("Servers")
@ListEditor(requireNonEmpty = true)
var servers: MutableList<String> = mutableListOf("localhost")
```

For a list where each item is a dropdown selection, use `DropdownList`:

```kotlin
// Static options - each slot picks one:
@Entry("Priority modes")
var priorityModes = DropdownList("Low", "Normal", "High")

// Dynamic options:
@Entry("Priority slots")
var prioritySlots = DropdownList(PriorityData::options)
```

---

## Object list entries

`MutableList<T>` where `T` is a non-leaf class with a no-arg constructor is automatically rendered as an object list editor. Each object's fields are edited in a sub-dialog that supports all standard entry types.

No `@EditorObjectList` annotation is needed - the type is auto-inferred.

```kotlin
@Entry("Servers", "List of server connection profiles")
var servers: MutableList<Server> = mutableListOf(Server())

class Server {
    @Entry("Host")    var host: String = "localhost"    // String -> text input
    @Entry("Port")    @Slider(1.0, 65535.0, 1.0) var port: Int = 25565
    @Entry("Enabled") var enabled: Boolean = true       // Boolean -> toggle
    @Entry("Priority") var priority = Dropdown("Low", "Normal", "High")

    override fun toString() = host  // used as the list row label
}
```

---

## Reference fields

`Reference<T>` is a transparent two-way proxy. Assign it to a config field to bind it to an external property. The config system reads and writes through the reference - the config class holds no data of its own for that field.

```kotlin
object PriorityData {
    var options: MutableList<String> = mutableListOf("Low", "Normal", "High")
}

@Config(title = "My Mod")
class MyConfig {
    // GUI edits PriorityData.options directly - no data duplication:
    @Entry
    var priorityOptions = Reference(PriorityData::options)

    // Dropdown reads the same live list (resolveFieldSource follows the Reference):
    @Entry
    var priority = Dropdown(PriorityData::options)
}
```

**Serialization:** `Reference` fields are serialized bidirectionally. On save, `ref.get()` is written to the JSON file. On load, the stored value is passed to `ref.set()`, updating the external property directly. Read-only references (created from `KProperty0`) skip the write-back on load.

`Reference<Boolean>` fields can drive `@Conditional` - the processor follows the reference automatically:

```kotlin
object FeatureFlags { var experimentalEnabled = false }

class MyConfig {
    var experimentalEnabled = Reference(FeatureFlags::experimentalEnabled)

    @Conditional("experimentalEnabled")
    @Entry("Experimental threshold")
    @Slider(0.0, 1.0, 0.05)
    var experimentalThreshold = 0.5
}
```

Use `KMutableProperty0` (a `var`) for read-write references, or `KProperty0` (a `val`) for read-only references where the backing collection is mutated in-place:

```kotlin
var opts = Reference(SomeObject::mutableVar)   // KMutableProperty0 - set() writes back
var opts = Reference(SomeObject::valList)      // KProperty0 - set() is a no-op, list mutated in-place
```

---

## Multi-select entries

`MultiSelect` renders a checkbox popup that lets the user pick zero or more options. Selected option **labels** are stored in `MultiSelect.selected`, making the JSON human-readable.

```kotlin
// Static options:
@Entry("Active modes", "Select all that should run simultaneously")
var activeModes = MultiSelect("Normal", "Fast", "Stealth")

// Dynamic options from an external property:
var serverModes = Reference(ServerProfileOptions::connectionModes)
@Entry("Connection modes")
var selectedModes = MultiSelect(ServerProfileOptions::connectionModes)
```

The row widget shows "None", the single selected label, or "N selected". Clicking opens a scrollable popup; clicking outside or pressing ESC closes it. Options added or removed from the source list are reflected immediately.

---

## Runtime entry visibility

Wrap the field value in `Visible<T>` to tie its visibility to a `Boolean` property. The condition is a `KProperty0<Boolean>` - a property reference that is re-evaluated every render frame.

**Same-class condition:**
```kotlin
@Entry(description = "Master toggle")
var enabled = true

@Entry(description = "Speed multiplier")
@Slider(0.5, 5.0, 0.5)
var speed = Visible(this::enabled, 1.0)

// Access the value:
config.speed.value   // 1.0
```

**External object condition:**
```kotlin
object FeatureFlags { var expertMode = false }

@Entry("Expert threshold")
@Slider(0.0, 1.0, 0.05)
var expertThreshold = Visible(FeatureFlags::expertMode, 0.5)
```

`Visible<T>` can also wrap `Reference<T>` for cross-class values with cross-class visibility:
```kotlin
var expertThreshold = Visible(FeatureFlags::expertMode, Reference(SharedData::threshold))
```

<details>
<summary>Programmatic visibility and binding (advanced)</summary>

Use the `bindVisible`, `bindCategoryWhen`, and `bindDropdownOptions` extension functions on `ProcessedConfig` for type-safe programmatic wiring after `process()` returns:

```kotlin
val model = ConfigProcessor.process(config)

// Show/hide an entry based on a Boolean property reference:
model.bindVisible(config::speed, config::enabled)

// Show/hide a whole category:
model.bindCategoryWhen(config::advanced, config::enabled)

// Point a dropdown-list entry's options at a live MutableList<String>:
model.bindDropdownOptions(config::priorityList, config::priorityOptions)
```

All three take `KProperty0` references - rename-safe and IDE-navigable. The predicates and option lists are re-evaluated every render frame.

For lower-level access, every `ProcessedEntry` exposes `visible: () -> Boolean` directly:

```kotlin
model.categories
    .flatMap { it.entries }
    .filterIsInstance<ProcessedEntry.SliderEntry>()
    .first { it.name == "Speed" }
    .also { it.visible = { config.enabled } }
```

</details>

---

<details>
<summary>Programmatic model construction</summary>

You can inject additional entries into an annotation-driven model or build categories entirely by hand. This is useful for entries whose structure is only known at runtime.

```kotlin
fun buildModel(): ProcessedConfig {
    val base = ConfigProcessor.process(config)

    val addButton = ProcessedEntry.ButtonEntry(
        name        = "Add custom option",
        description = "",
        buttonText  = "Add",
        action      = {
            options.add("Custom ${++counter}")
        },
    )

    val listEntry = ProcessedEntry.MutableListEntry(
        name            = "Selections",
        description     = "Each item picks from the live option list",
        elementType     = ProcessedEntry.MutableListEntry.ElementType.DROPDOWN,
        defaultElement  = 0,
        getList         = { selections as MutableList<Any> },
        dropdownOptions = { options },
    )

    val genIdx = base.categories.indexOfFirst { it.name == "General" }
    val cats   = base.categories.toMutableList()
    cats[genIdx] = cats[genIdx].let { it.copy(entries = it.entries + addButton + listEntry) }
    return base.copy(categories = cats)
}

// Re-build on every open so conditions and options are current:
onPress = { client ->
    client.setScreenAndShow(ConfigScreen(null, buildModel(), manager))
}
```

</details>

---

<details>
<summary>Persistence</summary>

`ConfigManager` uses Gson to serialize config fields to a pretty-printed JSON file.

- On `save()`: writes to a `.tmp` file, validates it parses cleanly, then atomically replaces the target - no data loss on interrupted writes.
- On `load()`: merges values in-place into the existing config instance - missing keys keep defaults, unknown keys are ignored.
- `reload()` = `load()` + notifies `onReload` listeners.
- `Button` fields and `@Excluded(config=true)` fields are never saved.
- `Reference` fields are serialized bidirectionally - on load, the stored value is written back to the referenced external property.

**Migrations:** register version-based migration steps that run automatically on load when the saved `_version` is older than the current schema:

```kotlin
val manager = ConfigManager.of(MyConfig(), "mymod")
    .migration(fromVersion = 0) {
        rename("oldName", "newName")
        remove("removedField")
    }
    .migration(fromVersion = 1) {
        nested("advanced") { rename("speedOld", "speed") }
    }
manager.load()
```

</details>

<details>
<summary>Reactive config values (Property&lt;T&gt;)</summary>

Wrap a field in `Property<T>` instead of a bare value to get per-field change notifications - useful when other code needs to react to one specific setting changing, without a full-config `onReload` listener:

```kotlin
@Entry("Speed multiplier")
@Slider(0.5, 3.0, 0.1)
var speed = Property(1.0)

// Somewhere in init:
config.speed.onChange { old, new -> motionModule.setSpeedMultiplier(new) }

// Access/assign the wrapped value:
config.speed.value = 2.0   // fires the listener above
```

The widget type is inferred from `Property`'s wrapped value exactly as it would be for a bare field. Listeners fire on direct assignment **and** when the value changes via `ConfigManager.reload()` (deserialization writes through `property.value =`, not by replacing the `Property` instance) - so `Property` stays valid to hold onto across reloads.

</details>

<details>
<summary>Error handling</summary>

```kotlin
val manager = ConfigManager.of(MyConfig(), "mymod")
    .onLoadFailed { e ->
        // called if the config file is corrupt or unreadable
        // config keeps its default values
    }
    .onSaveFailed { e ->
        // called if the atomic write fails (e.g. disk full)
    }
```

</details>

<details>
<summary>Runtime reloading</summary>

```kotlin
manager.onReload { updatedConfig ->
    // called every time reload() is invoked (e.g., after the player edits the file)
}
manager.reload()
```

</details>

<details>
<summary>@Config annotation options</summary>

`@Config` on the config class controls the generated screen:

```kotlin
@Config(
    title = "My Mod",
    subtitle = "v1.0",
    accentColor = 0xFFA6E3A1.toInt(), // override default blue; 0 = use default
    searchEnabled = false,             // hide the search bar
    defaultCategory = "Advanced",     // open this category by default
    autoFocusSearch = true,           // focus the search bar automatically on open
)
class MyConfig { ... }
```

| Parameter | Default | Effect |
|---|---|---|
| `title` | `""` | Header title (falls back to screen constructor arg) |
| `subtitle` | `""` | Second line below title (hidden when empty) |
| `accentColor` | `0` (blue) | ARGB highlight color for selected items, slider fill, etc. |
| `searchEnabled` | `true` | Shows or hides the search bar |
| `defaultCategory` | `""` (first) | Category name to open on screen load |
| `autoFocusSearch` | `false` | Focuses the search bar automatically when the screen opens |

</details>
