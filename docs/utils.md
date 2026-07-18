[← Back to README](../README.md)

# Utils

Small, self-contained helpers for things almost every mod built on SimpleCore needs. Everything here is a plain function or object - no event-bus wiring required, except where noted.

---

## McUtils

Null-safe accessors for common Minecraft client singletons. All properties must be read from the client thread only.

```kotlin
McUtils.mc            // Minecraft.getInstance()
McUtils.player         // LocalPlayer?
McUtils.world          // ClientLevel?
McUtils.isInGame       // player != null && world != null
McUtils.isScreenOpen   // ScreenTracker.currentScreen != null
McUtils.playerPos      // BlockPos? (foot block)
McUtils.playerVec      // Vec3? (foot position)
McUtils.playerEyePos   // Vec3? (eye position)
McUtils.isFirstPerson
```

---

## Time and scheduling

**`TimeMark`** - a millis-based point in time, for cooldowns and "how long since X" checks without hand-rolling a raw `Long` field:

```kotlin
var lastUse = TimeMark.FAR_PAST   // always in the past - safe initial value

fun tryUse() {
    if (lastUse.passedSince() < 2.seconds) return
    lastUse = TimeMark.now()
    // ... do the thing
}

TimeMark.now()
TimeMark.future(5.seconds)   // a mark 5 seconds from now
mark.isInPast() / mark.isInFuture()
mark + 2.seconds             // arithmetic with kotlin.time.Duration
mark2 - mark1                // Duration between two marks
```

**`Scheduler`** - runs a task after a tick or wall-clock delay, driven by a self-registered tick hook (no setup required):

```kotlin
Scheduler.runDelayed(20) { ChatUtils.print("1 second later (tick-based)") }
Scheduler.runDelayed(2.seconds) { ChatUtils.print("2 seconds later (wall-clock)") }
Scheduler.runNextTick { /* ... */ }
```

---

## Numbers and text

**`NumberUtils`** - display formatting:

```kotlin
1_234_567L.shortFormat()      // "1.23M"
1_234_567.shortFormat(1)      // "1.2M"
3.14159.roundTo(2)            // 3.14 (Double)
3.14159.toFixed(2)            // "3.14" (String)
1234567L.addSeparators()      // "1,234,567"
14.toRomanNumeral()           // "XIV" (1..3999 only)
```

**`StringUtils`**:

```kotlin
str.removeColorCodes()         // strip §X sequences
str.stripFormattingCodes()     // alias for removeColorCodes
str.isPlayerName()             // matches [A-Za-z0-9_]{3,16}
str.widthInPixels()            // Minecraft font pixel width
str.splitToWidth(maxWidth)     // word-wrap to a List<String>
```

**`RegexUtils`** - chat-parsing ergonomics over raw `java.util.regex`:

```kotlin
val pattern = Pattern.compile("Welcome, (?<name>\\w+)!")

pattern.matchMatcher(message)?.let { m -> ChatUtils.print("Hi, ${m.group("name")}") }
pattern.matchMatcher(component)   // overload taking a chat Component directly
pattern.find(message)             // Boolean - matches anywhere in the string
pattern.matchesAny(listOf("a", "b", "c"))
matcher.groupOrNull("name")       // null instead of throwing for a missing/unmatched group
```

---

## Chat

```kotlin
ChatUtils.print("§aMessage")           // inject into local chat (not sent to server)
ChatUtils.print(text)                  // overload accepting a Component
ChatUtils.send("/command")             // send chat message (prefix / for commands)
ChatUtils.clickable(text, command, hover?)  // Component with a RunCommand click event
ChatUtils.hoverable(text, lines)           // Component with a multi-line hover tooltip
```

---

## TabList & Scoreboard

**`TabListUtils`** - the Tab-key player list:

```kotlin
TabListUtils.header     // Component? - cached from the last server update
TabListUtils.footer     // Component?
TabListUtils.entries    // Collection<PlayerInfo> - read live, not cached
TabListUtils.entryNames // List<String> - entries' display names as plain strings
```

Subscribe to `TabListEvent` instead if you need to react to header/footer changes rather than poll:

```kotlin
@Subscribe
fun onTabList(event: TabListEvent) {
    ChatUtils.print("Footer changed: ${event.footer?.string}")
}
```

**`ScoreboardUtils`** - the sidebar scoreboard, pure live polling (no event, vanilla already keeps it current):

```kotlin
ScoreboardUtils.title       // String? - sidebar objective's title
ScoreboardUtils.lines       // List<PlayerScoreEntry>, sorted by score descending
ScoreboardUtils.lineTexts   // List<String> - the "owner" field with color codes stripped
                             // (most servers put the actual line text in "owner", not a display name)
```

---

## Sound

