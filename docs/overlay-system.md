[← Back to README](../README.md)

# Overlay System

SimpleCore provides a HUD overlay framework for rendering repositionable panels on the in-game screen. The primary API is `HudElement` with a `buildContent()` method that returns composable `HudRenderable` elements. The framework handles layout, background, hover detection, click routing, tooltip rendering, and editor integration automatically.

---

## Quick start

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

Register in your loader using `HudGroup` (see below):

```kotlin
// In your mod's main object:
val OVERLAYS = HudGroup("My Mod")

// In your loader:
OVERLAYS.register(MyHud)

// From a keybind or command:
onPress = { _ -> OVERLAYS.openEditor { myConfig.save() } }
```

---

## HudGroup - per-mod overlay grouping

`HudGroup` groups your mod's overlays under a single name and opens a filtered editor that shows only your mod's elements. Without it, `HudManager.openEditor()` shows every overlay from every loaded mod in one screen.

```kotlin
// Create one group per mod - typically a top-level val:
val OVERLAYS = HudGroup("My Mod")

// Register elements into the group (delegates to HudManager internally):
OVERLAYS.register(KillTracker)
OVERLAYS.register(TimerHud)
OVERLAYS.register(MinimapHud)

// Open the editor showing only your mod's overlays:
OVERLAYS.openEditor { myConfig.save() }

// Show only overlays that are currently rendering (hide disabled/ghost ones):
OVERLAYS.openEditor(showOnlyActive = true) { myConfig.save() }
```

`HudGroup` does not register any keybind automatically. Wire `openEditor()` to whatever trigger suits your mod:

```kotlin
// Keybind:
KeybindRegistry.registerVirtual(
    id = "mymod.overlay_editor",
    key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_O),
    KeyContext.IN_GAME,
    onPress = { _ -> OVERLAYS.openEditor { myConfig.save() } },
)

// Or a command (better for features players rarely need):
CommandRegistry.client("mymod") {
    literal("editor") {
        executes { OVERLAYS.openEditor { myConfig.save() } }
    }
}
```

**`showOnlyActive`:** When your mod has many context-sensitive overlays (e.g. overlays that only appear in specific dungeons or game modes), pass `showOnlyActive = true` to hide ghost handles for currently-inactive elements, keeping the editor uncluttered.

---

## HudRenderable types

`buildContent()` returns a `List<HudRenderable>`. The framework stacks that list vertically. Wrap elements in `horizontal` to place them side by side.

| Factory | Description |
|---|---|
| `text(text, color?, shadow?)` | Plain text. Supports Minecraft format codes (`§x`). |
| `hoverable(text, color?, tooltip, shadow?)` | Text that shows a tooltip list on hover. |
| `clickable(text, color?, tooltip?, shadow?, onClick)` | Text with underline-on-hover and a click callback. `onClick` receives the GLFW button code (0=left, 1=right). |
| `selector(label, current, options, onChange)` | `§7Label §a[§eValue§a]` - left-click cycles forward, right-click backward. |
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
HudRenderable.custom(80, 6) { state, lx, ly ->
    state.fill(lx, ly, lx + 80, ly + 6, 0xFF333333.toInt())
    state.fill(lx, ly, lx + (80 * fraction).toInt(), ly + 6, 0xFF55FF55.toInt())
}
```

**Color values are packed ARGB ints, always include the alpha byte.** `GuiGraphicsExtractor.text()` skips drawing entirely when alpha is 0 (opposite of older Minecraft versions, where alpha 0 meant "opaque") - use `0xFFxxxxxx.toInt()`, never a bare `0xxxxxxx` literal.

---

## HudElement options

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

`buildContent()` is called once per frame and the result is cached. If a click arrives in the same frame the cached result is reused - it is safe to do moderate work here.

---

## OverlayPosition

```kotlin
class OverlayPosition(
    var x: Float = 10f,   // pixels from left edge
    var y: Float = 10f,   // pixels from top edge
    var scale: Float = 1f // uniform scale multiplier (0.5 – 5.0 in the editor)
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

`OverlayPosition` is a plain class. GSON serializes it automatically when stored in a `@Config` class - no extra annotation needed.

---

## Overlay editor

```kotlin
// Show all overlays from all mods:
HudManager.openEditor()
HudManager.openEditor { save() }                    // call save() on close

// Show only overlays that rendered this frame (hide disabled/ghost handles):
HudManager.openEditor(showOnlyActive = true) { save() }

// Preferred: use HudGroup to scope the editor to your mod's overlays:
OVERLAYS.openEditor { save() }
OVERLAYS.openEditor(showOnlyActive = true) { save() }
```

| Control | Action |
|---|---|
| **Drag** | Reposition the overlay |
| **Arrow keys** | Nudge selected overlay by 1 px |
| **Shift + Arrow keys** | Nudge by 10 px |
| **Scroll wheel** | Adjust scale (0.5× – 5.0×) |
| **Middle-click** | Reset overlay to its registration-time position and scale |
| **Escape** | Close |

Changes apply immediately to the `OverlayPosition`. Pass an `onClose` lambda to persist them.

Overlays whose `isEnabled()` returned `false` this frame still appear in the editor as ghost handles (dim, labelled "disabled") so they can be repositioned even while inactive. Pass `showOnlyActive = true` to hide them.

---

<details>
<summary>Advanced: raw renderAt</summary>

For cases where `HudElement` is too constraining, use `OverlayPosition.renderAt` directly and subscribe to `RenderHudEvent` yourself:

```kotlin
position.renderAt(event.state, event.state.guiWidth(), event.state.guiHeight(), "My HUD") { state ->
    // state already translated to the overlay's position + scale
    state.fill(0, 0, 80, 20, 0xAA000000.toInt())
    state.text(Minecraft.getInstance().font, "Hello", 4, 6, 0xFFFFFFFF.toInt(), true)
    OverlaySize(80, 20)  // must match what was drawn
}
```

The block must return `OverlaySize(width, height)` so the editor can draw a drag handle.

</details>
