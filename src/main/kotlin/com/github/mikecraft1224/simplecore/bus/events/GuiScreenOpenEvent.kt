@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.client.gui.screen.Screen

/**
 * Fired when [net.minecraft.client.MinecraftClient.setScreen] is called.
 *
 * [screen] is `null` when a screen is being closed. Cancelling prevents the screen
 * change from taking effect.
 */
class GuiScreenOpenEvent(val screen: Screen?) : CancellableEvent()
