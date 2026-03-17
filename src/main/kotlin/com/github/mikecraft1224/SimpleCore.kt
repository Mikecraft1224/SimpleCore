package com.github.mikecraft1224

import com.github.mikecraft1224.bus.EventBus
import com.github.mikecraft1224.bus.FeatureAutoLoader
import com.github.mikecraft1224.config.examples.ExampleLoader
import net.fabricmc.api.ClientModInitializer

object SimpleCore : ClientModInitializer {
	val EVENTBUS = EventBus()

	/**
	 * Set to true before [onInitializeClient] is called to load the example config,
	 * example keybinds, and the config-screen open keybind.
	 *
	 * Intended for developers evaluating the config system behavior.
	 * Leave false in production mods that embed SimpleCore.
	 */
	var enableExamples: Boolean = true

	override fun onInitializeClient() {
		println("Hello this is ${BuildConfig.MOD_NAME} v${BuildConfig.MOD_VERSION}")

		// Event Bus
		FeatureAutoLoader.scanAndRegister(EVENTBUS, listOf("com.github.mikecraft1224"))
		FeatureAutoLoader.loadOptInPackages(EVENTBUS)

		if (enableExamples) {
			ExampleLoader.register()
		}
	}
}
