# SimpleCore ToDo / Requirements

A lightweight Fabric library mod providing common utilities for client-side Minecraft modding.

## Status Legend
- `[DONE]` - Feature complete, may have minor tech debt
- `[IN PROGRESS]` - Partially implemented
- `[NOT STARTED]` - Planned but no implementation yet

---

## Modules

### Event Bus `[DONE]`

**Features:**
- Thread-safe event dispatch (`ConcurrentHashMap` + `CopyOnWriteArrayList`)
- Priority-based handler ordering (`EventPriority`: LOWEST / LOW / NORMAL / HIGH / HIGHEST)
- Per-priority-bucket cancellation snapshot (`CancellableEvent` / `Event` split)
- Lazy Fabric hook registration via `EventCompanion` (hooks only wired when handlers exist)
- `@Feature` annotation + `FeatureAutoLoader` classpath scanning via ClassGraph
- `@Subscribe(polymorphic = true)` - handlers fire for declared type AND all subtypes
- `RegistrationHandle` - selective per-handler unregistration without full feature teardown
- Pluggable exception handling (`EventExceptionHandler`, `DefaultEventExceptionHandler`, `RethrowEventExceptionHandler`)
- `@Subscribe(filter = MyFilter::class)` - stateless `EventFilter` predicate, zero-cost when `NoFilter` (default)
- `@ConditionalFeature(condition = MyCondition::class)` - feature skipped if condition returns false at scan time; built-ins: `ModPresentCondition`, `PhysicalClientCondition`
- `FeatureScanRequest.rejectedPackages` - exclude sub-packages from scanning
- `FeatureScanRequest.annotationClasses` - use custom marker annotation instead of `@Feature`
- `EventBus.debugMode` - per-invocation debug logging
- `EventBus.getHandlerSummary(eventClass)` - read-only introspection snapshot
- `EventBusMonitor` - configurable ring buffer of recent post records
- `EventRegistry` - global event-class -> bus mapping; multi-bus isolation via factory lambdas
- `ScanEntrypoint` system - other mods opt in via Fabric entrypoints
- Kotlin-friendly API with `KClass` support throughout
- **Built-in events:** `ClientTickEvent` (with `isMod(i, offset)`, `Phase`), `RenderHudEvent` (with `Phase`: `GAME_OVERLAY`/`IN_INVENTORY`), `RenderWorldEvent`, `WorldTickEvent`, `InventoryKeyPressEvent`, `HudMouseClickEvent`, `SecondPassedEvent`, `WorldChangeEvent(joining)`, `ResourceReloadEvent`, `ChatReceiveEvent` (cancelable), `ChatSendEvent` (cancelable), `BlockClickEvent` (cancelable, LEFT/RIGHT), `ItemTooltipEvent`, `EntityEnterWorldEvent`, `EntityLeaveWorldEvent`, `GuiScreenOpenEvent` (cancelable), `ServerTickEvent`, `ServerBlockChangeEvent`, `ReceiveParticleEvent` (cancelable), `PlaySoundEvent` (cancelable)

---

### Keybind System `[DONE]`

**Features:**
- Vanilla keybinds via `KeybindRegistry.registerVanilla` (Fabric `KeyBindingHelper` integration)
- Virtual keybinds via `KeybindRegistry.registerVirtual` (no vanilla registration, fully runtime-managed)
- `KeyDescriptor` - unified key descriptor using `InputUtil.Key`; factories `KeyDescriptor.keyboard(keyCode)` and `KeyDescriptor.mouse(button)` cover keyboard and mouse buttons
- `KeybindHandle` returned by both register methods - `unregister()`, `block()`/`unblock()`, `isRegistered`
- `KeyContext` - `ANY`, `IN_GAME`, `IN_ANY_SCREEN`, `IN_CHAT`, `IN_HANDLED_SCREEN`; passed as `vararg` for ergonomic single-context calls
- `Modifiers` (Ctrl, Shift, Alt) - `matches(Window)` for tick polling, `matchesMask(Int)` for event bitmask
- Callbacks: `onPress`, `onRelease`, `onHold` (with `holdEveryTicks` throttle), `onHandledScreen`
- Global blocking: `blockKeybind()` / `unblockKeybind()`
- Context-level blocking: `blockContext(vararg KeyContext)` / `unblockContext(vararg KeyContext)`
- Per-keybind blocking: `KeybindHandle.block()` / `unblock()` without unregistering
- Mouse button support via `KeyDescriptor.mouse(button)` routing to `glfwGetMouseButton`
- Runtime rebinding: `updateVirtualKeybind(id, KeyDescriptor)`
- Id validation warnings: vanilla keys checked against `key.<modid>.<action>` convention; virtual warns on duplicate ids
- Thread-safe: `@Volatile` on `frame`, `blocked`, `individuallyBlocked`; `blockedContexts` backed by `ConcurrentHashMap.newKeySet()`

