[← Back to README](../README.md)

# Multi-version builds (Stonecutter)

SimpleCore targets two Minecraft versions from one codebase using [Stonecutter](https://stonecutter.kikugie.dev/):
**26.1.2** (what most other mods currently target) and **26.2** (~2 weeks old at time of writing). The plan is to
keep adding a new version roughly every few months as Minecraft/Fabric updates land, dropping old ones once the
ecosystem catches up.

## How it works

Stonecutter keeps **one shared source tree** (`src/`) and a small preprocessor (`Stitcher`) that reads special
comments to decide which lines are active for which version. There's no separate copy of the code per version -
you edit `src/` once, and version-specific code lives inline, guarded by comments:

```kotlin
val client = Minecraft.getInstance()
//? if >= 26.2 {
val camera = client.gameRenderer.mainCamera()
//?} else {
/*val camera = client.gameRenderer.mainCamera
*///?}
```

Only the *active* branch is real, compiling code; the other branch is a `/* ... */` comment. **Switching the
active version physically rewrites `src/` in place**, commenting out the branch that no longer applies and
uncommenting the other one. This is the key thing to understand - there is no "build any version directly"
shortcut; you switch, then build.

## Everyday commands

```bash
# See which version is currently active (check stonecutter.gradle.kts, or just try building)
./gradlew build              # builds whichever version is currently active
./gradlew runClient          # launches the currently active version

# Switch the active version (rewrites src/ in place, processing all //? comments):
./gradlew "Set active project to 26.1.2"
./gradlew "Set active project to 26.2"

# Build/run a specific version WITHOUT permanently switching your working copy - see "One-off builds" below.
```

The task names contain spaces - quote them on the command line as shown.

## Before committing

`vcsVersion` (in `settings.gradle.kts`) is set to `26.2` - that's the version whose processed form is what git
tracks. If you've been working with `26.1.2` active, switch back before committing:

```bash
./gradlew "Reset active project"   # switches back to vcsVersion (26.2)
```

Committing while a different version is active would make the tracked `src/` show 26.1.2-shaped code with the
26.2 branches commented out - not wrong, exactly, but confusing for diffs and for anyone else pulling the repo.

## One-off builds without switching

To build/verify a specific version without leaving it as your active working copy, switch, build, then switch
back:

```bash
./gradlew "Set active project to 26.2"
./gradlew :26.2:build
./gradlew "Set active project to 26.1.2"   # or "Reset active project" to go back to vcsVersion
```

(`./gradlew :26.2:compileKotlin` etc. only compiles whatever is *currently* physically in `src/` - it does **not**
switch for you. Running it while 26.1.2 is active will just recompile 26.1.2-shaped code under the `:26.2` label.)

## Adding a new version later

When the next Minecraft version lands (~3 months out, historically):

1. Confirm it's on Fabric's [meta API](https://meta.fabricmc.net/v2/versions/game) and check
   [Fabric API's maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml) for a
   matching build.
2. Add the version string to `versions(...)` in `settings.gradle.kts`.
3. Add a `["<version>"]` table to `stonecutter.properties.toml` with at least `deps.fabric_api` for that version.
4. `./gradlew "Set active project to <new version>"`, then `./gradlew :<new version>:compileKotlin :<new version>:compileJava`
   and fix whatever breaks with `//? if` guards (see below). This is almost always small for adjacent
   point-releases - between 26.1.2 and 26.2 it was exactly 4 spots (see below), all one-line renames.
5. Decide whether to bump `vcsVersion` to the new version once you're primarily developing against it, and drop
   the oldest version from `versions(...)` once nothing still needs it.

## Writing a `//? if` guard

Real syntax used in this codebase - a full block, with the "off" branch written as a raw comment:

```kotlin
//? if >= 26.2 {
import net.minecraft.commands.arguments.TeamColorArgument
//?} else {
/*import net.minecraft.commands.arguments.ColorArgument
*///?}
```

- Condition operators: `>=`, `<`, `>`, `<=`, `==` against a version string (semver comparison, so `26.1.2 < 26.2` compares correctly).
- The `if` branch is real active code; the `else` branch is a `/* ... */` comment that gets uncommented when that
  branch becomes active. The closing `*///?}` on one line is intentional (end the comment, then close the guard).
- For a single line with no `else`, a bare `//? if <condition>` above one line of code also works (see SkyHanni's
  codebase for examples) - only reach for the block form when you need an alternate branch.
- For simple project-wide token renames (not structural changes), `stonecutter.gradle.kts` has a commented-out
  `stonecutter parameters { replacements { ... } }` block - prefer that over sprinkling `//? if` everywhere for a
  one-word rename that shows up in many places.

## Consuming SimpleCore locally from another mod

`maven-publish` is set up (`build.gradle.kts`) so downstream mods can depend on a locally-published copy while
both are in development, ahead of a real Modrinth release:

```bash
./gradlew :26.1.2:publishToMavenLocal
./gradlew "Set active project to 26.2"
./gradlew :26.2:publishToMavenLocal
./gradlew "Set active project to 26.1.2"   # switch back afterward
```

Each version publishes independently to `~/.m2` as `com.github.mikecraft1224.simplecore:simplecore:<mod.version>+<mc version>`
(e.g. `1.0.0+26.1.2` and `1.0.0+26.2`), same group/artifactId, disambiguated by the version string - so re-publish
whichever version(s) changed, no need to redo both every time. The consuming mod needs `mavenLocal()` in its own
`repositories {}` and a dependency on the matching version string for whichever MC version *it* targets.

This is local-only for now. Publishing to Modrinth proper later goes through the
[Minotaur](https://modrinth.github.io/minotaur/) Gradle plugin instead - Modrinth's maven
(already used here as a dependency source for Fabric API-adjacent artifacts) is read-only and isn't a valid
`maven-publish` target.

## Known version differences (26.1.2 → 26.2)

| API | 26.1.2 | 26.2 |
|---|---|---|
| Active camera | `GameRenderer.getMainCamera()` (Kotlin: `.mainCamera` property) | `GameRenderer.mainCamera()` (method) |
| Command color argument | `ColorArgument` / `ColorArgument.getColor(...)` → `ChatFormatting` | `TeamColorArgument` / `TeamColorArgument.getTeamColor(...)` → `TeamColor` |
| Closing a screen from `Gui`/`Hud` | not available - use `Minecraft.setScreen(screen)` | `Gui.setScreen(screen)` |

All three are guarded in `src/` already (`RenderWorldEvent.kt`, `command/api/Arguments.kt`,
`command/api/ClientCommandContext.kt`, `config/screen/ConfigScreen.kt`).
