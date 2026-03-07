package com.github.mikecraft1224

import com.github.mikecraft1224.bus.EventBus
import com.github.mikecraft1224.bus.FeatureAutoLoader
import com.github.mikecraft1224.input.KeybindRegistry
import com.github.mikecraft1224.input.api.KeyContext
import com.github.mikecraft1224.input.api.KeyDescriptor
import com.github.mikecraft1224.input.api.Modifiers
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.option.KeyBinding
import org.lwjgl.glfw.GLFW

object SimpleCore : ClientModInitializer {
	val EVENTBUS = EventBus()

	override fun onInitializeClient() {
		println("Hello this is ${BuildConfig.MOD_NAME} v${BuildConfig.MOD_VERSION}")

		// Event Bus
		FeatureAutoLoader.scanAndRegister(EVENTBUS, listOf("com.github.mikecraft1224"))
		FeatureAutoLoader.loadOptInPackages(EVENTBUS)

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
