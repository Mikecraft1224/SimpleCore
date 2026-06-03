# SimpleCore

Lightweight Fabric client-side mod library.

## Table of Contents

- [Modules](#modules)
- [Event Bus](#event-bus)
  - [Using the Global Bus](#using-the-global-bus)
    - [1. Opt in](#1-opt-in)
    - [2. Write handlers](#2-write-handlers)
    - [3. Post events](#3-post-events)
    - [Handler options](#handler-options)
  - [Creating Your Own Bus](#creating-your-own-bus)
- [Keybind System](#keybind-system)
  - [Registering keybinds](#registering-keybinds)
  - [Key descriptors](#key-descriptors)
  - [Contexts and modifiers](#contexts-and-modifiers)
  - [Callbacks](#callbacks)
  - [KeybindHandle - lifecycle control](#keybindhandle--lifecycle-control)
  - [Blocking](#blocking)
- [Config System](#config-system)
  - [Quick start](#quick-start)
  - [Type inference](#type-inference)
  - [Value types](#value-types)
  - [Annotations reference](#annotations-reference)
  - [Categories and subcategories](#categories-and-subcategories)
  - [Conditional categories](#conditional-categories)
  - [Collapsible groups](#collapsible-groups)
  - [List entries](#list-entries)
  - [Object list entries](#object-list-entries)
  - [Reference fields](#reference-fields)
  - [Multi-select entries](#multi-select-entries)
  - [Runtime entry visibility](#runtime-entry-visibility)
  - [Programmatic model construction](#programmatic-model-construction)
  - [Persistence](#persistence)
  - [Error handling](#error-handling)
  - [Runtime reloading](#runtime-reloading)
  - [@Config annotation options](#config-annotation-options)
- [UI Library](#ui-library)
  - [Quick start](#ui-quick-start)
  - [Widgets](#widgets)
  - [Layouts](#layouts)
  - [Theming](#theming)
- [Command System](#command-system)
  - [Registering a command](#registering-a-command)
  - [Aliases](#aliases)
  - [Tab completion](#tab-completion)
  - [Permission guards](#permission-guards)
  - [CommandHandle](#commandhandle--runtime-enabledisable)
  - [Splitting across files](#splitting-across-files)
  - [Argument types reference](#argument-types-reference)
- [Overlay System](#overlay-system)
  - [Quick start](#overlay-quick-start)
  - [HudRenderable types](#hudrenderable-types)
  - [HudElement options](#hudelement-options)
  - [OverlayPosition](#overlayposition)
  - [Overlay editor](#overlay-editor)
- [Render System](#render-system)
- [Utils](#utils)
  - [World rendering](#world-rendering)
  - [HUD rendering](#hud-rendering)
- [Build](#build)
- [Contributing](#contributing)
- [License](#license)

---

## Modules

| Module | Status |
|---|---|
| Event Bus | Done |
| Keybind System | Done |
| Config System | Done |
| Command System | Done |
| Overlay System | Done |
| Render System | Done |
| UI Library | In Progress |
| Utils | Done |
| Events | Done |

---

## Event Bus

SimpleCore provides a global, thread-safe event bus that any mod can subscribe to. Most mods only need three things: opt in, write handlers, and post events.

For advanced use cases - running your own isolated bus, custom exception policies, handler introspection - see [Creating Your Own Bus](#creating-your-own-bus).

---

### Using the Global Bus

#### 1. Opt in

Add a `ScanEntrypoint` to your `fabric.mod.json` to tell SimpleCore which packages to scan for your handlers:

```json
{
  "entrypoints": {
    "simplecore:feature_scan": [
      {
        "value": "com.example.mymod.SimpleCoreEntrypoint",
        "adapter": "kotlin"
      }
    ]
  }
}
```

```kotlin
class SimpleCoreEntrypoint : ScanEntrypoint {
    override fun scanRequest() = FeatureScanRequest(
        packages = listOf("com.example.mymod.features")
    )
}
```

To exclude internal sub-packages from the scan:

```kotlin
FeatureScanRequest(
    packages = listOf("com.example.mymod.features"),
    rejectedPackages = listOf("com.example.mymod.features.internal")
)
```

---

#### 2. Write handlers

Annotate a class or object with `@Feature` and its handler methods with `@Subscribe`:

```kotlin
@Feature
object CombatFeature {
    @Subscribe
    fun onTick(event: ClientTickEvent) {
        // runs every client tick
    }
}
```

`FeatureAutoLoader` discovers and registers all `@Feature` classes in the scanned packages automatically at startup. Classes must have a no-arg constructor or be Kotlin `object`s.

---

#### 3. Post events

To fire an event to all mods subscribed to the global bus:

```kotlin
EventRegistry.post { MyEvent(data) }
```

Only buses that have at least one handler registered for `MyEvent` receive it. Cancellation state is isolated per bus - one mod cancelling an event does not affect another mod's copy.

---

#### Handler options

<details>
<summary>Priority - control execution order</summary>

Available priorities, highest first: `HIGHEST`, `HIGH`, `NORMAL` (default), `LOW`, `LOWEST`.

```kotlin
@Subscribe(priority = EventPriority.HIGH)
fun onHighPriority(event: ClientTickEvent) { /* runs before NORMAL handlers */ }
```

**Cancellation semantics:** the cancelled state is snapshotted once per priority level. Handlers in the same priority bucket see the same cancelled state - they cannot observe each other's cancellations, only those from higher-priority buckets.

```kotlin
@Subscribe(receiveCancelled = true)
fun alwaysRuns(event: MyEvent) { /* runs even if a higher-priority handler cancelled it */ }
```

</details>

<details>
<summary>Inline filters - skip invocation without guard code in the handler</summary>

```kotlin
object EndPhaseOnly : EventFilter<ClientTickEvent> {
    override fun test(event: ClientTickEvent) =
        event.phase == ClientTickEvent.Phase.END
}

@Subscribe(filter = EndPhaseOnly::class)
fun onEndTick(event: ClientTickEvent) {
    // only called on END phase - no if-check needed here
}
```

Filters must be Kotlin `object`s or have a no-arg constructor. The default `NoFilter` adds zero overhead.

</details>

<details>
<summary>Conditional loading - skip a feature based on environment or mod presence</summary>

```kotlin
// Only load on a physical client (not a dedicated server):
@Feature
@ConditionalFeature(PhysicalClientCondition::class)
object HudFeature {
    @Subscribe
    fun onTick(event: ClientTickEvent) { /* ... */ }
}

// Only load when another mod is present:
class RequiresSodium : ModPresentCondition() {
    override val modId = "sodium"
}

@Feature
@ConditionalFeature(RequiresSodium::class)
object SodiumIntegration { /* ... */ }
```

Built-in conditions: `PhysicalClientCondition`, `ModPresentCondition` (abstract, override `modId`).

Custom condition:

```kotlin
object MyCondition : FeatureCondition {
    override fun shouldLoad(): Boolean = true // any check available at mod init time
}
```

Conditions are evaluated once at scan time. They must not depend on world or player state.

</details>

<details>
<summary>Polymorphic dispatch - subscribe to a base type</summary>

```kotlin
// Fires for every Event subtype posted to the bus:
@Subscribe(polymorphic = true)
fun onAnyEvent(event: Event) { /* ... */ }

// Fires for CancellableEvent and all its subtypes:
@Subscribe(polymorphic = true)
fun onAnyCancellable(event: CancellableEvent) { /* ... */ }
```

Polymorphic handlers are stored at registration time - no extra cost per dispatch.

</details>

---

### Creating Your Own Bus

For most mods, the global bus is all you need. If you need an isolated bus - for example, to build a plugin system within your own mod, or to run your own `ScanEntrypoint` key - you can instantiate and manage an `EventBus` directly.

The relevant files are documented with KDoc:

| File | Purpose |
|---|---|
| `EventBus.kt` | Core bus: registration, dispatch, introspection, debug mode |
| `FeatureAutoLoader.kt` | Classpath scanning and bus wiring |
| `EventRegistry.kt` | Global event-class -> bus mapping |
| `bus/api/Event.kt` | `Event` and `CancellableEvent` base classes |
| `bus/api/EventCompanion.kt` | Lazy Fabric hook registration pattern |
| `bus/api/EventExceptionHandler.kt` | Pluggable exception handling |
| `bus/api/RegistrationHandle.kt` | Per-handler selective unregistration |

<details>
<summary>Minimal own-bus setup</summary>

```kotlin
// Create a bus with a custom exception handler:
val bus = EventBus(exceptionHandler = RethrowEventExceptionHandler())

// Scan your own packages and wire it into the global registry:
FeatureAutoLoader.scanAndRegister(bus, listOf("com.example.mymod.features"))

// Or accept opt-in entrypoints from other mods under your own key:
FeatureAutoLoader.loadOptInPackages(bus, entrypointKey = "mymod:feature_scan")
```

Custom event classes register their Fabric hooks lazily via `EventCompanion` - see `ClientTickEvent.kt` for a complete example.

</details>

<details>
<summary>Selective unregistration and introspection</summary>

```kotlin
// Get per-handler removal tokens:
val handles: List<RegistrationHandle> = bus.registerFeatureWithHandles(myFeature)
handles[0].unregister()           // remove one handler
handles[0].isRegistered           // false after unregister

// Inspect the handler list for an event type:
bus.getHandlerSummary(ClientTickEvent::class).forEach { h ->
    println("${h.ownerClass}::${h.methodName} [${h.priority}]")
}

// Enable per-dispatch debug logging and the post record ring buffer:
bus.debugMode = true
EventBusMonitor.getRecent()       // List<EventBusMonitor.PostRecord>
EventBusMonitor.setCapacity(512)  // default 256
```

</details>

---

## Keybind System

`KeybindRegistry` manages both vanilla (Minecraft options screen) and virtual (runtime-only) keybinds. Both registration methods return a `KeybindHandle` for lifecycle control.

---

### Registering keybinds

**Vanilla keybind** - appears in Minecraft's controls screen, player-remappable:

```kotlin
val handle: KeybindHandle = KeybindRegistry.registerVanilla(
    id = "key.mymod.sprint",          // must follow key.<modid>.<action> convention
    category = KeyBinding.Category.MOVEMENT,
    defaultKey = KeyDescriptor.keyboard(GLFW.GLFW_KEY_LEFT_ALT),
    context = KeyContext.IN_GAME,
    onPress = { client -> /* ... */ },
    onRelease = { client -> /* ... */ },
)
```

**Virtual keybind** - not shown in options screen, fully runtime-managed:

```kotlin
val handle: KeybindHandle = KeybindRegistry.registerVirtual(
    id = "mymod.zoom",
    key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_C, Modifiers(ctrl = true)),
    context = KeyContext.IN_GAME,
    onPress = { client -> /* ... */ },
)
```

Rebind a virtual keybind at runtime (e.g., after loading config):

```kotlin
KeybindRegistry.updateVirtualKeybind("mymod.zoom", KeyDescriptor.keyboard(GLFW.GLFW_KEY_Z))
```

---

### Key descriptors

`KeyDescriptor` wraps an `InputUtil.Key` (Minecraft's own key type) and optional modifiers:

```kotlin
KeyDescriptor.keyboard(GLFW.GLFW_KEY_F)                          // plain keyboard key
KeyDescriptor.keyboard(GLFW.GLFW_KEY_F, Modifiers(shift = true)) // Shift+F
KeyDescriptor.mouse(GLFW.GLFW_MOUSE_BUTTON_4)                    // mouse side button
KeyDescriptor()                                                   // unbound (UNKNOWN_KEY)
```

`Modifiers` supports `ctrl`, `shift`, and `alt` booleans.

---

### Contexts and modifiers

`context` accepts one or more `KeyContext` values as a `vararg`. Passing nothing defaults to `ANY`.

| Context | Fires when |
|---|---|
| `ANY` | Always (default) |
| `IN_GAME` | No screen open |
| `IN_ANY_SCREEN` | Any non-chat screen is open, including inventory/handled screens |
| `IN_CHAT` | Chat screen is open |
| `IN_HANDLED_SCREEN` | Inventory, chest, crafting table, etc. (strict subset of `IN_ANY_SCREEN`) |

```kotlin
// Single context - pass positionally after the required params
KeybindRegistry.registerVirtual(id = "mymod.zoom", key = myKey, KeyContext.IN_GAME, onPress = { /* ... */ })

// Multiple contexts - multiple positional vararg values
KeybindRegistry.registerVirtual(id = "mymod.zoom", key = myKey, KeyContext.IN_GAME, KeyContext.IN_ANY_SCREEN, onPress = { /* ... */ })

// No context arg - defaults to ANY
KeybindRegistry.registerVirtual(id = "mymod.zoom", key = myKey, onPress = { /* ... */ })
```

---

### Callbacks

| Callback | Signature | When called |
|---|---|---|
| `onPress` | `(MinecraftClient) -> Unit` | Leading edge - key goes down |
| `onRelease` | `(MinecraftClient) -> Unit` | Trailing edge - key comes up |
| `onHold` | `(MinecraftClient, Int) -> Unit` | Every `holdEveryTicks` ticks while held; second arg is hold tick count |
| `onHandledScreen` | `(MinecraftClient, Slot) -> Unit` | Immediate key-press inside a handled screen; second arg is the hovered slot |

All callbacks are optional and default to no-ops.

---

### KeybindHandle - lifecycle control

<details>
<summary>Unregister, block, and inspect keybinds at runtime</summary>

```kotlin
val handle = KeybindRegistry.registerVirtual(
    id = "mymod.zoom",
    key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_C),
)

// Remove from dispatch permanently (releases if currently pressed):
handle.unregister()
handle.isRegistered  // false after unregister

// Temporarily suppress without unregistering:
handle.block()
handle.unblock()
```

> **Note:** Unregistering a vanilla keybind removes it from SimpleCore's dispatch but does not remove it from Minecraft's keybinding options screen - Fabric provides no API for that.

</details>

---

### Blocking

<details>
<summary>Global and context-level input suppression</summary>

```kotlin
// Pause all keybind processing (releases all pressed keys):
KeybindRegistry.blockKeybind()
KeybindRegistry.unblockKeybind()

// Suppress specific contexts (e.g., during a cutscene):
KeybindRegistry.blockContext(KeyContext.IN_GAME)
KeybindRegistry.unblockContext(KeyContext.IN_GAME)

// Per-keybind suppression via the handle:
handle.block()
handle.unblock()
```

</details>

---

## Config System

SimpleCore provides an annotation-driven config system. Define a plain Kotlin class with annotated fields - the library infers the widget type from the field type and handles JSON persistence and GUI generation automatically. The GUI supports live updates: dynamic dropdown options, conditional categories, and entry visibility predicates are re-evaluated every render frame.

---

### Quick start

**1. Define a config class**

```kotlin
import com.github.mikecraft1224.config.api.annotations.*
import com.github.mikecraft1224.config.api.values.*

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
client.setScreen(ConfigScreen(client.currentScreen, ConfigProcessor.process(config), manager))
```

---

### Type inference

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

---

### Value types

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
```

**`Button`** - clickable button with a label and an action lambda; never serialized.

```kotlin
var resetAction = Button("Reset") { speed = 1.0; enabled = true }
```

---

### Annotations reference

Annotations live in two subpackages - use star imports:

```kotlin
import com.github.mikecraft1224.config.api.annotations.*  // all annotations + VisibilityCondition
import com.github.mikecraft1224.config.api.values.*        // Dropdown, MultiSelect, Keybind, Button, Reference, DropdownList
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

### Categories and subcategories

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

### Conditional categories

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

`@Collapsible` renders a nested object as an expandable inline group rather than a separate sidebar page. Add `@DefaultCollapsed` to start it collapsed.

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

### List entries

`MutableList<String>`, `MutableList<Int>`, and `MutableList<Boolean>` are automatically rendered as list editors with add / remove / drag-to-reorder.

```kotlin
@Entry("Blocked players")
var blockedPlayers: MutableList<String> = mutableListOf()

@Entry("Flag slots")
var flags: MutableList<Boolean> = mutableListOf()
```

For a list where each item is a dropdown selection, use `DropdownList`:

```kotlin
// Static options - each slot picks one:
@Entry("Priority modes")
var priorityModes = DropdownList("Low", "Normal", "High")

// Dynamic options from an external property:
@Entry("Priority slots")
var prioritySlots = DropdownList(PriorityData::options)
```

---

### Object list entries

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

### Reference fields

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

### Multi-select entries

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

### Runtime entry visibility

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
    client.setScreen(ConfigScreen(null, buildModel(), manager))
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

</details>

---

## UI Library

SimpleCore provides a lightweight widget/layout framework for building custom Minecraft GUI screens.

---

### UI Quick Start

```kotlin
class MyScreen : UiScreen(Text.literal("My Screen")) {
    override fun buildRoot(): Panel = FrameLayout().also { frame ->
        frame.add(Label("Hello", UiTheme.DEFAULT), relX = 10, relY = 10, w = 100, h = 20)
        frame.add(Button("Close", UiTheme.DEFAULT) {
            MinecraftClient.getInstance().setScreen(null)
        }, relX = 10, relY = 36, w = 80, h = 20)
    }
}

// Open it:
MinecraftClient.getInstance().setScreen(MyScreen())
```

See `ExampleScreen` in `com.github.mikecraft1224.ui.examples` for a full demo of all widgets and layouts.

---

### Widgets

| Widget | Description |
|---|---|
| `Button(label, theme, onClick)` | Filled rect with centered label |
| `Toggle(theme, isOn, onChanged)` | Pill toggle switch (34x14 px knob) |
| `TextField(theme, placeholder, onChanged)` | Wraps `TextFieldWidget` |
| `Label(text, theme, color, shadow)` | Non-interactive text; open class |
| `Image(texture, u, v, regionW, regionH, texW, texH)` | Renders a GUI texture |
| `Divider(theme, label, vertical)` | Horizontal or vertical line with optional centered label |

---

### Layouts

| Layout | Description |
|---|---|
| `HSplit(ratio)` | Two children: left takes `ratio` fraction of width |
| `VSplit(ratio)` | Two children: top takes `ratio` fraction of height |
| `LinearLayout(direction, spacing)` | Row or column stack; `add(widget, prefW, prefH)` |
| `FrameLayout()` | Absolute placement: `add(widget, relX, relY, w, h)` |

---

### Theming

All widgets accept a `UiTheme` instance. Pass a custom theme to override individual colors:

```kotlin
val theme = UiTheme(
    accent  = 0xFFA6E3A1.toInt(), // green instead of blue
    surface0 = 0xFF2A2A3C.toInt(),
)
val btn = Button("OK", theme) { /* ... */ }
```

Defaults are Catppuccin Mocha. `UiTheme.DEFAULT` is the pre-built default instance.

---

## Command System

SimpleCore provides a Kotlin DSL for registering client-side chat commands that wraps Brigadier's verbose builder API. Commands are defined with a tree of `literal` and `argument` nodes, automatically appear in tab completion, and return a `CommandHandle` for runtime enable/disable.

---

### Registering a command

```kotlin
CommandRegistry.client("mymod") {
    executes { sendFeedback("Hello from /mymod!") }

    literal("reload") {
        executes {
            manager.reload()
            sendFeedback("Config reloaded.")
        }
    }

    literal("give") {
        argument("item", word()) {
            argument("count", integer(1, 64)) {
                executes {
                    sendFeedback("Giving ${int("count")}x ${string("item")}")
                }
            }
        }
    }
}
```

Import argument factory functions from `com.github.mikecraft1224.command.api`:
`integer()`, `longArg()`, `floatArg()`, `doubleArg()`, `bool()`, `word()`, `string()`, `greedyString()`, `identifier()`, `color()`.

Retrieve argument values in `executes {}` using the matching accessor on the `ClientCommandContext` receiver: `int(name)`, `string(name)`, `bool(name)`, `identifier(name)`, etc.

For argument types not covered by the built-in accessors:
```kotlin
argument("nbt", NbtCompoundArgumentType.nbtCompound()) {
    executes {
        val tag = get<NbtCompound>("nbt")   // generic reified getter
    }
}
```

---

### Aliases

```kotlin
val handle = CommandRegistry.client("simplecore", "sc") {
    executes { sendFeedback("SimpleCore ${BuildConfig.MOD_VERSION}") }
}
```

All aliases share the same `CommandHandle`.

---

### Tab completion

Literal subcommand names are automatically suggested by Brigadier. Override suggestions for argument nodes with `suggests {}`:

```kotlin
argument("mode", word()) {
    suggests { _ ->
        listOf("fast", "normal", "slow").forEach { suggest(it) }
    }
    executes { sendFeedback("Mode: ${string("mode")}") }
}
```

The block receives `SuggestionsBuilder` as receiver and the `CommandContext` as a parameter. Brigadier filters by the prefix the user has already typed - `suggest()` everything, Brigadier does the filtering.

---

### Permission guards

`requires {}` is evaluated for both tab completion and execution. Nodes whose `requires` returns `false` are hidden from the suggestion list entirely:

```kotlin
literal("admin") {
    requires { source -> source.player?.hasPermissionLevel(4) == true }
    executes { sendFeedback("Admin only.") }
}
```

---

### CommandHandle - runtime enable/disable

```kotlin
val handle = CommandRegistry.client("mymod") { ... }

handle.disable()  // hides from tab completion and blocks execution
handle.enable()   // restores
handle.isEnabled  // current state
```

Because `disable()` works via Brigadier's `requires`, disabled commands vanish from tab completion, not just execution.

---

### Splitting across files

Use `SubCommand` to define each subcommand group in its own file and wire them together at registration time:

```kotlin
// GiveCommand.kt
object GiveCommand : SubCommand {
    override fun CommandBuilder.register() {
        literal("give") {
            argument("item", word()) {
                executes { sendFeedback("Giving ${string("item")}") }
            }
        }
    }
}

// AdminCommand.kt - nested SubCommands work too
object AdminCommand : SubCommand {
    override fun CommandBuilder.register() {
        literal("admin") {
            requires { source -> source.player?.hasPermissionLevel(4) == true }
            install(AdminBanCommand)
            install(AdminKickCommand)
        }
    }
}

// Registration file
CommandRegistry.client("mymod") {
    install(GiveCommand)
    install(TeleportCommand)
    install(AdminCommand)
    // ... no limit on depth or width
}
```

There is no limit on how deep or wide the tree can be. `install()` is available at every level.

---

### Failing with a user-visible error

`fail(message)` throws a `CommandSyntaxException` which Brigadier shows to the player as a system message and halts execution:

```kotlin
executes {
    val count = int("count")
    if (count > 64) fail("Maximum count is 64.")
    // ...
}
```

Use `sendError(message)` instead when you want to report an error without aborting.

---

### Argument types reference

| Factory | Getter | Notes |
|---|---|---|
| `integer(min?, max?)` | `int(name)` | |
| `longArg(min?, max?)` | `long(name)` | |
| `floatArg(min?, max?)` | `float(name)` | |
| `doubleArg(min?, max?)` | `double(name)` | |
| `bool()` | `bool(name)` | Suggests `true` / `false` |
| `word()` | `string(name)` | Single word, no spaces |
| `string()` | `string(name)` | Quoted or single word |
| `greedyString()` | `string(name)` | Consumes rest of input; must be last |
| `identifier()` | `identifier(name)` | `namespace:path` resource location |
| `color()` | `color(name)` | Minecraft `Formatting` color name |
| Any `ArgumentType<T>` | `get<T>(name)` | Generic fallback for custom types |

---

## Overlay System

SimpleCore provides a HUD overlay framework for rendering repositionable panels on the in-game screen. The primary API is `HudElement` with a `buildContent()` method that returns composable `HudRenderable` elements. The framework handles layout, background, hover detection, click routing, tooltip rendering, and editor integration automatically.

---

<a name="overlay-quick-start"></a>

### Quick start

```kotlin
object MyHud : HudElement("My HUD", OverlayPosition(10f, 10f)) {

    override fun isEnabled() = McUtils.isInGame

    override fun buildContent() = listOf(
        HudRenderable.text("§e§lMy Tracker"),
        HudRenderable.hoverable("§7Profit: §a1,234,567", tooltip = listOf("§7This session")),
        HudRenderable.selector("Mode", { mode }, listOf("Coins", "Items")) { mode = it },
        HudRenderable.clickable("§c[Reset]", tooltip = listOf("§7Resets all data")) { reset() },
    )
}
```

Register in your loader:

```kotlin
HudManager.register(MyHud)
```

Open the overlay editor from a keybind:

```kotlin
KeybindRegistry.registerVirtual(
    id = "mymod.overlay_editor",
    key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_O),
    KeyContext.IN_GAME,
    onPress = { _ -> HudManager.openEditor { myConfig.save() } },
)
```

---

### HudRenderable types

`buildContent()` returns a `List<HudRenderable>`. The framework stacks that list vertically. Wrap elements in `horizontal` to place them side by side.

| Factory | Description |
|---|---|
| `text(text, color?, shadow?)` | Plain text. Supports Minecraft format codes (`§x`). |
| `hoverable(text, color?, tooltip, shadow?)` | Text that shows a tooltip list on hover. |
| `clickable(text, color?, tooltip?, shadow?, onClick)` | Text with underline-on-hover and a click callback. `onClick` receives the GLFW button code (0=left, 1=right). |
| `selector(label, current, options, onChange)` | `§7Label §a[§eValue§a]` — left-click cycles forward, right-click backward. |
| `horizontal(children, spacing?)` | Place children side by side. |
| `vertical(children, spacing?)` | Stack children vertically (used by framework automatically). |
| `spacer(height)` | Invisible vertical gap. |
| `custom(width, height, draw)` | Escape hatch for custom drawing. |

```kotlin
// Side-by-side label + value
HudRenderable.horizontal(listOf(
    HudRenderable.text("§7Speed: "),
    HudRenderable.text("§a${speed} m/s"),
))

// Progress bar via custom()
HudRenderable.custom(80, 6) { ctx, lx, ly ->
    ctx.fill(lx, ly, lx + 80, ly + 6, 0xFF333333.toInt())
    ctx.fill(lx, ly, lx + (80 * fraction).toInt(), ly + 6, 0xFF55FF55.toInt())
}
```

---

### HudElement options

```kotlin
object MyHud : HudElement("Display Name", position) {
    override fun isEnabled(): Boolean = true     // false = skip rendering + hide from editor
    override val showBackground: Boolean = true  // dark background panel
    override val linePadding: Int = 5            // pixels of padding around content
    override val lineSpacing: Int = 2            // pixels between top-level elements

    override fun buildContent(): List<HudRenderable> = listOf(...)

    // Override to handle a click on the overlay before element-level routing:
    override fun mouseClicked(mx: Int, my: Int, button: Int): Boolean = false
}
```

`buildContent()` is called once per frame and the result is cached. If a click arrives in the same frame the cached result is reused — it is safe to do moderate work here.

---

### OverlayPosition

```kotlin
class OverlayPosition(
    var x: Float = 10f,   // pixels from left edge
    var y: Float = 10f,   // pixels from top edge
    var scale: Float = 1f // uniform scale multiplier (0.5 – 3.0 in the editor)
)
```

Store in your `@Config` class to persist positions across sessions:

```kotlin
@Config("My Mod")
object MyConfig {
    var hudPosition = OverlayPosition(10f, 10f)
}

object MyHud : HudElement("My HUD", MyConfig.hudPosition) { ... }
```

`OverlayPosition` is a plain class. GSON serializes it automatically when stored in a `@Config` class — no extra annotation needed.

---

### Overlay editor

```kotlin
HudManager.openEditor()               // open; no callback
HudManager.openEditor { save() }      // open; call save() when the screen closes
```

| Control | Action |
|---|---|
| **Drag** | Reposition the overlay |
| **Arrow keys** | Nudge selected overlay by 1 px |
| **Shift + Arrow keys** | Nudge by 10 px |
| **Scroll wheel** | Adjust scale |
| **Escape** | Close |

Changes apply immediately to the `OverlayPosition`. Pass an `onClose` lambda to persist them.

Only overlays whose `isEnabled()` returned `true` in the last frame appear in the editor.

---

<details>
<summary>Advanced: raw renderAt</summary>

For cases where `HudElement` is too constraining, use `OverlayPosition.renderAt` directly and subscribe to `RenderHudEvent` yourself:

```kotlin
position.renderAt(event.ctx, event.screenWidth, event.screenHeight, "My HUD") { ctx ->
    // DrawContext already translated to the overlay's position + scale
    ctx.fill(0, 0, 80, 20, 0xAA000000.toInt())
    ctx.drawText(textRenderer, "Hello", 4, 6, 0xFFFFFF, true)
    OverlaySize(80, 20)  // must match what was drawn
}
```

The block must return `OverlaySize(width, height)` so the editor can draw a drag handle.

</details>

---

## Render System

SimpleCore provides two sets of rendering utilities: **world-space** helpers (3D shapes, lines, text drawn in the world) and **HUD** helpers (2D elements drawn on the screen).

---

### World rendering

Subscribe to `RenderWorldEvent` and call the extension functions from `WorldRenderUtils.kt` and `WorldRenderShapes.kt`. Draw calls are enqueued by priority and flushed in order after all handlers have run.

```kotlin
@Feature
object WorldRenderer {
    @Subscribe
    fun onRender(event: RenderWorldEvent) {
        if (!McUtils.isInGame) return

        // Outlined box around a block position
        val box = Box(10.0, 64.0, 10.0, 11.0, 65.0, 11.0)
        event.drawOutlinedBox(box, Color(255, 0, 0, 200))

        // Tracer line from the camera eye to a target
        event.drawTracer(Vec3d(100.0, 64.0, 100.0), Color.WHITE)

        // Waypoint marker with a label
        event.drawWaypoint(Vec3d(50.0, 64.0, 50.0), "Target", Color(0, 255, 100, 255))
    }
}
```

Register the event and feature in your mod initializer:

```kotlin
EventRegistry.addBus(RenderWorldEvent::class, SimpleCore.EVENTBUS)
RenderWorldEvent.registerEvents()
SimpleCore.EVENTBUS.registerFeature(WorldRenderer)
```

#### Draw priority

Geometry is queued and drawn in priority order. Lower values draw first (underneath higher values):

| Constant | Value | Use for |
|---|---|---|
| `RenderWorldEvent.PRIORITY_WORLD` | 0 | Filled/outlined boxes, circles |
| `RenderWorldEvent.PRIORITY_LINE` | 100 | 3D lines, tracers |
| `RenderWorldEvent.PRIORITY_TEXT` | 200 | World-space text labels |

Pass a custom `priority` parameter to any draw function to override its default:

```kotlin
event.draw3DLine(from, to, Color.WHITE, priority = RenderWorldEvent.PRIORITY_WORLD)
```

#### WorldRenderUtils reference

| Function | Description |
|---|---|
| `drawBox(box, color, style, seeThroughBlocks)` | Shorthand combining `FILLED`, `OUTLINED`, or `BOTH` |
| `drawFilledBox(box, color, seeThroughBlocks)` | Solid box |
| `drawOutlinedBox(box, color, seeThroughBlocks)` | Wireframe box |
| `drawBlockHighlight(pos, color, seeThroughBlocks)` | Slightly inset solid box for block highlights |
| `draw3DLine(from, to, color, seeThroughBlocks)` | Single line segment |
| `drawTracer(to, color, seeThroughBlocks)` | Line from the camera crosshair to a world position |
| `drawGradientLine(from, to, colorFrom, colorTo, seeThroughBlocks)` | Line with a color gradient between endpoints |
| `drawGradientTracer(to, colorFrom, colorTo, seeThroughBlocks)` | Gradient tracer from the camera crosshair to a world position |
| `drawText(pos, text, scale, color, shadow, seeThroughBlocks)` | Billboard text label |

#### WorldRenderShapes reference

| Function | Description |
|---|---|
| `drawCircle(center, radius, color, seeThroughBlocks, segments)` | Horizontal wireframe ring |
| `drawFilledCircle(center, radius, color, seeThroughBlocks, segments)` | Solid horizontal disc |
| `drawSphere(center, radius, color, seeThroughBlocks, stacks, slices)` | Wireframe sphere (latitude rings + longitude meridians) |
| `drawPolyline(points, color, seeThroughBlocks)` | Connected line segments through a list of points |
| `drawBezier(p1, control, p3, color, seeThroughBlocks, steps)` | Quadratic Bézier curve |
| `drawEntityBox(entity, color, seeThroughBlocks)` | Box fitted to an entity's bounding box |
| `drawWaypoint(pos, label, color, seeThroughBlocks)` | Small labeled pillar marker with text |
| `drawDynamicText(pos, text, baseScale, ...)` | Billboard text that scales with camera distance |

---

### HUD rendering

Subscribe to `RenderHudEvent` for 2D screen-space drawing. `HudRenderUtils.kt` provides `DrawContext` extension functions that accept `Color` values and use width/height parameters instead of raw coordinates.

```kotlin
@Feature
object StatusHud {
    @Subscribe
    fun onHud(event: RenderHudEvent) {
        if (!McUtils.isInGame) return
        event.drawTextPanel(
            lines = listOf("Health: 20", "Armor: 10"),
            x = 10,
            y = 10,
        )
    }
}
```

Register the event and feature in your mod initializer:

```kotlin
EventRegistry.addBus(RenderHudEvent::class, SimpleCore.EVENTBUS)
RenderHudEvent.registerEvents()
SimpleCore.EVENTBUS.registerFeature(StatusHud)
```

#### DrawContext extensions

| Function | Description |
|---|---|
| `fillRect(x, y, width, height, color)` | Solid rectangle (width/height form) |
| `drawBorderedRect(x, y, width, height, fillColor, borderColor, borderWidth)` | Rectangle with a solid border |
| `drawHudText(text, x, y, color, shadow)` | Draws a string via the Minecraft text renderer |
| `drawTextPanel(lines, x, y, textColor, bgColor, padding, shadow)` | Multi-line text with a background; returns `RenderSize` |
| `drawProgressBar(x, y, width, height, progress, foreground, background)` | Horizontal bar filled proportionally; `progress` in [0, 1] |
| `enableScissorRect(x, y, width, height)` | Restrict rendering to a region (width/height form) |
| `disableScissorRect()` | Remove the active scissor region |

`RenderHudEvent` exposes all of the above as direct forwarders, plus:

| Function | Description |
|---|---|
| `drawCenteredText(text, y, color, shadow)` | Centers text horizontally based on `screenWidth` |

---

## Utils

---

### McUtils

Null-safe accessors for common Minecraft client singletons. All properties must be read from the client thread only.

```kotlin
McUtils.mc           // MinecraftClient.getInstance()
McUtils.player       // ClientPlayerEntity?
McUtils.world        // ClientWorld?
McUtils.isInGame     // player != null && world != null
McUtils.isScreenOpen // mc.currentScreen != null
McUtils.playerPos    // BlockPos? (foot block)
McUtils.playerVec    // Vec3d? (foot position)
McUtils.playerEyePos // Vec3d? (eye position)
McUtils.isFirstPerson
```

---

### VecUtils

Extension functions on `Vec3d`, `BlockPos`, and `Box`.

**Vec3d:**

| Function | Description |
|---|---|
| `up(n)` / `down(n)` | Offset Y by ±n (default 1.0) |
| `add(dx, dy, dz)` | Component-wise add |
| `roundToBlock()` | Floor all components |
| `blockCenter()` | Floor X/Z, add 0.5 to X/Z (top-face center of the block column) |
| `distanceToPlayer()` / `distanceSqToPlayer()` | Distance to the local player's foot position |
| `distanceIgnoreY(other)` / `distanceSqIgnoreY(other)` | Horizontal-only distance |
| `middle(other)` | Midpoint between two vectors |
| `interpolate(other, t)` | Linear interpolation (delegates to MC's `lerp`) |
| `boundingToOffset(dx, dy, dz)` | Create a `Box` from `this` to `this + offset` |
| `axisAlignedTo(other)` | Smallest `Box` spanning two points |
| `expandBlock(n)` | 1×1×1 block-sized `Box` around this position inflated by `n/16` |
| `getBlockStateAt()` / `getBlockAt()` | Query the current world |
| `isInLoadedChunk()` | Returns false when the chunk is not loaded |
| `distanceToLine(start, end)` | Distance from this point to a line segment |

**BlockPos:**

| Function | Description |
|---|---|
| `toVec3d()` | Block center as `Vec3d` (x+0.5, y, z+0.5) |
| `getBlockStateAt()` / `getBlockAt()` | Query the current world |

**Box:**

| Function | Description |
|---|---|
| `expandBlock(n)` | Inflate by `n/16` |
| `center()` | Center of the box |
| `topCenter()` | Center of the top face |
| `isPlayerInside()` | Containment check against the local player |
| `minVec()` / `maxVec()` | Min/max corners as `Vec3d` |
| `corners()` | All 8 corners as a `List<Vec3d>` |

---

### ChatUtils

```kotlin
ChatUtils.print("§aMessage")           // inject into local chat (not sent to server)
ChatUtils.print(text)                  // overload accepting a Text object
ChatUtils.send("/command")             // send chat message (prefix / for commands)
ChatUtils.clickable(text, command, hover?)  // Text with RUN_COMMAND click event
ChatUtils.hoverable(text, lines)           // Text with multi-line hover tooltip
```

---

### StringUtils

```kotlin
str.removeColorCodes()         // strip §X sequences
str.stripFormattingCodes()     // alias for removeColorCodes
str.isPlayerName()             // matches [A-Za-z0-9_]{3,16}
str.widthInPixels()            // Minecraft font pixel width
str.splitToWidth(maxWidth)     // word-wrap to a List<String>
```

---

### Color

```kotlin
Color(r, g, b, a)              // ARGB data class
Color.fromHex("#RRGGBB")       // parse hex string
Color.fromMinecraftCode('a')   // map §-color code char to Color (0–9, a–f)
color.toHex()                  // serialize to "#RRGGBB" or "#RRGGBBAA"
color.withAlpha(a)
color.darker(factor)
color.lighter(factor)
color.blend(other, t)
```

Named constants: `Color.WHITE`, `BLACK`, `RED`, `GREEN`, `BLUE`, `YELLOW`, `CYAN`, `TRANSPARENT`.

---

## Build

```bash
./gradlew.bat build
./gradlew.bat test
```

---

## Contributing

Fork, add tests for your changes, target `master`. Keep changes focused - API additions are preferred over breaking changes.

---

## License

See `LICENSE`.
