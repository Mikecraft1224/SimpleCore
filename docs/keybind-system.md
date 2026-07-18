[← Back to README](../README.md)

# Keybind System

`KeybindRegistry` manages both vanilla (Minecraft options screen) and virtual (runtime-only) keybinds. Both registration methods return a `KeybindHandle` for lifecycle control.

---

## Registering keybinds

**Vanilla keybind** - appears in Minecraft's controls screen, player-remappable:

```kotlin
val handle: KeybindHandle = KeybindRegistry.registerVanilla(
    id = "key.mymod.sprint",          // must follow key.<modid>.<action> convention
    category = KeyMapping.Category.MOVEMENT,
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

## Key descriptors

`KeyDescriptor` wraps an `InputConstants.Key` (Minecraft's own key type) and optional modifiers:

```kotlin
KeyDescriptor.keyboard(GLFW.GLFW_KEY_F)                          // plain keyboard key
KeyDescriptor.keyboard(GLFW.GLFW_KEY_F, Modifiers(shift = true)) // Shift+F
KeyDescriptor.mouse(GLFW.GLFW_MOUSE_BUTTON_4)                    // mouse side button
KeyDescriptor()                                                   // unbound (InputConstants.UNKNOWN)
KeyDescriptor.from(config.myKeybind)                              // from a config Keybind value
```

`Modifiers` supports `ctrl`, `shift`, and `alt` booleans.

---

## Contexts and modifiers

`context` accepts one or more `KeyContext` values as a `vararg`. Passing nothing defaults to `ANY`.

| Context | Fires when |
|---|---|
| `ANY` | Always (default) |
| `IN_GAME` | No screen open |
| `IN_ANY_SCREEN` | Any non-chat screen is open, including inventory/container screens |
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

## Callbacks

| Callback | Signature | When called |
|---|---|---|
| `onPress` | `(Minecraft) -> Unit` | Leading edge - key goes down |
| `onRelease` | `(Minecraft) -> Unit` | Trailing edge - key comes up |
| `onHold` | `(Minecraft, Int) -> Unit` | Every `holdEveryTicks` ticks while held; second arg is hold tick count |
| `onHandledScreen` | `(Minecraft, Slot) -> Unit` | Immediate key-press inside a container screen; second arg is the hovered slot |

All callbacks are optional and default to no-ops.

---

## KeybindHandle - lifecycle control

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

> **Note:** Unregistering a vanilla keybind removes it from SimpleCore's dispatch but does not remove it from Minecraft's keybinding options screen - Fabric provides no API for that. The `KeyMapping` will continue to appear in the options menu for the rest of the session.

</details>

---

## Binding a virtual keybind to a config field

A virtual keybind's key can be tied directly to a `Keybind`-typed config field, so changing it in the config screen updates the live binding immediately (no restart, no manual wiring):

```kotlin
val handle = KeybindRegistry.registerVirtual(
    id  = "mymod.open_config",
    key = KeyDescriptor.keyboard(config.configKey.keyCode),
    KeyContext.IN_GAME,
    onPress = { client ->
        val model = ConfigProcessor.process(config).applyKeybindBindings()
        client.setScreenAndShow(ConfigScreen(null, model, manager))
    },
).withConfigBinding(config::configKey)
```

`withConfigBinding` stores the property reference on the handle; `ProcessedConfig.applyKeybindBindings()` (call it right after `ConfigProcessor.process`) matches every pending binding to its `ProcessedEntry.KeybindEntry` by field name and wires it up - so this works without holding onto the handle variable anywhere else.

For a one-off bind without the pending-list mechanism, wire a specific processed entry directly:

```kotlin
val model = ConfigProcessor.process(config)
handle.bindConfigField(model, config::configKey)   // or bindConfigFieldByName(model, "configKey")
```

Only works with virtual keybinds - vanilla keybinds backed by `KeyMapping` cannot be rebound programmatically mid-session.

---

## Blocking

<details>
<summary>Global and context-level input suppression</summary>

```kotlin
// Pause all keybind processing (releases all pressed keys):
KeybindRegistry.blockKeybind()
KeybindRegistry.unblockKeybind()

// Suppress specific contexts (e.g., during a cutscene):
KeybindRegistry.blockContext(KeyContext.IN_GAME)
KeybindRegistry.unblockContext(KeyContext.IN_GAME)
KeybindRegistry.getBlockedContexts()  // current suppressed set

// Per-keybind suppression via the handle:
handle.block()
handle.unblock()
```

</details>
