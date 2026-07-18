@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.utils

import com.github.mikecraft1224.simplecore.utils.internal.TabListCache
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.network.chat.Component

/**
 * Reads the current tab list (Tab key player list) header/footer and player rows.
 *
 * Header/footer are cached from the last [com.github.mikecraft1224.simplecore.bus.events.TabListEvent] -
 * subscribe to that event instead if you need to react to changes rather than poll. Player rows
 * are read live from the connection, since vanilla already keeps that up to date for its own tab
 * list rendering, so no caching is needed there.
 */
object TabListUtils {
    /** Current tab list header, or `null` if none is set. */
    val header: Component? get() = TabListCache.header

    /** Current tab list footer, or `null` if none is set. */
    val footer: Component? get() = TabListCache.footer

    /** All players currently shown in the tab list. */
    val entries: Collection<PlayerInfo> get() = McUtils.player?.connection?.listedOnlinePlayers ?: emptyList()

    /** [entries]' display names as plain strings, for quick display/logging. */
    val entryNames: List<String> get() = entries.map { it.tabListDisplayName?.string ?: it.profile.name }
}
