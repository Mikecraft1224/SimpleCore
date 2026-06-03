package com.github.mikecraft1224.simplecore.overlay

import com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition
import com.github.mikecraft1224.simplecore.overlay.api.OverlaySize
import net.minecraft.client.MinecraftClient

/**
 * Frame-scoped registry of rendered overlays.
 *
 * Cleared at the start of each HUD render pass by [com.github.mikecraft1224.simplecore.bus.events.RenderHudEvent],
 * then populated via `renderAt` ([com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition]) as each overlay renders.
 * [OverlayEditScreen] reads [frameEntries] to draw draggable handles over the rendered overlays.
 *
 * Only overlays that actually rendered this frame (i.e. called `renderAt`) appear here.
 * Conditional overlays that returned early won't be shown in the editor.
 */
object OverlayRegistry {

    /**
     * A snapshot of one overlay as rendered in the current frame.
     *
     * @property label   Unique human-readable name shown in the editor.
     * @property position The live [OverlayPosition] — mutating `position.x`/`position.y` moves the overlay.
     * @property size    Pixel dimensions as reported by the overlay's `renderAt` block.
     * @property absX    Resolved absolute screen X (position.x clamped to screen bounds).
     * @property absY    Resolved absolute screen Y (position.y clamped to screen bounds).
     */
    data class FrameEntry(
        val label: String,
        val position: OverlayPosition,
        val size: OverlaySize,
        val absX: Int,
        val absY: Int,
    )

    private val _entries = mutableListOf<FrameEntry>()

    /** All overlays that rendered in the current frame, in render order. */
    val frameEntries: List<FrameEntry> get() = _entries

    /**
     * Clears the frame list. Called by [com.github.mikecraft1224.simplecore.bus.events.RenderHudEvent]
     * at the start of each HUD pass — do not call this manually.
     */
    fun beginFrame() {
        _entries.clear()
    }

    /**
     * Registers an overlay for this frame. Called automatically by
     * `renderAt` — do not call this manually.
     */
    fun report(label: String, position: OverlayPosition, size: OverlaySize, absX: Int, absY: Int) {
        _entries.removeAll { it.label == label }
        _entries.add(FrameEntry(label, position, size, absX, absY))
    }

    /**
     * Opens the interactive overlay position editor.
     *
     * Prefer [com.github.mikecraft1224.simplecore.overlay.HudManager.openEditor] over calling
     * this directly — it is the public API surface for this operation.
     *
     * @param onClose Optional callback invoked when the editor screen closes.
     */
    internal fun openEditScreen(onClose: (() -> Unit)? = null) {
        MinecraftClient.getInstance().setScreen(OverlayEditScreen(onClose))
    }
}