```kotlin
McUtils.playSound(SoundEvents.UI_BUTTON_CLICK, volume = 1f, pitch = 1f)  // works for both
McUtils.playSound(SoundEvents.ANVIL_LAND)                                 // SoundEvent and Holder<SoundEvent>
McUtils.playSoundAt(pos, SoundEvents.ANVIL_LAND, volume = 1f, pitch = 1f, category = SoundSource.PLAYERS)

// Named presets for common feedback cues:
SoundUtils.playClick()
SoundUtils.playError()
SoundUtils.playSuccess()
```

---

## Entities

**`EntityUtils`** - reified nearest/list helpers built on `Level.getEntitiesOfClass`:

```kotlin
nearestEntity<Mob>(32.0)                       // nearest of type T within radius, or null
nearestEntity<Mob>(32.0) { !it.isRemoved }      // with a predicate
entitiesNear<Player>(16.0)                     // all matches, not just nearest
nearestEntityTo<Mob>(somePos, 10.0)             // nearest to an arbitrary point, not the player
```

**`RaycastUtils`** - line-of-sight and crosshair targeting, built on vanilla's own raycast machinery rather than reimplemented ray-AABB math:

```kotlin
raycastBlocks(from, to)          // BlockHitResult? - terrain-only ray, HitResult.Type.MISS if clear
canSee(from, to)                 // Boolean - true if no block blocks the line
canSee(entity)                   // Boolean - player eye position to entity's hitbox center

entityLookingAt<Mob>(maxDistance = 20.0)               // what the player's crosshair is on
entityLookingAt<Mob>(20.0) { it.health > 0 }           // with a predicate
```

`entityLookingAt` ignores terrain occlusion - combine with `canSee(entity)` if you need both.

---

## Movement and rotation

