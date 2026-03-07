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
    override fun shouldLoad() = /* any check available at mod init time */
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

- Vanilla keybinds with Fabric integration via `KeybindRegistry`
- Virtual keybinds independent of the vanilla system via `VirtualKeybind`
- Context-aware activation (in-game only checks)
- Modifier key support: Ctrl, Shift, Alt
- Configurable press modes: `PRESS`, `RELEASE`, `HOLD`, `TOGGLE`

---

## Config System

**Status: In Progress**

<details>
<summary>What's done and what's missing</summary>

**Done — 13 annotation definitions for UI mapping:**
`@Category`, `@Collapsible`, `@DefaultCollapsed`, `@Slider`, `@Dropdown`, `@ColorPicker`, `@Button`, `@TextField`, `@SearchField`, `@Separator`, `@ExcludeFromVisuals`, `@ExcludeFromConfig`, `@ListConfig`

**Missing:**
- File persistence (JSON or TOML serialization)
- Reflection-based annotation processor for automatic UI generation
- GUI renderer (all field types)
- Runtime config reloading

</details>

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
