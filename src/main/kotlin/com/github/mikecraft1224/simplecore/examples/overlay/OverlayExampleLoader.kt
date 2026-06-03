package com.github.mikecraft1224.simplecore.examples.overlay

import com.github.mikecraft1224.simplecore.SimpleCore
import com.github.mikecraft1224.simplecore.input.KeybindRegistry
import com.github.mikecraft1224.simplecore.input.api.KeyContext
import com.github.mikecraft1224.simplecore.input.api.KeyDescriptor
import com.github.mikecraft1224.simplecore.overlay.HudManager
import org.lwjgl.glfw.GLFW

/**
 * Loads the overlay examples when [com.github.mikecraft1224.simplecore.examples.Examples.overlay] is true.
 *
 * Registers:
 * - [SessionTracker] with the EventBus (for ClientTickEvent) and HudManager (for rendering)
 * - A virtual keybind (default: **O**) to open the overlay editor in-game
 */
object OverlayExampleLoader {

    fun register() {
        // Register for tick events (ClientTickEvent is already set up by FeatureAutoLoader
        // via KeybindRegistry, which is a @Feature that also subscribes to ClientTickEvent).
        SimpleCore.EVENTBUS.registerFeature(SessionTracker)
        // RenderHudEvent is dispatched by HudManager (@Feature), auto-loaded by FeatureAutoLoader.
        HudManager.register(SessionTracker)

        KeybindRegistry.registerVirtual(
            id = "simplecore.overlay_editor",
            key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_O),
            KeyContext.IN_GAME,
            onPress = { _ -> HudManager.openEditor() },
        )
    }
}