**`RotationUtils`** - reads and sets the player's *real* yaw/pitch. Every function here changes what's
actually rendered and what's actually sent to the server, exactly as if the player moved the mouse - there is
no fake-rotation mode that tells the server something different from what the client sees (that's a
hit-validation bypass most servers treat as cheating, and this framework doesn't provide it).

```kotlin
RotationUtils.yaw                        // current real yaw
RotationUtils.pitch                      // current real pitch

RotationUtils.set(yaw, pitch)            // instant, exact
RotationUtils.setYaw(yaw)                // keeps current pitch
RotationUtils.setPitch(pitch)            // keeps current yaw

RotationUtils.snapYaw(increment = 45f)   // snap current yaw to the nearest 45° (or 90°, etc)
RotationUtils.snapPitch(increment = 45f)
RotationUtils.resetPitch()               // pitch -> 0, level with the horizon

RotationUtils.nearestCardinal()          // Direction - N/S/E/W closest to current yaw
RotationUtils.snapToCardinal()           // snap yaw to that direction
RotationUtils.face(Direction.NORTH)      // turn to face an exact cardinal direction

RotationUtils.angleTo(from, to)          // Pair<Float, Float> - yaw/pitch to look from one point at another
RotationUtils.lookAt(target)             // instantly face a Vec3
RotationUtils.smoothTurnTo(yaw, pitch, ticks = 10)   // interpolated turn over N ticks (shortest path)
RotationUtils.smoothLookAt(target, ticks = 10)
```

**`MovementUtils`** - simulates real WASD/jump/sprint/sneak input. Drives the same `ClientInput` state real
key presses do, so movement physics and server-side validation see it exactly like genuine input. There's no
position/velocity teleportation helper - only input simulation.

```kotlin
MovementUtils.setInput(forward = true, sprint = true)   // holds keys until changed again
MovementUtils.walkForward(sprint = true)                 // shorthand for the common case
MovementUtils.stopMovement()                             // release all movement keys
MovementUtils.jump()                                     // a single jump, like tapping the key once
```

---

## Interaction

**`InteractionUtils`** - simulates the player's own left-click, right-click, and hotbar actions: the same
client-to-server calls vanilla's own mouse/key handling makes, just triggered programmatically. Every function
is a no-op if the player or game mode isn't loaded.

```kotlin
InteractionUtils.attack(entity)                       // left-click attack
InteractionUtils.interact(entity, hand = InteractionHand.MAIN_HAND)   // right-click an entity (trade, mount, etc)

InteractionUtils.useItem(hand = InteractionHand.MAIN_HAND)            // right-click in air (eat, drink, open a book)
InteractionUtils.useItemOnBlock(hitResult, hand)                      // right-click a specific block hit
InteractionUtils.useItemOnBlock(pos, face = Direction.UP, hand)       // convenience overload, builds the hit for you

InteractionUtils.breakBlockInstant(pos)               // creative mode only - instant break
InteractionUtils.startBreaking(pos, face)             // survival mining: call once to start...
InteractionUtils.continueBreaking(pos, face)          // ...once per tick while mining...
InteractionUtils.stopBreaking()                       // ...and once when done/cancelled

InteractionUtils.selectHotbarSlot(slot)               // 0-8, same as pressing the number key
InteractionUtils.swapToHotbar(containerSlot, hotbarSlot)  // swap any open menu's slot with a hotbar slot -
                                                            // also works against the player's own inventory

InteractionUtils.pickBlock(pos)                       // middle-click pick-block
InteractionUtils.pickEntity(entity)                   // middle-click pick-block on an entity (e.g. spawn egg)
```

Typical pairing with [`RaycastUtils`](#entities) - act on whatever the player is currently looking at:

```kotlin
val target = entityLookingAt<Mob>(maxDistance = 4.0) ?: return
InteractionUtils.attack(target)
```

`useItemOnBlock`/`startBreaking`/`continueBreaking` take a `Direction` (the block face) - use the `direction`
from a `BlockHitResult` (e.g. from `raycastBlocks(...)`) when acting on whatever the player is actually looking at,
rather than always defaulting to `Direction.UP`.

---

## Items and inventory

**`ItemUtils`** - extensions on `ItemStack` for the 26.2 data-component model:

```kotlin
stack.loreLines()             // List<String> - lore text, color codes preserved
stack.customNameOrNull()      // String? - only if explicitly set (anvil rename, NBT)
stack.displayNameString()     // String - always non-null (custom name or default translated name)
stack.customData()            // CompoundTag - the minecraft:custom_data component, empty if none
stack.enchantments()          // ItemEnchantments - regular, falling back to stored (e.g. enchanted books)
stack.loreContains("Legendary")  // Boolean, case-insensitive, color codes ignored
```

**`InventoryUtils`** - reading the currently open container screen:

```kotlin
openContainerMenu             // AbstractContainerMenu? - null if no container GUI is open
openContainerItem(slotIndex)  // ItemStack, ItemStack.EMPTY if none/closed
openContainerItems()          // List<ItemStack> - all non-empty slots, including the player's own inventory
```

---

## Debug tooling

`DebugOverlayExample` is an opt-in HUD showing SimpleCore's own internal state - useful while developing a mod on top of SimpleCore to sanity-check that your features actually wired up:

```kotlin
SimpleCore.examples.debug = true    // registers the overlay (hidden by default)
DebugOverlayExample.enabled = true  // show it
SimpleCore.EVENTBUS.debugMode = true // optional: populate the recent-posts counter (EventBusMonitor)
```

Shows: registered event classes, recent dispatch count and cancellation count (when `debugMode` is on), registered/active overlay counts, and registered keybind count.

---

## VecUtils

Extension functions on `Vec3`, `BlockPos`, and `AABB`.

**Vec3:**

| Function | Description |
|---|---|
| `up(n)` / `down(n)` | Offset Y by ±n (default 1.0) |
| `roundToBlock()` | Floor all components |
| `blockCenter()` | Floor X/Z, add 0.5 to X/Z (top-face center of the block column) |
| `distanceToPlayer()` / `distanceSqToPlayer()` | Distance to the local player's foot position |
| `distanceIgnoreY(other)` / `distanceSqIgnoreY(other)` | Horizontal-only distance |
| `middle(other)` | Midpoint between two vectors |
| `interpolate(other, t)` | Linear interpolation (delegates to `Vec3.lerp`) |
| `boundingToOffset(dx, dy, dz)` | Create an `AABB` from `this` to `this + offset` |
| `axisAlignedTo(other)` | Smallest `AABB` spanning two points |
| `expandBlock(n)` | 1×1×1 block-sized `AABB` around this position inflated by `n/16` |
| `getBlockStateAt()` / `getBlockAt()` | Query the current world |
| `isInLoadedChunk()` | Returns false when the chunk is not loaded |
| `distanceToLine(start, end)` | Distance from this point to a line segment |

**BlockPos:**

| Function | Description |
|---|---|
| `toVec3d()` | Block center as `Vec3` (x+0.5, y, z+0.5) |
| `getBlockStateAt()` / `getBlockAt()` | Query the current world |

**AABB:**

| Function | Description |
|---|---|
| `expandBlock(n)` | Inflate by `n/16` |
| `center()` | Center of the box |
| `topCenter()` | Center of the top face |
| `isPlayerInside()` | Containment check against the local player |
| `minVec()` / `maxVec()` | Min/max corners as `Vec3` |
| `corners()` | All 8 corners as a `List<Vec3>` |

---

## Color

```kotlin
Color(r, g, b, a)              // ARGB data class
Color.fromHex("#RRGGBB")       // parse hex string
Color.fromArgb(argb) / Color.fromRgb(rgb, alpha?)
Color.fromMinecraftCode('a')   // map §-color code char to Color (0–9, a–f)
color.argb                     // packed ARGB int
color.rgb                      // packed RGB int (alpha ignored)
color.toHex()                  // serialize to "#RRGGBB" or "#RRGGBBAA"
color.withAlpha(a)             // Int (0-255) or Float (0-1) overload
color.darker(factor) / color.lighter(factor)
color.blend(other, t)
color.toAwtColor() / Color.fromAwtColor(c)
```

Named constants: `Color.WHITE`, `BLACK`, `RED`, `GREEN`, `BLUE`, `YELLOW`, `CYAN`, `TRANSPARENT`.
