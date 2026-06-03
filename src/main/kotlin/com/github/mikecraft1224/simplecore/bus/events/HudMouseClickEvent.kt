@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.Event
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion

/**
 * Fired when a mouse button is pressed while no screen is open (i.e. during normal gameplay HUD).
 *
 * Dispatched by `MouseButtonMixin` via [com.github.mikecraft1224.simplecore.bus.EventRegistry].
 * Consumed by [com.github.mikecraft1224.simplecore.overlay.HudManager] to route clicks to interactive
 * [com.github.mikecraft1224.simplecore.overlay.api.HudElement] instances.
 *
 * @property mx Scaled GUI-space mouse X (screen pixels / window scale factor).
 * @property my Scaled GUI-space mouse Y.
 * @property button GLFW mouse button constant (0 = left, 1 = right, 2 = middle).
 */
class HudMouseClickEvent(
    val mx: Int,
    val my: Int,
    val button: Int,
) : Event() {
    companion object : EventCompanion {
        override fun registerEvents() {}
    }
}