---

### Config System `[DONE]`

**Done:**
- `@Config(title, subtitle, accentColor, searchEnabled, defaultCategory)` - top-level config metadata
- Structural annotations (`config.api.annotations`): `@Category`, `@Collapsible`, `@DefaultCollapsed`, `@Separator`, `@Excluded`, `@SearchTag`
- Entry annotation: `@Entry(name?, description?)` - marks a field as a GUI entry; name auto-derived from camelCase if omitted
- Visibility annotation: `@Conditional(value?, condition?)` - composable condition for both entries and categories; `value` follows `Reference<Boolean>` fields transparently for cross-class visibility; `condition: KClass<out VisibilityCondition>` for complex conditions
- Targeted annotations: `@Slider(min, max, step?)` for numeric ranges; `@Info` for read-only string display
- **Value types** - field type fully encodes the widget; no further annotation needed:
  - `Dropdown(vararg options)` / `Dropdown(property)` - single selection; serialized as Int index
  - `DropdownList(vararg options)` / `DropdownList(property)` - list of dropdown selections; serialized as `[Int]`
  - `MultiSelect(vararg options)` / `MultiSelect(property)` - zero-or-more selection; serialized as `[String]`
  - `Button(label) { action }` - clickable button; not serialized
  - `Keybind(packed)` / `Keybind.of(keyCode, ctrl, shift, alt)` - keybind; serialized as packed Int
- **Type inference** - `Boolean` -> toggle, `String` -> text, `Enum` -> dropdown, `java.awt.Color` -> color picker, `MutableList<String/Int/Boolean>` -> list editor, `MutableList<CustomClass>` -> object list editor (auto-inferred, no `@ObjectList` annotation needed), `Reference<MutableList<T>>` -> list editor
- `Reference<T>` (`config.api.values`) - transparent two-way proxy for an external property; config fields can reference external state; serialized bidirectionally; `resolveFieldSource` follows References for `@Conditional`
- `ConfigSerializer` - Gson reflection serializer; atomic write (tmp -> validate -> move); in-place merge on load; object-element lists merged via `mergeFromJson` per element so value types inside nested classes are preserved
- `ConfigManager` - `save/load/reload/onReload/onSaveFailed/onLoadFailed`, migration DSL (`MigrationContext`: `rename`, `remove`, `transformValue`, `nested`)
- `ConfigProcessor` - reflection processor; type inference; `resolveFieldSource` follows References; `findEntryByName` matches by `fieldName` (Kotlin property name)
- `ConfigScreen` - two-panel screen (category list + scrollable entry list); search bar; separator lines; collapsible groups; scrollbar; tooltips; subcategory accordion sidebar; live condition/visibility evaluation per frame
- List overlay: add/remove/edit; drag-to-reorder; dropdown-per-item mode with per-item DropdownOverlay
- Object list overlay: add/delete/reorder; "Edit" opens ElementEditOverlay sub-dialog
- Accordion-style collapsible groups: depth-colored header; tree-view guide lines; arbitrary nesting
- Color overlay: ARGB sliders + HSV spectrum + hex field + preview swatch
- Dropdown/MultiSelect overlays: popup with scrolling, ESC-to-close, click-outside-to-close
- `ProcessedEntry.fieldName` - set for all entries; used by `bindVisible` / `bindDropdownOptions` for refactor-safe KProperty runtime bindings
- KProperty runtime binding API: `bindVisible`, `bindCategoryWhen`, `bindDropdownOptions` on `ProcessedConfig`
- `KeybindHandle.withConfigBinding(KProperty0<*>)` + `ProcessedConfig.applyKeybindBindings()` - one-step keybind-config wiring

~~`MutableListScreen`~~ - deleted (replaced by `ListOverlay`)

**Remaining:**
- Tests for list types, default values, and collapsed state persistence
- Persist collapsed-group state across screen open/close

---

### Command System `[DONE]`

