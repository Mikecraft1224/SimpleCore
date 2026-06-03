@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import net.minecraft.client.MinecraftClient
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.Text

object ChatUtils {
    /** Shows [message] in the local chat HUD. Not sent to the server. */
    fun print(message: String) = print(Text.literal(message))

    /** Shows [text] in the local chat HUD. Not sent to the server. */
    fun print(text: Text) {
        MinecraftClient.getInstance().inGameHud?.chatHud?.addMessage(text)
    }

    /**
     * Sends [message] to the server as a chat message. Include a leading `/` to send
     * a command (the server will process it as a command if it starts with `/`).
     */
    fun send(message: String) {
        val player = McUtils.player ?: return
        player.networkHandler.sendChatMessage(message)
    }

    /**
     * Returns a [Text] component that runs [command] on click and optionally shows
     * [hover] as a tooltip.
     */
    fun clickable(text: String, command: String, hover: String? = null): Text =
        Text.literal(text).styled { style ->
            var s = style.withClickEvent(ClickEvent.RunCommand(command))
            if (hover != null) s = s.withHoverEvent(HoverEvent.ShowText(Text.literal(hover)))
            s
        }

    /**
     * Returns a [Text] component with a multi-line hover tooltip. Lines are joined
     * with newlines.
     */
    fun hoverable(text: String, hover: List<String>): Text =
        Text.literal(text).styled { style ->
            style.withHoverEvent(HoverEvent.ShowText(Text.literal(hover.joinToString("\n"))))
        }
}
