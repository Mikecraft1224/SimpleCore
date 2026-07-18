@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.examples.overlay

import com.github.mikecraft1224.simplecore.bus.api.Subscribe
import com.github.mikecraft1224.simplecore.bus.events.ClientTickEvent
import com.github.mikecraft1224.simplecore.overlay.api.HudElement
import com.github.mikecraft1224.simplecore.overlay.api.HudRenderable
import com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos

/**
 * Example HUD overlay - a compact session stats tracker demonstrating all [HudRenderable] types.
 *
 * - **Clickable title**: left-click resets the session kill counter.
 * - **Hoverable kills line**: shows a tooltip with session context.
 * - **Plain text lines**: elapsed time and player coordinates.
 * - **Selector**: cycles through display modes (All / Kills / Position).
 *
 * The panel is repositioned from in-game by pressing the overlay editor keybind (default: O).
 *
 * To persist the position across sessions, store it in a `@Config` class and pass it to
 * the constructor - see TestConfig.kt for the pattern.
 */
object SessionTracker : HudElement("Session Tracker", OverlayPosition(10f, 120f)) {

    private val sessionStart = System.currentTimeMillis()
    private var kills = 0
    private var cachedPos: BlockPos? = null
    private var displayMode = "All"
    private val displayModes = listOf("All", "Kills", "Position")

    // -- Public API ------------------------------------------------------------

    /** Increments the session kill counter. Call from a kill-detection event. */
    fun addKill() { kills++ }

    /** Resets kills and session timer. */
    fun resetSession() { kills = 0 }

    // -- Lifecycle -------------------------------------------------------------

    override fun isEnabled() = Minecraft.getInstance().player != null

    // -- Data update (every tick) ----------------------------------------------

    @Subscribe
    fun onTick(event: ClientTickEvent) {
        if (event.phase != ClientTickEvent.Phase.END) return
        cachedPos = event.client.player?.blockPosition()
    }

    // -- Content ---------------------------------------------------------------

    override fun buildContent(): List<HudRenderable> = buildList {
        add(HudRenderable.clickable(
            "§6§lSession Tracker",
            color = 0xFFFFAA00.toInt(),
            tooltip = listOf("§7Left-click to §creset §7session."),
        ) { button -> if (button == 0) resetSession() })

        // spacer: blank vertical gap between title and stats
        add(HudRenderable.spacer(3))

        val elapsed = System.currentTimeMillis() - sessionStart
        val h = (elapsed / 3_600_000).toInt()
        val m = ((elapsed / 60_000) % 60).toInt()
        val s = ((elapsed / 1_000) % 60).toInt()

        // horizontal: label on the left, value on the right - demonstrates HudRenderable.horizontal()
        add(HudRenderable.horizontal(listOf(
            HudRenderable.text("§7Time: ", 0xFFAAAAAA.toInt()),
            HudRenderable.text("§f%d:%02d:%02d".format(h, m, s)),
        )))

        if (displayMode != "Position") {
            add(HudRenderable.hoverable(
                "§7Kills: §a$kills",
                color = if (kills > 0) 0xFF55FF55.toInt() else 0xFFAAAAAA.toInt(),
                tooltip = listOf("§7Total kills this session.", "§eLeft-click the title to reset."),
            ))
        }

        if (displayMode != "Kills") {
            val pos = cachedPos
            val posStr = if (pos != null) "${pos.x}, ${pos.y}, ${pos.z}" else "---"
            add(HudRenderable.text("§7Pos:   §f$posStr", 0xFFAAAAAA.toInt()))
        }

        add(HudRenderable.selector(
            label = "Mode",
            current = { displayMode },
            options = displayModes,
            onChange = { displayMode = it },
        ))
    }
}
