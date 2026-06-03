@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.EventRegistry
import com.github.mikecraft1224.simplecore.bus.api.CancellableEvent
import com.github.mikecraft1224.simplecore.bus.api.EventCompanion
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents

/**
 * Fired before the player sends a chat message to the server.
 *
 * Cancelling suppresses the message. Commands (starting with `/`) are not covered
 * by this event; they fire their own Fabric API callback.
 */
class ChatSendEvent(val message: String) : CancellableEvent() {
    companion object : EventCompanion {
        private var registered = false

        override fun registerEvents() {
            if (registered) return
            ClientSendMessageEvents.ALLOW_CHAT.register { message ->
                val event = ChatSendEvent(message)
                EventRegistry.post(event)
                !event.isCancelled
            }
            registered = true
        }
    }
}
