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
  - [KeybindHandle — lifecycle control](#keybindhandle--lifecycle-control)
  - [Blocking](#blocking)
- [Config System](#config-system)
- [Build](#build)
- [Contributing](#contributing)
- [License](#license)

---

## Modules

| Module | Status |
|---|---|
| Event Bus | Done |
| Keybind System | Done |
| Config System | In Progress |
| Command System | Not Started |
| UI Library | Not Started |
| Utils | Not Started |

---

## Event Bus

SimpleCore provides a global, thread-safe event bus that any mod can subscribe to. Most mods only need three things: opt in, write handlers, and post events.

For advanced use cases — running your own isolated bus, custom exception policies, handler introspection — see [Creating Your Own Bus](#creating-your-own-bus).

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

Only buses that have at least one handler registered for `MyEvent` receive it. Cancellation state is isolated per bus — one mod cancelling an event does not affect another mod's copy.

---

#### Handler options

<details>
<summary>Priority — control execution order</summary>

Available priorities, highest first: `HIGHEST`, `HIGH`, `NORMAL` (default), `LOW`, `LOWEST`.

```kotlin
@Subscribe(priority = EventPriority.HIGH)
fun onHighPriority(event: ClientTickEvent) { /* runs before NORMAL handlers */ }
```

**Cancellation semantics:** the cancelled state is snapshotted once per priority level. Handlers in the same priority bucket see the same cancelled state — they cannot observe each other's cancellations, only those from higher-priority buckets.

```kotlin
@Subscribe(receiveCancelled = true)
fun alwaysRuns(event: MyEvent) { /* runs even if a higher-priority handler cancelled it */ }
```

</details>

<details>
<summary>Inline filters — skip invocation without guard code in the handler</summary>

```kotlin
object EndPhaseOnly : EventFilter<ClientTickEvent> {
    override fun test(event: ClientTickEvent) =
        event.phase == ClientTickEvent.Phase.END
}

@Subscribe(filter = EndPhaseOnly::class)
fun onEndTick(event: ClientTickEvent) {
    // only called on END phase — no if-check needed here
}
```

Filters must be Kotlin `object`s or have a no-arg constructor. The default `NoFilter` adds zero overhead.

</details>

<details>
<summary>Conditional loading — skip a feature based on environment or mod presence</summary>

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
<summary>Polymorphic dispatch — subscribe to a base type</summary>

```kotlin
// Fires for every Event subtype posted to the bus:
@Subscribe(polymorphic = true)
fun onAnyEvent(event: Event) { /* ... */ }

// Fires for CancellableEvent and all its subtypes:
@Subscribe(polymorphic = true)
fun onAnyCancellable(event: CancellableEvent) { /* ... */ }
```

Polymorphic handlers are stored at registration time — no extra cost per dispatch.

</details>

---

### Creating Your Own Bus

For most mods, the global bus is all you need. If you need an isolated bus — for example, to build a plugin system within your own mod, or to run your own `ScanEntrypoint` key — you can instantiate and manage an `EventBus` directly.

The relevant files are documented with KDoc:

| File | Purpose |
|---|---|
| `EventBus.kt` | Core bus: registration, dispatch, introspection, debug mode |
| `FeatureAutoLoader.kt` | Classpath scanning and bus wiring |
| `EventRegistry.kt` | Global event-class → bus mapping |
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

Custom event classes register their Fabric hooks lazily via `EventCompanion` — see `ClientTickEvent.kt` for a complete example.

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

**Vanilla keybind** — appears in Minecraft's controls screen, player-remappable:

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

**Virtual keybind** — not shown in options screen, fully runtime-managed:

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
| `IN_CUSTOM_SCREEN` | A non-chat, non-handled screen is open (custom mod GUIs) |
| `IN_CHAT` | Chat screen is open |
| `IN_HANDLED_SCREEN` | Inventory, chest, crafting table, etc. |

```kotlin
// Single context — pass positionally after the required params
KeybindRegistry.registerVirtual(id = "mymod.zoom", key = myKey, KeyContext.IN_GAME, onPress = { /* ... */ })

// Multiple contexts — multiple positional vararg values
KeybindRegistry.registerVirtual(id = "mymod.zoom", key = myKey, KeyContext.IN_GAME, KeyContext.IN_CUSTOM_SCREEN, onPress = { /* ... */ })

// No context arg — defaults to ANY
KeybindRegistry.registerVirtual(id = "mymod.zoom", key = myKey, onPress = { /* ... */ })
```

---

### Callbacks

| Callback | Signature | When called |
|---|---|---|
| `onPress` | `(MinecraftClient) -> Unit` | Leading edge — key goes down |
| `onRelease` | `(MinecraftClient) -> Unit` | Trailing edge — key comes up |
| `onHold` | `(MinecraftClient, Int) -> Unit` | Every `holdEveryTicks` ticks while held; second arg is hold tick count |
| `onHandledScreen` | `(MinecraftClient, Slot) -> Unit` | Immediate key-press inside a handled screen; second arg is the hovered slot |

All callbacks are optional and default to no-ops.

---

### KeybindHandle — lifecycle control

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

> **Note:** Unregistering a vanilla keybind removes it from SimpleCore's dispatch but does not remove it from Minecraft's keybinding options screen — Fabric provides no API for that.

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

SimpleCore provides a annotation-driven config system. You define a plain Kotlin class with annotated fields, and the library handles JSON persistence and GUI generation automatically.

---

### Quick start

**1. Define a config class**

```kotlin
class MyConfig {
    @Entry("Enable feature", "Turns the feature on or off")
    @EditorBoolean
    var enabled = true

    @Entry("Speed", "Movement speed multiplier")
    @EditorSlider(min = 0.5, max = 5.0, step = 0.5)
    var speed = 1.0

    @Entry("Mode")
    @EditorDropdown                        // Enum — uses toString()
    var mode = Mode.NORMAL

    @Entry("Quality")
    @EditorDropdown(values = ["Low", "Medium", "High"])   // Int backing field
    var quality = 1

    @Separator
    @Entry("Blocked players", "Players to ignore")
    @EditorMutable(defaultString = "")
    var blockedPlayers: MutableList<String> = mutableListOf()

    @Category("Advanced", "Advanced options")
    var advanced = AdvancedSettings()
}

enum class Mode { NORMAL, FAST, STEALTH }

class AdvancedSettings {
    @Entry("Username")
    @EditorText
    var name = "Player"

    @Entry("Accent colour")
    @EditorColor
    var color = java.awt.Color(0x5865F2)

    @Collapsible
    @Entry("Extra")
    @DefaultCollapsed
    var extra = ExtraSettings()
}

class ExtraSettings {
    @Entry("Reset defaults")
    @EditorButton("Reset")
    var resetAction = Runnable { /* reset logic */ }
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
// From a keybind or mod menu integration:
client.setScreen(ConfigScreen(client.currentScreen, ConfigProcessor.process(config), manager))
```

---

### Annotations reference

| Annotation | Field type | Effect |
|---|---|---|
| `@Entry(name, description)` | any | Required to show the field in the GUI |
| `@EditorBoolean` | `Boolean` | Toggle switch |
| `@EditorSlider(min, max, step)` | `Int` / `Float` / `Double` | Slider with numeric display |
| `@EditorDropdown` | `Enum` | Cycles through enum values using `toString()` |
| `@EditorDropdown(values = [...])` | `Int` (index) or `String` (label) | Dropdown with explicit label list |
| `@EditorText` | `String` | Text input field |
| `@EditorButton(buttonText)` | `Runnable` | Clickable button (not saved to file) |
| `@EditorColor` | `java.awt.Color` | Opens ARGB channel sliders with live preview |
| `@EditorKeybind(defaultKey)` | `Int` (GLFW key code) | Keybind capture button |
| `@EditorInfo` | `String` | Read-only label showing the field value |
| `@EditorMutable` | `List<String/Int/Boolean>` | Opens list editor (add / edit / remove items) |
| `@Category(name, description)` | nested object | Creates a separate category page |
| `@Collapsible` | nested object | Inline collapsible group within the current page |
| `@DefaultCollapsed` | (with `@Collapsible`) | Group starts collapsed |
| `@Separator` | any field | Draws a horizontal divider line above the field |
| `@Excluded(config, gui)` | any | Skip field from JSON (`config=true`) or GUI (`gui=true`, default) |

---

### Persistence

`ConfigManager` uses Gson to serialize config fields to a pretty-printed JSON file.

- On `save()`: writes to a `.tmp` file, validates it parses cleanly, then atomically replaces the target — no data loss on interrupted writes.
- On `load()`: merges values in-place into the existing config instance — missing keys keep defaults, unknown keys are ignored.
- `reload()` = `load()` + notifies `onReload` listeners.
- `Runnable` fields and `@Excluded(config=true)` fields are never saved.

---

### Error handling

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

---

### Runtime reloading

```kotlin
manager.onReload { updatedConfig ->
    // called every time reload() is invoked (e.g., after the player edits the file)
}
manager.reload()
```

---

## Build

```bash
./gradlew.bat build
./gradlew.bat test
```

---

## Contributing

Fork, add tests for your changes, target `master`. Keep changes focused — API additions are preferred over breaking changes.

---

## License

See `LICENSE`.
