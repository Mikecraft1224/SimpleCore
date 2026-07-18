[← Back to README](../README.md)

# Event Bus

SimpleCore provides a global, thread-safe event bus that any mod can subscribe to. Most mods only need three things: opt in, write handlers, and post events.

For advanced use cases - running your own isolated bus, custom exception policies, handler introspection - see [Creating Your Own Bus](#creating-your-own-bus).

---

## Using the Global Bus

### 1. Opt in

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

To use your own marker annotation instead of the shared `@Feature`:

```kotlin
FeatureScanRequest(
    packages = listOf("com.example.mymod.features"),
    annotationClasses = listOf(MyFeature::class)
)
```

---

### 2. Write handlers

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

### 3. Post events

To fire an event to all mods subscribed to the global bus:

```kotlin
EventRegistry.post { MyEvent(data) }
```

Only buses that have at least one handler registered for `MyEvent` receive it. Cancellation state is isolated per bus - one mod cancelling an event does not affect another mod's copy.

---

### Handler options

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

## Creating Your Own Bus

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

`SimpleCore.EVENTBUS`'s own `EventBusMonitor` stats can be shown in-game via `DebugOverlayExample` - see [Utils](utils.md#debug-tooling).

</details>

---

## Built-in events

Beyond `ClientTickEvent`, `RenderWorldEvent`, and `RenderHudEvent` (covered in [Render System](render-system.md)), SimpleCore ships several ready-to-use events:

| Event | Fires when | Cancellable |
|---|---|---|
| `ChatReceiveEvent` / `ChatSendEvent` | A chat message is received / about to be sent | Yes |
| `WorldChangeEvent` / `WorldTickEvent` | Client world (dis)connects / ticks | No / No |
| `EntityWorldEvent` (enter/leave) | An entity is added to or removed from the world | No |
| `BlockClickEvent` | The player clicks a block | Yes |
| `ServerBlockChangeEvent` | The server sends a block update packet | No |
| `ItemTooltipEvent` | An item tooltip is being built | No |
| `PlaySoundEvent` | The server sends a sound-play packet | Yes |
| `ReceiveParticleEvent` | The server sends a particle-spawn packet | Yes |
| `ResourceReloadEvent` | Resources are reloaded | No |
| `GuiScreenOpenEvent` | A screen opens or closes | No |
| `InventoryKeyPressEvent` | A key is pressed while a container screen is open | No |
| `HudMouseClickEvent` | A mouse button is pressed with no screen open | No |
| `SecondPassedEvent` | Once per real-time second | No |
| `TabListEvent` | The server updates the tab-list header/footer | No |
| `PacketReceiveEvent` / `PacketSendEvent` | Any packet is received from / sent to the server | Yes |
| `RenderEntityOutlineEvent` | Once per frame - queue entities for a glow outline | No |

`PacketReceiveEvent`/`PacketSendEvent` fire for *every single packet* (movement, keep-alive, chunk data, everything) - filter by `event.packet`'s type early and return quickly for types you don't care about:

```kotlin
@Subscribe
fun onPacket(event: PacketReceiveEvent) {
    val packet = event.packet as? ClientboundSomeSpecificPacket ?: return
    // ...
}
```

`TabListEvent` covers only the header/footer text. For the actual player rows (name, ping, gamemode), read them live via `TabListUtils.entries` instead - see [Utils](utils.md#tablist--scoreboard).

`RenderEntityOutlineEvent` gives every loaded entity per frame; call `event.highlight(entity, color)` to queue a glow outline (the same visual effect as the vanilla Glowing potion, always visible through walls):

```kotlin
@Feature
object PartyHighlighter {
    @Subscribe
    fun onOutline(event: RenderEntityOutlineEvent) {
        for (entity in event.entities) {
            if (entity is Player && entity.name.string in partyMembers) {
                event.highlight(entity, Color.CYAN)
            }
        }
    }
}
```

Like `RenderWorldEvent`, `RenderEntityOutlineEvent` needs one-time explicit wiring (it doesn't lazy-register the way most other events do):

```kotlin
EventRegistry.addBus(RenderEntityOutlineEvent::class, SimpleCore.EVENTBUS)
RenderEntityOutlineEvent.registerEvents()
```
