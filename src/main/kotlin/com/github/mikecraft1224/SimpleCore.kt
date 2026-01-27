package com.github.mikecraft1224

import com.github.mikecraft1224.bus.EventBus
import com.github.mikecraft1224.bus.FeatureAutoLoader
import com.github.mikecraft1224.bus.api.Feature
import com.github.mikecraft1224.input.KeybindRegistry
import com.github.mikecraft1224.input.api.ConfigKeybind
import com.github.mikecraft1224.input.api.KeyContext
import com.github.mikecraft1224.input.api.Modifiers
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.option.KeyBinding
import org.lwjgl.glfw.GLFW
import java.util.*

@Feature
object SimpleCore : ClientModInitializer {
	val EVENTBUS = EventBus()

	override fun onInitializeClient() {
		println("Hello this is ${BuildConfig.MOD_NAME} v${BuildConfig.MOD_VERSION}")

		// Event Bus
		FeatureAutoLoader.loadOptInPackages(EVENTBUS)

		KeybindRegistry.registerVanilla(
			"Test Keybind",
			KeyBinding.Category.MISC,  // Use built-in category, or create custom with KeyBinding.Category.create(Identifier.of("modid", "name"))
			GLFW.GLFW_KEY_UNKNOWN,
			EnumSet.of(KeyContext.ANY),
			Modifiers(ctrl = true, shift = false, alt = false),
			onPress = { println("Test Keybind Pressed") },
			onRelease = { println("Test Keybind Released") },
			onHold = { _, _ -> println("Test Keybind Held") },
			onHandledScreen = { _, _ -> println("Test Keybind Pressed in Handled Screen") },
		)

		KeybindRegistry.registerVirtual(
			"Test Virtual Keybind",
			ConfigKeybind(GLFW.GLFW_KEY_B, Modifiers(ctrl = false, shift = false, alt = false)),
			EnumSet.of(KeyContext.ANY),
			onPress = { println("Test Virtual Keybind Pressed") },
			onRelease = { println("Test Virtual Keybind Released") },
			onHold = { _, _ -> println("Test Virtual Keybind Held") },
			onHandledScreen = { _, _ -> println("Test Virtual Keybind Pressed in Handled Screen") },
		)
	}
}

