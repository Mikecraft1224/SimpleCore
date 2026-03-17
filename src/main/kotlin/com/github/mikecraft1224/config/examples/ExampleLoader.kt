package com.github.mikecraft1224.config.examples

import com.github.mikecraft1224.config.ConfigManager
import com.github.mikecraft1224.config.ConfigProcessor
import com.github.mikecraft1224.config.KeybindPacked
import com.github.mikecraft1224.config.screen.ConfigScreen
import com.github.mikecraft1224.input.KeybindRegistry
import com.github.mikecraft1224.input.api.KeyContext
import com.github.mikecraft1224.input.api.KeyDescriptor
import com.github.mikecraft1224.input.api.Modifiers
import net.minecraft.client.option.KeyBinding
import org.lwjgl.glfw.GLFW

/**
 * Loads all example config machinery when [SimpleCore.enableExamples] is true.
 *
 * Move this registration into your own mod's init if you want to replicate the
 * example setup in a downstream project.
 */
object ExampleLoader {

    val config = TestConfig()
    val manager = ConfigManager.of(config, "simplecore-test")

    /**
     * Registers the example TestConfig, its ConfigManager, and the keybind to open
     * the config screen. Call this from [com.github.mikecraft1224.SimpleCore.onInitializeClient]
     * only when [com.github.mikecraft1224.SimpleCore.enableExamples] is true.
     */
    fun register() {
        manager.load()
        val model = ConfigProcessor.process(config)

        val configOpenHandle = KeybindRegistry.registerVirtual(
            id = "simplecore.example_config_open",
            key = KeyDescriptor.keyboard(KeybindPacked.keyCode(config.configKey)),
            KeyContext.IN_GAME,
            onPress = { client ->
                client.setScreen(ConfigScreen(null, model, manager))
            },
        )

        // Bind the config field so GUI key changes update the runtime binding immediately
        configOpenHandle.bindConfigField(model, config::configKey)

        KeybindRegistry.registerVanilla(
            id = "key.simplecore.test",
            category = KeyBinding.Category.MISC,
            defaultKey = KeyDescriptor(modifiers = Modifiers(ctrl = true)),
            KeyContext.ANY,
            onPress = { println("Test Keybind Pressed") },
            onRelease = { println("Test Keybind Released") },
            onHold = { _, _ -> println("Test Keybind Held") },
            onHandledScreen = { _, _ -> println("Test Keybind Pressed in Handled Screen") },
        )

        KeybindRegistry.registerVirtual(
            id = "simplecore.test_virtual",
            key = KeyDescriptor.keyboard(GLFW.GLFW_KEY_B),
            KeyContext.ANY,
            onPress = { println("Test Virtual Keybind Pressed") },
            onRelease = { println("Test Virtual Keybind Released") },
            onHold = { _, _ -> println("Test Virtual Keybind Held") },
            onHandledScreen = { _, _ -> println("Test Virtual Keybind Pressed in Handled Screen") },
        )
    }
}
