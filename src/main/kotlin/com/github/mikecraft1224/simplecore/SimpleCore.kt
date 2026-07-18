package com.github.mikecraft1224.simplecore

import com.github.mikecraft1224.simplecore.bus.EventBus
import com.github.mikecraft1224.simplecore.bus.FeatureAutoLoader
import com.github.mikecraft1224.simplecore.examples.Examples
import net.fabricmc.api.ClientModInitializer

object SimpleCore : ClientModInitializer {
	val EVENTBUS = EventBus()

	/**
	 * Configure which built-in examples to load before [onInitializeClient] runs.
	 *
	 * ```kotlin
	 * SimpleCore.examples.config = true
	 * ```
	 */
	val examples = Examples()

	override fun onInitializeClient() {
		println("Hello this is ${BuildConfig.MOD_NAME} v${BuildConfig.MOD_VERSION}")
        examples.command = true
        examples.render = true
        examples.config = true
        examples.overlay = true

        // Event Bus
		FeatureAutoLoader.scanAndRegister(EVENTBUS, listOf("com.github.mikecraft1224.simplecore"))
		FeatureAutoLoader.loadOptInPackages(EVENTBUS)

		examples.load()
	}
}
