# SimpleCore

Lightweight Fabric client-side mod library - an event bus, keybind system, annotation-driven config/GUI, a command DSL, a HUD overlay framework, and world/HUD render utilities, so downstream mods don't have to build that infrastructure themselves.

## Modules

| Module | What it gives you | Docs |
|---|---|---|
| Event Bus | `@Feature`/`@Subscribe` dispatch, priorities, filters, polymorphic handlers, per-handler unregister | [docs/event-bus.md](docs/event-bus.md) |
| Keybind System | Vanilla + virtual keybinds, contexts, hold/handled-screen callbacks, config binding | [docs/keybind-system.md](docs/keybind-system.md) |
| Config System | Annotation-driven fields, auto-inferred GUI widgets, JSON persistence, migrations | [docs/config-system.md](docs/config-system.md) |
| Command System | Kotlin DSL over Brigadier, tab completion, permission guards, runtime enable/disable | [docs/command-system.md](docs/command-system.md) |
| Overlay System | Draggable HUD panels, per-mod grouping, a built-in position editor | [docs/overlay-system.md](docs/overlay-system.md) |
| Render System | World-space shapes/tracers/text (Gizmos-based) and 2D HUD drawing helpers | [docs/render-system.md](docs/render-system.md) |
| Utils | Time/scheduling, numbers, chat, tab-list/scoreboard, sound, entities, raycasts, items, movement/rotation, simulated clicks/interaction | [docs/utils.md](docs/utils.md) |
| Multi-version builds | How Stonecutter builds 26.1.2 + 26.2 from one codebase, and how to add a new version | [docs/multi-version.md](docs/multi-version.md) |

---

## Quick start

**1. Opt in to the event bus** (`fabric.mod.json`):

```json
{ "entrypoints": { "simplecore:feature_scan": [ { "value": "com.example.mymod.SimpleCoreEntrypoint", "adapter": "kotlin" } ] } }
```

```kotlin
class SimpleCoreEntrypoint : ScanEntrypoint {
    override fun scanRequest() = FeatureScanRequest(packages = listOf("com.example.mymod.features"))
}
```

**2. Write a feature:**

```kotlin
@Feature
object MyFeature {
    @Subscribe
    fun onTick(event: ClientTickEvent) { /* runs every client tick */ }
}
```

**3. Add a config, a command, and a HUD overlay:**

```kotlin
@Config(title = "My Mod")
class MyConfig {
    @Entry(description = "Speed multiplier")
    @Slider(0.5, 5.0, 0.5)
    var speed = 1.0
}

CommandRegistry.client("mymod") {
    executes { sendFeedback("Hello from /mymod!") }
}

object MyHud : HudElement("My HUD", OverlayPosition(10f, 10f)) {
    override fun buildContent() = listOf(HudRenderable.text("§eSpeed: §a${config.speed}"))
}
```

Full walkthroughs, all annotations/value types, and the complete API reference for each module are in [docs/](docs) (table above).

---

## Build

SimpleCore targets two Minecraft versions from one codebase via [Stonecutter](https://stonecutter.kikugie.dev/) - see [docs/multi-version.md](docs/multi-version.md) for the full explanation. The short version:

```bash
./gradlew build       # builds whichever version is currently active
./gradlew runClient   # launches whichever version is currently active

./gradlew "Set active project to 26.1.2"   # switch active version (rewrites src/ in place)
./gradlew "Set active project to 26.2"
./gradlew "Reset active project"           # switch back to the vcsVersion (26.2) before committing
```

---

## Contributing

Fork, add tests for your changes, target `master`. Keep changes focused - API additions are preferred over breaking changes.

---

## License

See `LICENSE`.
