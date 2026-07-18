package com.github.mikecraft1224.simplecore.examples

import com.github.mikecraft1224.simplecore.examples.commands.CommandExampleLoader
import com.github.mikecraft1224.simplecore.examples.config.ConfigExampleLoader
import com.github.mikecraft1224.simplecore.examples.debug.DebugOverlayExampleLoader
import com.github.mikecraft1224.simplecore.examples.overlay.OverlayExampleLoader
import com.github.mikecraft1224.simplecore.examples.render.WorldRenderExampleLoader

/**
 * Controls which built-in examples are loaded on startup.
 *
 * Configure via [com.github.mikecraft1224.simplecore.SimpleCore.examples] before [com.github.mikecraft1224.simplecore.SimpleCore.onInitializeClient] runs:
 * ```kotlin
 * SimpleCore.examples.config = true
 * SimpleCore.examples.command = true
 * SimpleCore.examples.render = true
 * SimpleCore.examples.overlay = true
 * ```
 */
class Examples {

    /** Load the example config screen (default key: Insert). */
    var config: Boolean = false

    /** Load the world render example (draws shapes around the player each frame). */
    var render: Boolean = false

    /** Load the command example (`/sc` and `/simplecore`). */
    var command: Boolean = false

    /** Load the overlay example — a draggable session-stats HUD (open editor with O key). */
    var overlay: Boolean = false

    /** Load the debug overlay example — an internal-state HUD, hidden by default (see [com.github.mikecraft1224.simplecore.examples.debug.DebugOverlayExample.enabled]). */
    var debug: Boolean = false

    internal fun load() {
        if (config)  ConfigExampleLoader.register()
        if (render)  WorldRenderExampleLoader.register()
        if (command) CommandExampleLoader.register()
        if (overlay) OverlayExampleLoader.register()
        if (debug)   DebugOverlayExampleLoader.register()
    }
}