**Features:**
- `CommandRegistry.client(name, vararg aliases) { }` - registers a client-side command via `ClientCommandRegistrationCallback`; returns `CommandHandle`
- `CommandHandle.enable()` / `disable()` - toggles the command at runtime; implemented via Brigadier `requires` so disabled commands also vanish from tab completion
- `CommandBuilder` DSL: `literal(name) {}`, `argument(name, type) {}`, `executes {}`, `requires {}`, `suggests {}`
- `ClientCommandContext` receiver in `executes {}`: typed argument accessors (`int`, `long`, `float`, `double`, `bool`, `string`, `identifier`, `color`), generic `get<T>(name)` for custom types, `sendFeedback`/`sendError` helpers, `fail(message)` for `CommandSyntaxException`, `input` for raw command string
- `Arguments.kt` - convenience factories: `integer()`, `longArg()`, `floatArg()`, `doubleArg()`, `bool()`, `word()`, `string()`, `greedyString()`, `identifier()`, `color()`
- `SubCommand` (`fun interface`) - self-contained command group implementable as a Kotlin `object`; installed via `CommandBuilder.install(sub)`; nestable to arbitrary depth
- `@Feature` auto-discovery - `CommandRegistry` is found by `FeatureAutoLoader` without explicit registration in `SimpleCore`
- Commands share handles across aliases - `handle.disable()` disables all aliases at once

**Remaining / improvements:**
- Server-side command support (`CommandRegistry.server(...)`)
- Expand Minecraft-specific argument types (entity selector, block position, NBT, etc.)
- Command registration happens at mod init - late registration (post-join) not supported

---

### Overlay System `[DONE]`

**Features:**
- `RenderHudEvent` - fires each frame via Fabric `HudRenderCallback.EVENT`; calls `OverlayRegistry.beginFrame()` before posting so frame entries are always fresh
- `OverlayPosition(x, y, scale)` - stores absolute screen position + uniform scale; GSON-serializable as a plain field in any config class
- `OverlaySize(width, height)` - pixel dimensions returned from the `renderAt` block; used to size drag handles
- `renderAt(ctx, screenW, screenH, label) { block }` - inline extension on `OverlayPosition`; applies matrix transform (translate + scale), calls block at local (0, 0), restores matrix, reports to registry
- `OverlayRegistry` - frame-scoped singleton; `beginFrame()` clears, `renderAt` populates `frameEntries`; `openEditScreen(onClose?)` is now `internal` — use `HudManager.openEditor()` instead
- `OverlayEditScreen(onClose?)` - interactive drag editor; `removed()` calls `onClose` so mods can auto-save config; drag = reposition, arrow keys = nudge 1px (Shift = 10px), scroll = scale (0.5× – 3.0×), Escape = close
- `HudElement(displayName, position)` - base class for overlay panels; `buildContent(): List<HudRenderable>` describes content; result cached per frame (set in `renderFrame`, reused in `routeMouseClicked`)
- `HudRenderable` - sealed interface; factory functions: `text`, `hoverable`, `clickable`, `selector`, `vertical`, `horizontal`, `spacer`, `custom`
- `HudManager` - `@Feature` dispatcher; `register(element)`, `unregister(element)`, `openEditor(onClose?)` (public API for opening the editor)
- Example: `SessionTracker` - demonstrates all `HudRenderable` types including `horizontal` for label+value rows; `buildContent()` with cached tick data
- `OverlayExampleLoader` - enables with `SimpleCore.examples.overlay = true`; default editor keybind: O; uses `HudManager.openEditor()`

---

### UI Library `[IN PROGRESS]`

**Done:**
- `Widget` interface - bounding box + render + full input dispatch matching MC 1.21.10 API (`Click`, `KeyInput`, `CharInput`)
- `Panel` abstract container - children list, forwarded render/input, `onAdded`/`onRemoved` via `ChildRegistrar`
- `UiScreen` base Screen - hosts root Panel, delegates all input, manages `ChildRegistrar` lifecycle; F4 toggles debug bounds overlay
- `UiTheme` data class - Catppuccin Mocha defaults, partially overridable per-screen; `currentTheme` global + `withTheme {}` scope
- `ChildRegistrar` interface - lets widgets register Minecraft child elements without breaking protected access
- Widgets: `Button`, `Toggle` (pill toggle), `TextField` (wraps `TextFieldWidget`), `Label` (open; static or lambda text provider), `Image` (drawTexture), `Divider` (horizontal/vertical, optional label), `FormRow` (label+widget composite for settings forms)
- Layouts: `HSplit` (left/right ratio), `VSplit` (top/bottom ratio), `LinearLayout` (HORIZONTAL/VERTICAL, per-child preferred size, weight-based distribution, spacing), `FrameLayout` (absolute relX/relY placement)
- `ModifiedWidget` - fluent sizing/padding/alignment/weight hints via extension functions (`.width()`, `.height()`, `.size()`, `.padding()`, `.align()`, `.weight()`, `.fillWidth()`, `.fillHeight()`)
- DSL scopes: `@UiDsl`, `LinearLayoutScope` (`+widget` operator), `FrameScope` (`widget.at(relX, relY, w, h)`)
- `weightedRow(vararg Pair<Widget, Int>)` builder for proportional splits
- `ExampleScreen` - rewritten to demonstrate all new DSL features, FormRow, weight-based layouts, and dynamic Labels

