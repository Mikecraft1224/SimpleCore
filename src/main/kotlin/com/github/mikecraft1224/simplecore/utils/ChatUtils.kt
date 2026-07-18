@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent

object ChatUtils {
    /** Shows [message] in the local chat HUD. Not sent to the server. */
    fun print(message: String) = print(Component.literal(message))

    /** Shows [text] in the local chat HUD. Not sent to the server. */
    fun print(text: Component) {
        McUtils.player?.sendSystemMessage(text)
    }

    /**
     * Sends [message] to the server as a chat message. Include a leading `/` to send
     * a command (the server will process it as a command if it starts with `/`).
     */
    fun send(message: String) {
        val player = McUtils.player ?: return
        player.connection.sendChat(message)
    }

    /**
     * Returns a [Component] that runs [command] on click and optionally shows
     * [hover] as a tooltip.
     */
    fun clickable(text: String, command: String, hover: String? = null): Component =
        Component.literal(text).withStyle { style ->
            var s = style.withClickEvent(ClickEvent.RunCommand(command))
            if (hover != null) s = s.withHoverEvent(HoverEvent.ShowText(Component.literal(hover)))
            s
        }

    /**
     * Returns a [Component] with a multi-line hover tooltip. Lines are joined
     * with newlines.
     */
    fun hoverable(text: String, hover: List<String>): Component =
        Component.literal(text).withStyle { style ->
            style.withHoverEvent(HoverEvent.ShowText(Component.literal(hover.joinToString("\n"))))
        }
}
