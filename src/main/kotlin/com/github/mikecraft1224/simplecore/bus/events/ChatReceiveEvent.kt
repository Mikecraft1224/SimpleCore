@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.network.chat.Component

/**
 * Fired when the client receives a game (system) message from the server.
 *
 * Action bar messages (overlay = true) are excluded. Cancelling suppresses the message
 * from appearing in chat.
 */
class ChatReceiveEvent(val message: String, val component: Component) : CancellableEvent() {
    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return
            ClientReceiveMessageEvents.ALLOW_GAME.register { text, overlay ->
                if (overlay) return@register true
                val event = ChatReceiveEvent(text.string, text)
                EventRegistry.post(event)
                !event.isCancelled
            }
            registered = true
        }
    }
}
