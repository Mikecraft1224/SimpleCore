@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import net.minecraft.client.gui.screens.Screen

/**
 * Fired when [net.minecraft.client.Minecraft.setScreenAndShow] is called.
 *
 * [screen] is `null` when a screen is being closed. Cancelling prevents the screen
 * change from taking effect.
 */
class GuiScreenOpenEvent(val screen: Screen?) : CancellableEvent()
