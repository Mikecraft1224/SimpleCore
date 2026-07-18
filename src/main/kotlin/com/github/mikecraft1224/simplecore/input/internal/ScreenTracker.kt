package com.github.mikecraft1224.simplecore.input.internal

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.client.gui.screens.Screen

/**
 * Tracks the currently open [Screen].
 *
 * 26.2 removed [net.minecraft.client.Minecraft]'s plain `screen` accessor as part of the
 * render-state-extraction GUI rework, so this is rebuilt on top of Fabric API's
 * [ScreenEvents.AFTER_INIT]/[ScreenEvents.remove] instead. Self-registers on first access.
 */
internal object ScreenTracker {
    var currentScreen: Screen? = null
        private set

    init {
        ScreenEvents.AFTER_INIT.register(ScreenEvents.AfterInit { _, screen, _, _ ->
            currentScreen = screen
            ScreenEvents.remove(screen).register(ScreenEvents.Remove {
                if (currentScreen === screen) currentScreen = null
            })
        })
    }
}
