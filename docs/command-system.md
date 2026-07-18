[← Back to README](../README.md)

# Command System

SimpleCore provides a Kotlin DSL for registering client-side chat commands that wraps Brigadier's verbose builder API. Commands are defined with a tree of `literal` and `argument` nodes, automatically appear in tab completion, and return a `CommandHandle` for runtime enable/disable.

---

## Registering a command

```kotlin
CommandRegistry.client("mymod") {
    executes { sendFeedback("Hello from /mymod!") }

    literal("reload") {
        executes {
            manager.reload()
            sendFeedback("Config reloaded.")
        }
    }

    literal("give") {
        argument("item", word()) {
            argument("count", integer(1, 64)) {
                executes {
                    sendFeedback("Giving ${int("count")}x ${string("item")}")
                }
            }
        }
    }
}
```

Import argument factory functions from `com.github.mikecraft1224.simplecore.command.api`:
`integer()`, `longArg()`, `floatArg()`, `doubleArg()`, `bool()`, `word()`, `string()`, `greedyString()`, `identifier()`, `color()`.

Retrieve argument values in `executes {}` using the matching accessor on the `ClientCommandContext` receiver: `int(name)`, `string(name)`, `bool(name)`, `identifier(name)`, etc.

For argument types not covered by the built-in accessors:
```kotlin
argument("nbt", NbtCompoundArgumentType.nbtCompound()) {
    executes {
        val tag = get<CompoundTag>("nbt")   // generic reified getter
    }
}
```

### Shorthand callbacks

Skip the nested `executes {}` block for simple nodes:

```kotlin
literalCallback("reload", "rl") { manager.reload(); sendFeedback("Reloaded.") }

argCallback("give item", word()) { sendFeedback("Item: ${string("item")}") }

// A node that needs no ClientCommandContext at all:
literal("ping") { simpleCallback { pingServer() } }
```

---

## Aliases

```kotlin
val handle = CommandRegistry.client("simplecore", "sc") {
    executes { sendFeedback("SimpleCore ${BuildConfig.MOD_VERSION}") }
}
```

All aliases share the same `CommandHandle`.

---

## Tab completion

Literal subcommand names are automatically suggested by Brigadier. Override suggestions for argument nodes with `suggests {}`:

```kotlin
argument("mode", word()) {
    suggests { _ ->
        listOf("fast", "normal", "slow").forEach { suggest(it) }
    }
    executes { sendFeedback("Mode: ${string("mode")}") }
}
```

The block receives `SuggestionsBuilder` as receiver and the `CommandContext` as a parameter. Brigadier filters by the prefix the user has already typed - `suggest()` everything, Brigadier does the filtering.

For fixed or runtime-computed option lists, `suggestStatic`/`suggestDynamic` do the prefix-filtering for you:

```kotlin
argument("mode", word()) {
    suggestStatic("fast", "normal", "slow")
    executes { sendFeedback("Mode: ${string("mode")}") }
}

argument("player", word()) {
    suggestDynamic { McUtils.player?.connection?.onlinePlayers?.map { it.profile.name } ?: emptyList() }
    executes { sendFeedback("Player: ${string("player")}") }
}
```

---

## Permission guards

`requires {}` is evaluated for both tab completion and execution. Nodes whose `requires` returns `false` are hidden from the suggestion list entirely:

```kotlin
literal("admin") {
    requires { source -> source.player?.hasPermissionLevel(4) == true }
    executes { sendFeedback("Admin only.") }
}
```

---

## CommandHandle - runtime enable/disable

```kotlin
val handle = CommandRegistry.client("mymod") { ... }

handle.disable()  // hides from tab completion and blocks execution
handle.enable()   // restores
handle.isEnabled  // current state
handle.description // set via CommandBuilder.description(text) in the DSL block
```

Because `disable()` works via Brigadier's `requires`, disabled commands vanish from tab completion, not just execution.

---

## Splitting across files

Use `SubCommand` to define each subcommand group in its own file and wire them together at registration time:

```kotlin
// GiveCommand.kt
object GiveCommand : SubCommand {
    override fun CommandBuilder.register() {
        literal("give") {
            argument("item", word()) {
                executes { sendFeedback("Giving ${string("item")}") }
            }
        }
    }
}

// AdminCommand.kt - nested SubCommands work too
object AdminCommand : SubCommand {
    override fun CommandBuilder.register() {
        literal("admin") {
            requires { source -> source.player?.hasPermissionLevel(4) == true }
            install(AdminBanCommand)
            install(AdminKickCommand)
        }
    }
}

// Registration file
CommandRegistry.client("mymod") {
    install(GiveCommand)
    install(TeleportCommand)
    install(AdminCommand)
    // ... no limit on depth or width
}
```

There is no limit on how deep or wide the tree can be. `install()` is available at every level.

---

## Failing with a user-visible error

`fail(message)` throws a `CommandSyntaxException` which Brigadier shows to the player as a system message and halts execution:

```kotlin
executes {
    val count = int("count")
    if (count > 64) fail("Maximum count is 64.")
    // ...
}
```

Use `sendError(message)` instead when you want to report an error without aborting.

---

## Argument types reference

| Factory | Getter | Notes |
|---|---|---|
| `integer(min?, max?)` | `int(name)` | |
| `longArg(min?, max?)` | `long(name)` | |
| `floatArg(min?, max?)` | `float(name)` | |
| `doubleArg(min?, max?)` | `double(name)` | |
| `bool()` | `bool(name)` | Suggests `true` / `false` |
| `word()` | `string(name)` | Single word, no spaces |
| `string()` | `string(name)` | Quoted or single word |
| `greedyString()` | `string(name)` | Consumes rest of input; must be last |
| `identifier()` | `identifier(name)` | `namespace:path` resource location |
| `color()` | `color(name)` | Team color name - returns a `TeamColor` |
| Any `ArgumentType<T>` | `get<T>(name)` | Generic fallback for custom types |
