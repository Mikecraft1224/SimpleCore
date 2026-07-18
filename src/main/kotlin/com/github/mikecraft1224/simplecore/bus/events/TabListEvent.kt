@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.bus.events

import com.github.mikecraft1224.simplecore.bus.api.Event
import net.minecraft.network.chat.Component

/**
 * Fired when the server updates the tab list (Tab key player list) header/footer text.
 *
 * There's no equivalent packet-level event for the player rows themselves (name, ping,
 * gamemode) - read them live via [com.github.mikecraft1224.simplecore.utils.TabListUtils.entries]
 * instead, since vanilla already keeps that up to date for its own tab list rendering.
 *
 * ```kotlin
 * @Subscribe
 * fun onTabList(event: TabListEvent) {
 *     ChatUtils.print("Tab list footer changed: ${event.footer?.string}")
 * }
 * ```
 */
class TabListEvent(val header: Component?, val footer: Component?) : Event()