**Remaining:**
- Overlay mechanism in UiScreen (OverlayHost interface) -- required for fully extracting
  Dropdown, ColorPicker, Keybind, and ListEditor into standalone Widget classes
- Full config entry widgets as proper Widgets (Slider, Dropdown, ColorPickerWidget, etc.)
- Scrollable panels / scroll container
- Tooltip support
- Focus ring / keyboard navigation

---

### Utils `[DONE]`

**Implemented:**
- `Color` - ARGB data class; constructors: `fromArgb/fromRgb/fromHex/fromAwtColor`; manipulation: `withAlpha`, `darker`, `lighter`, `blend`; interop: `toAwtColor()`; new: `toHex()` (serializes to `#RRGGBB`/`#RRGGBBAA`), `fromMinecraftCode(char)` (maps §-color code char to Color)
- `McUtils` - null-safe accessors: `mc`, `player`, `world`, `isInGame`, `isScreenOpen`, `playerPos`, `playerVec` (Vec3d foot position), `playerEyePos`, `isFirstPerson`
- `VecUtils` - extension functions on `Vec3d`, `BlockPos`, `Box`: `up/down`, `roundToBlock`, `blockCenter`, `distanceToPlayer/distanceSqToPlayer`, `distanceIgnoreY`, `middle`, `interpolate`, `boundingToOffset`, `axisAlignedTo`, `expandBlock`, `getBlockAt/getBlockStateAt`, `isInLoadedChunk`, `distanceToLine`; `BlockPos.toVec3d()`; `Box.center/topCenter/isPlayerInside/minVec/maxVec/corners`
- `StringUtils` - `removeColorCodes()`, `stripFormattingCodes()`, `isPlayerName()`, `widthInPixels()`, `splitToWidth(maxWidth)`
- `ChatUtils` - `print(String/Text)`, `send(String)`, `clickable(text, command, hover?)`, `hoverable(text, lines)`

---

## Technical Debt & Issues

### Low
1. **Missing documentation** - No KDoc comments on public APIs
2. **No example mod** - Would help demonstrate library usage

---

## Priority Roadmap

### P0 - Critical Fixes
- [x] Add `@Volatile` to `frame` and `blocked` in `KeybindRegistry.kt`
- [x] Remove or fix autoloader reference in `EventBus.kt`

### P1 - Complete Config System
- [x] Implement JSON file persistence (`ConfigSerializer` + `ConfigManager`)
- [x] Build reflection-based annotation processor (`ConfigProcessor`)
- [x] Create GUI renderer with all field types (`ConfigScreen` + `ColorEditScreen`)
- [x] Dynamic dropdown options (`options: () -> List<String>` lambda)
- [x] Conditional categories (`CategoryCondition`, `@Category(condition = ...)`, live per-frame eval)
- [x] Runtime entry visibility (`ProcessedEntry.visible: () -> Boolean`)
- [x] Dropdown-in-list (`@EditorMutable(dropdownValues = [...])`, `MutableListEntry.ElementType.DROPDOWN`)
- [x] Object list entries (`@EditorObjectList`, `ObjectListOverlay`, `ElementEditOverlay`)
- [x] Programmatic model construction via `ProcessedEntry` sealed class hierarchy

### P2 - New Features
- [x] Command system implementation
- [ ] Expand Minecraft-specific argument types (entity selector, block position, NBT, vec3)
- [x] UI library with moveable overlays
- [ ] Utils module with common helpers

### P3 - Polish
- [ ] Add KDoc documentation
- [ ] Write tests: list types, default values, collapsed state, cancellation semantics, concurrent register/unregister
