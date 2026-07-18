package com.github.mikecraft1224.simplecore.examples.debug

import com.github.mikecraft1224.simplecore.overlay.HudManager

/**
 * Loads the debug overlay example when `SimpleCore.examples.debug` is true.
 *
 * Starts hidden - set [DebugOverlayExample.enabled] to `true` to show it.
 */
object DebugOverlayExampleLoader {
    fun register() {
        HudManager.register(DebugOverlayExample)
    }
}
