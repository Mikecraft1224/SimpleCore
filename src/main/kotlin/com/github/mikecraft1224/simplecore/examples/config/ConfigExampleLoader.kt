package com.github.mikecraft1224.simplecore.examples.config

import com.github.mikecraft1224.simplecore.config.ConfigProcessor
import com.github.mikecraft1224.simplecore.config.ConfigManager
import com.github.mikecraft1224.simplecore.config.ProcessedConfig
import com.github.mikecraft1224.simplecore.config.screen.ConfigScreen
import com.github.mikecraft1224.simplecore.input.KeybindRegistry
import com.github.mikecraft1224.simplecore.input.api.KeyContext
import com.github.mikecraft1224.simplecore.input.api.KeyDescriptor
import com.github.mikecraft1224.simplecore.input.api.Modifiers
import com.github.mikecraft1224.simplecore.input.api.applyKeybindBindings
import net.minecraft.client.option.KeyBinding
import org.lwjgl.glfw.GLFW

/**
 * Loads the example config screen when `SimpleCore.examples.config` is true.
 *
 * Registers a [TestConfig] backed by a [ConfigManager], processes it into a [model] once at
 * startup, and registers keybinds that open the config screen.
 *
 * ### Keybind-to-config wiring
 *
 * Keybinds that should update a config entry when rebound call `withConfigBinding` at
 * registration time. This enqueues the binding without needing the processed model yet.
 * After all keybinds are registered, `applyKeybindBindings` is called once to wire every
 * pending binding to its entry in [model]. The `onPress` handlers then simply open the
 * screen with the already-built model - no re-processing on each open.
 *
 * This pattern lets keybind registrations be spread across any number of files or objects:
 * each one calls `withConfigBinding` independently, and a single `applyKeybindBindings`
 * call at the end of startup wires them all up at once.
 */
object ConfigExampleLoader {

    val config  = TestConfig()
    val manager = ConfigManager.of(config, "simplecore-test")
    lateinit var model: ProcessedConfig

    fun register() {
        manager.load()

        // Register all keybinds first - withConfigBinding enqueues the binding but does
        // not require the model yet, so registrations can be spread across any file.
        KeybindRegistry.registerVirtual(
            id = "simplecore.example_config_open",
            key = KeyDescriptor.from(config.configKey),
            KeyContext.IN_GAME,
            onPress = { client -> client.setScreen(ConfigScreen(null, model, manager)) },
        ).withConfigBinding(config::configKey)

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

        // Build the model once after all keybinds are registered.
        // applyKeybindBindings() drains all pending withConfigBinding() calls in one shot.
        // Visibility conditions declared via Visible(...) in the config class are wired automatically.
        model = ConfigProcessor.process(config).applyKeybindBindings()
    }
}
