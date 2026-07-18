[← Back to README](../README.md)

# Render System

SimpleCore provides two sets of rendering utilities: **world-space** helpers (3D shapes, lines, text drawn in the world via Minecraft's [Gizmos](https://github.com/FabricMC) debug-drawing API) and **HUD** helpers (2D elements drawn on the screen).

---

## World rendering

Subscribe to `RenderWorldEvent` and call the extension functions from `WorldRenderUtils.kt` and `WorldRenderShapes.kt`. Unlike older Minecraft versions, there's no matrices/vertex-consumer-provider plumbing here - `Gizmos` calls take plain world-space `Vec3` positions and vanilla handles camera-relative math and draw ordering internally, so handlers just call the extension functions directly.

```kotlin
@Feature
object WorldRenderer {
    @Subscribe
    fun onRender(event: RenderWorldEvent) {
        if (!McUtils.isInGame) return

        // Outlined box around a block position
        val box = AABB(10.0, 64.0, 10.0, 11.0, 65.0, 11.0)
        event.drawOutlinedBox(box, Color(255, 0, 0, 200))

        // Tracer line from the player's crosshair to a target
        event.drawTracer(Vec3(100.0, 64.0, 100.0), Color.WHITE)

        // Waypoint marker with a label
        event.drawWaypoint(Vec3(50.0, 64.0, 50.0), "Target", Color(0, 255, 100, 255))
    }
}
```

Register the event and feature in your mod initializer:

```kotlin
EventRegistry.addBus(RenderWorldEvent::class, SimpleCore.EVENTBUS)
RenderWorldEvent.registerEvents()
SimpleCore.EVENTBUS.registerFeature(WorldRenderer)
```

### `seeThroughBlocks`

Every draw function takes a `seeThroughBlocks: Boolean` parameter (default varies per function). When `true`, the shape is drawn on top of terrain (xray-style, always visible); when `false`, vanilla depth-tests it normally against the world.

### Tracers are crosshair-relative, not camera-relative

`drawTracer`/`drawGradientTracer` start from the player's eye position offset along their look direction (`forwardOffset`, default 2 blocks) rather than the raw camera position. A line drawn directly from the camera toward its own target point is collinear with the view axis and therefore invisible - every point along it projects to the same screen pixel under perspective. Starting from the look direction instead means the line only degenerates that way when the target happens to be dead-center in the crosshair.

### WorldRenderUtils reference

| Function | Description |
|---|---|
| `drawBox(box, color, style, seeThroughBlocks, lineWidth)` | Shorthand combining `FILLED`, `OUTLINED`, or `BOTH` (`BoxStyle`) |
| `drawFilledBox(box, color, seeThroughBlocks)` | Solid box |
| `drawOutlinedBox(box, color, seeThroughBlocks, lineWidth)` | Wireframe box |
| `drawBlockHighlight(pos, color, seeThroughBlocks)` | Slightly inset solid box for block highlights |
| `outlineTopFace(box, color, seeThroughBlocks, lineWidth, face)` | Outline just one face of a box |
| `fillFace(box, face, color, seeThroughBlocks)` | Fill one face of a box |
| `drawFaceRayWorld(origin, face, color, length, seeThroughBlocks)` | Directional indicator quad extending from a point |
| `draw3DLine(from, to, color, seeThroughBlocks, lineWidth)` | Single line segment |
| `drawTracer(to, color, seeThroughBlocks, lineWidth, forwardOffset)` | Line from the player's crosshair to a world position |
| `drawGradientLine(from, to, colorFrom, colorTo, seeThroughBlocks, lineWidth, segments)` | Line with a color gradient, approximated as short solid segments |
| `drawGradientTracer(to, colorFrom, colorTo, seeThroughBlocks, lineWidth, forwardOffset)` | Gradient tracer from the crosshair to a world position |
| `exactBoundingBox(entity)` | Interpolated (partial-tick-correct) entity bounding box |
| `drawEntityBox(entity, color, style, seeThroughBlocks, lineWidth)` | Box fitted to an entity's interpolated bounding box |

### WorldRenderShapes reference

| Function | Description |
|---|---|
| `drawCircle(center, radius, color, seeThroughBlocks, lineWidth)` | Horizontal wireframe ring |
| `drawFilledCircle(center, radius, color, seeThroughBlocks)` | Solid horizontal disc |
| `drawPolyline(points, color, seeThroughBlocks, lineWidth)` | Connected line segments through a list of points |
| `drawBezier(p1, control, p3, color, seeThroughBlocks, lineWidth, steps)` | Quadratic Bézier curve |
| `drawText(pos, text, scale, color, seeThroughBlocks)` | Billboard text label |
| `drawDynamicText(pos, text, baseScale, color, seeThroughBlocks, hideTooCloseAt, maxDistance)` | Billboard text that scales with camera distance and hides at extreme ranges |
| `drawWaypoint(pos, label, color, seeThroughBlocks, beacon, minimumAlpha)` | Small labeled pillar marker with distance-scaled alpha |
| `renderBeaconBeam(pos, color, height, radius, seeThroughBlocks)` | Simplified translucent vertical beam, a lightweight stand-in for vanilla's real beacon beam |
| `drawSphere(center, radius, color, seeThroughBlocks, stacks, slices, lineWidth)` | Wireframe sphere (latitude rings + longitude meridians) |
| `drawCylinder(base, radius, height, color, seeThroughBlocks, segments, lineWidth)` | Wireframe cylinder (two rims + vertical lines) |
| `drawPyramid(apex, baseCenter, baseRadius, color, seeThroughBlocks, lineWidth)` | Wireframe pyramid (square base + edges to apex) |

---

## Entity outline / glow highlighting

`RenderEntityOutlineEvent` fires once per frame with every entity currently loaded in the world. Call `event.highlight(entity, color)` to queue a colored outline - the same visual effect as the vanilla Glowing potion, always visible through walls (it reuses vanilla's own entity-outline post-process pass).

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

Needs the same one-time explicit wiring as `RenderWorldEvent`:

```kotlin
EventRegistry.addBus(RenderEntityOutlineEvent::class, SimpleCore.EVENTBUS)
RenderEntityOutlineEvent.registerEvents()
```

Queued colors take effect starting the next rendered frame (a one-frame lag, imperceptible in practice). There is no non-xray variant - the underlying mechanism is always see-through.

---

## HUD rendering

Subscribe to `RenderHudEvent` for 2D screen-space drawing. `HudRenderUtils.kt` provides `GuiGraphicsExtractor` extension functions that accept `Color` values and use width/height parameters instead of raw coordinates.

```kotlin
@Feature
object StatusHud {
    @Subscribe
    fun onHud(event: RenderHudEvent) {
        if (!McUtils.isInGame) return
        event.state.fillRect(10, 10, 100, 40, Color(0, 0, 0, 180))
        event.state.drawText(Minecraft.getInstance().font, "Health: 20", 14, 14, Color.WHITE)
    }
}
```

Register the event and feature in your mod initializer:

```kotlin
EventRegistry.addBus(RenderHudEvent::class, SimpleCore.EVENTBUS)
RenderHudEvent.registerEvents()
SimpleCore.EVENTBUS.registerFeature(StatusHud)
```

For most HUD elements, prefer the higher-level `HudElement`/`HudRenderable` API in [Overlay System](overlay-system.md) - it gives you draggable positioning, the editor, hover/click routing, and tooltips for free. Drop to raw `RenderHudEvent` + `HudRenderUtils` only when you need something `HudElement` can't express.

### GuiGraphicsExtractor extensions (HudRenderUtils.kt)

| Function | Description |
|---|---|
| `fillRect(x, y, width, height, color)` | Solid rectangle (width/height form instead of x1/y1/x2/y2) |
| `drawBorderedRect(x, y, width, height, fillColor, borderColor, borderWidth)` | Rectangle with a solid border |
| `drawText(font, text, x, y, color, shadow?)` | Draws text using a `Color` instead of a raw packed int |
| `drawCenteredText(font, text, centerX, y, color)` | Horizontally-centered text |
| `fillGradientRect(x, y, width, height, colorTop, colorBottom)` | Top-to-bottom gradient fill |
| `withScissor(x, y, width, height) { ... }` | Clips drawing to a region for the duration of the block; always pairs enable/disable scissor, even if the block throws - use for scrollable panels |

**Alpha matters.** `GuiGraphicsExtractor.text()` skips drawing entirely when the color's alpha is 0 - unlike older Minecraft versions where alpha 0 was treated as opaque. Always use a full `0xFFxxxxxx` (or a `Color` with `a = 255`), never a bare `0xxxxxxx` int.
