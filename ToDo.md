# SimpleCore ToDo / Requirements

A lightweight Fabric library mod providing common utilities for client-side Minecraft modding.

## Status Legend
- `[DONE]` — Feature complete, may have minor tech debt
- `[IN PROGRESS]` — Partially implemented
- `[NOT STARTED]` — Planned but no implementation yet

---

## Modules

### Event Bus `[DONE]`

**Features:**
- Thread-safe event dispatch (`ConcurrentHashMap` + `CopyOnWriteArrayList`)
- Priority-based handler ordering (`EventPriority`: LOWEST / LOW / NORMAL / HIGH / HIGHEST)
- Per-priority-bucket cancellation snapshot (`CancellableEvent` / `Event` split)
- Lazy Fabric hook registration via `EventCompanion` (hooks only wired when handlers exist)
- `@Feature` annotation + `FeatureAutoLoader` classpath scanning via ClassGraph
- `@Subscribe(polymorphic = true)` — handlers fire for declared type AND all subtypes
- `RegistrationHandle` — selective per-handler unregistration without full feature teardown
- Pluggable exception handling (`EventExceptionHandler`, `DefaultEventExceptionHandler`, `RethrowEventExceptionHandler`)
- `@Subscribe(filter = MyFilter::class)` — stateless `EventFilter` predicate, zero-cost when `NoFilter` (default)
- `@ConditionalFeature(condition = MyCondition::class)` — feature skipped if condition returns false at scan time; built-ins: `ModPresentCondition`, `PhysicalClientCondition`
- `FeatureScanRequest.rejectedPackages` — exclude sub-packages from scanning
- `FeatureScanRequest.annotationClasses` — use custom marker annotation instead of `@Feature`
- `EventBus.debugMode` — per-invocation debug logging
- `EventBus.getHandlerSummary(eventClass)` — read-only introspection snapshot
- `EventBusMonitor` — configurable ring buffer of recent post records
- `EventRegistry` — global event-class → bus mapping; multi-bus isolation via factory lambdas
- `ScanEntrypoint` system — other mods opt in via Fabric entrypoints
- Kotlin-friendly API with `KClass` support throughout

---

### Keybind System `[DONE]`

**Features:**
- Vanilla keybinds via `KeybindRegistry.registerVanilla` (Fabric `KeyBindingHelper` integration)
- Virtual keybinds via `KeybindRegistry.registerVirtual` (no vanilla registration, fully runtime-managed)
- `KeyDescriptor` — unified key descriptor using `InputUtil.Key`; factories `KeyDescriptor.keyboard(keyCode)` and `KeyDescriptor.mouse(button)` cover keyboard and mouse buttons
- `KeybindHandle` returned by both register methods — `unregister()`, `block()`/`unblock()`, `isRegistered`
- `KeyContext` — `ANY`, `IN_GAME`, `IN_CUSTOM_SCREEN`, `IN_CHAT`, `IN_HANDLED_SCREEN`; passed as `vararg` for ergonomic single-context calls
- `Modifiers` (Ctrl, Shift, Alt) — `matches(Window)` for tick polling, `matchesMask(Int)` for event bitmask
- Callbacks: `onPress`, `onRelease`, `onHold` (with `holdEveryTicks` throttle), `onHandledScreen`
- Global blocking: `blockKeybind()` / `unblockKeybind()`
- Context-level blocking: `blockContext(vararg KeyContext)` / `unblockContext(vararg KeyContext)`
- Per-keybind blocking: `KeybindHandle.block()` / `unblock()` without unregistering
- Mouse button support via `KeyDescriptor.mouse(button)` routing to `glfwGetMouseButton`
- Runtime rebinding: `updateVirtualKeybind(id, KeyDescriptor)`
- Id validation warnings: vanilla keys checked against `key.<modid>.<action>` convention; virtual warns on duplicate ids
- Thread-safe: `@Volatile` on `frame`, `blocked`, `individuallyBlocked`; `blockedContexts` backed by `ConcurrentHashMap.newKeySet()`

---

### Config System `[IN PROGRESS]`

**Done:**
- 13 annotation definitions for UI mapping:
  - `@Category`, `@Collapsible`, `@DefaultCollapsed`
  - `@Slider`, `@Dropdown`, `@ColorPicker`, `@Button`, `@TextField`, `@SearchField`
  - `@Separator`, `@ExcludeFromVisuals`, `@ExcludeFromConfig`, `@ListConfig`

**Missing:**
- File persistence (JSON or TOML serialization)
- GUI renderer implementation
- Reflection-based annotation processor for automatic UI generation
- Runtime config reloading

**Requirements (from original spec):**

#### Data Types
- Primitives: `int`, `string`, `bool`
- Lists:
  - Immutable and mutable variants
  - User-definable element types
  - Custom default value support

#### Field / UI Components
- Dropdown selection
- Color picker with alpha support
- Clickable buttons
- Slider with numeric text input (start, step, end)
- Separator (visual divider above field)
- Text field for string input
- Search field across categories

#### Visibility / Configuration Flags
- `excludeFromVisuals` — field not shown in GUI
- `excludeFromConfig` — field not saved to file

#### Behavior / Persistence
- Persist collapsed state per category
- Annotations define mapping between data and UI
- Search filters categories and fields dynamically

#### Implementation Notes
- Use Reflection for automatic UI generation from annotations
- Support subclasses for collapsible category groups
- Tests for list types, default values, and collapsed state persistence

---

### Command System `[NOT STARTED]`

Empty `command/` directory exists.

**Requirements:**
- Client-side command registration
- Argument parsing with type validation
- Tab completion / suggestions
- Help text generation
- Subcommand support
- Permission/context checks

---

### UI Library `[NOT STARTED]`

**Requirements (from original spec):**

#### Core Components
- Base overlay class with position tracking and drag-to-move
- Render layering / z-ordering
- Click and hover event handling

#### Predefined UIs
- Text content displays
- Optional images/icons per row
- Collapsible categories
- Clickable rows with callbacks

#### Customization
- Color theming system
- Font configuration
- Padding/margin controls

---

### Utils `[NOT STARTED]`

**Potential Helpers:**
- Math utilities (interpolation, clamping)
- Color manipulation (HSV/RGB conversion, alpha blending)
- Text formatting helpers
- Timer/cooldown utilities
- Minecraft-specific helpers (player state, world access)

---

## Technical Debt & Issues

### Low
1. **Missing documentation** — No KDoc comments on public APIs
2. **No example mod** — Would help demonstrate library usage

---

## Priority Roadmap

### P0 — Critical Fixes
- [x] Add `@Volatile` to `frame` and `blocked` in `KeybindRegistry.kt`
- [x] Remove or fix autoloader reference in `EventBus.kt`

### P1 — Complete Config System
- [ ] Implement JSON/TOML file persistence
- [ ] Build reflection-based annotation processor
- [ ] Create GUI renderer with all field types
- [ ] Add config reload support

### P2 — New Features
- [ ] Command system implementation
- [ ] UI library with moveable overlays
- [ ] Utils module with common helpers

### P3 — Polish
- [ ] Add KDoc documentation
- [ ] Create example mod
- [ ] Write tests: cancellation semantics, polymorphic dispatch, concurrent register/unregister, filter behaviour
