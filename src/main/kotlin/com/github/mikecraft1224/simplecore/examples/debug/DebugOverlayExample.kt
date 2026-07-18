package com.github.mikecraft1224.simplecore.examples.debug

import com.github.mikecraft1224.simplecore.SimpleCore
import com.github.mikecraft1224.simplecore.bus.EventBusMonitor
import com.github.mikecraft1224.simplecore.input.KeybindRegistry
import com.github.mikecraft1224.simplecore.overlay.OverlayRegistry
import com.github.mikecraft1224.simplecore.overlay.api.HudElement
import com.github.mikecraft1224.simplecore.overlay.api.HudRenderable
import com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition

/**
 * A live debug HUD showing SimpleCore's own internal state - registered event classes, recent
 * dispatch throughput (when [SimpleCore.EVENTBUS]'s `debugMode` is on), registered overlays, and
 * registered keybinds. Useful while developing a mod on top of SimpleCore to sanity-check that
 * your features actually wired up.
 *
 * Disabled by default - flip [enabled] (e.g. from your own keybind) to show it:
 * ```kotlin
 * SimpleCore.EVENTBUS.debugMode = true // optional: populate the recent-posts counter
 * DebugOverlayExample.enabled = true
 * ```
 */
object DebugOverlayExample : HudElement("SimpleCore Debug", OverlayPosition(10f, 200f)) {

    /** Set to `true` to show the overlay. */
    var enabled = false

    override fun isEnabled(): Boolean = enabled

    override fun buildContent(): List<HudRenderable> {
        val bus = SimpleCore.EVENTBUS
        val recent = EventBusMonitor.getRecent()
        val recentCancelled = recent.count { it.wasCancelled }

        return listOf(
            HudRenderable.text("§b§lSimpleCore Debug"),
            HudRenderable.text("§7Event classes: §f${bus.getRegisteredEventClasses().size}"),
            HudRenderable.text("§7Debug mode: ${if (bus.debugMode) "§aon" else "§coff"}"),
            HudRenderable.text("§7Recent posts: §f${recent.size} §7(§c$recentCancelled§7 cancelled)"),
            HudRenderable.text("§7Overlays: §f${OverlayRegistry.frameEntries.size}§7/§f${OverlayRegistry.allEntries.size} active"),
            HudRenderable.text("§7Keybinds: §f${KeybindRegistry.registeredCount}"),
        )
    }
}
