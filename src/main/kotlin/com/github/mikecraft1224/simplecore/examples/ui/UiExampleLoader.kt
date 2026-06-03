package com.github.mikecraft1224.simplecore.examples.ui

import com.github.mikecraft1224.simplecore.input.KeybindRegistry
import com.github.mikecraft1224.simplecore.input.api.KeyContext
import com.github.mikecraft1224.simplecore.input.api.KeyDescriptor
import org.lwjgl.glfw.GLFW

/**
 * Loads the UI example screen when `SimpleCore.examples.ui` is true.
 *
 * Registers a virtual keybind (default: Home) that opens [ExampleScreen] from in-game.
 */
object UiExampleLoader {

    fun register() {
        KeybindRegistry.registerVirtual(
            id = "simplecore.example_ui_open",
            key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_HOME),
            KeyContext.IN_GAME,
            onPress = { client ->
                client.setScreen(ExampleScreen(null))
            },
        )
    }
}
