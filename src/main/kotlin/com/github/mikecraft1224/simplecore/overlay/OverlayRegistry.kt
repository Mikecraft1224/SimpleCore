package com.github.mikecraft1224.simplecore.overlay

import com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition
import com.github.mikecraft1224.simplecore.overlay.api.OverlaySize
import net.minecraft.client.Minecraft

/**
 * Registry of all HUD overlays known to the system.
 *
 * Maintains two views:
 * - [allEntries] - every registered overlay (persists across frames, includes disabled ones).
 * - [frameEntries] - only overlays that actually rendered in the current frame.
 *
 * [OverlayEditScreen] uses [allEntries] so disabled overlays still appear as ghost handles and
 * can be repositioned even when [com.github.mikecraft1224.simplecore.overlay.api.HudElement.isEnabled]
 * returns false.
 *
 * Overlays are seeded into [allEntries] when [com.github.mikecraft1224.simplecore.overlay.HudManager.register]
 * is called, and removed when [com.github.mikecraft1224.simplecore.overlay.HudManager.unregister] is called.
 * The entry is updated each frame when the overlay actually renders via `renderAt`.
 */
object OverlayRegistry {

    /**
     * A snapshot of one overlay's position and size.
     *
     * @property label       Unique human-readable name shown in the editor.
     * @property position    The live [OverlayPosition] - mutating it moves the overlay immediately.
     * @property size        Pixel dimensions. Updated each frame when the overlay renders; falls
     *                       back to an 80x20 placeholder for overlays that have not yet rendered.
     * @property absX        Resolved absolute screen X (position.x clamped to [0, screen width]).
     * @property absY        Resolved absolute screen Y (position.y clamped to [0, screen height]).
     * @property reset       Restores the overlay to its registration-time position and scale.
     *                       Null only transiently before [seed] is called (never null in practice).
     */
    class FrameEntry(
        val label: String,
        val position: OverlayPosition,
        val size: OverlaySize,
        val absX: Int,
        val absY: Int,
        val reset: (() -> Unit)? = null,
    )

    private val _allEntries  = linkedMapOf<String, FrameEntry>()
    private val _frameActive = mutableSetOf<String>()

    /** Every registered overlay, including those not rendered this frame (disabled overlays). */
    val allEntries: List<FrameEntry>   get() = _allEntries.values.toList()

    /** Overlays that rendered in the current frame. */
    val frameEntries: List<FrameEntry> get() = _frameActive.mapNotNull { _allEntries[it] }

    /** Returns true if [label] rendered in the current frame. */
    fun isActive(label: String): Boolean = label in _frameActive

    /**
     * Marks the start of a new HUD render pass.
     * Clears the active-this-frame set but keeps all registered entries intact.
     * Called by [com.github.mikecraft1224.simplecore.bus.events.RenderHudEvent] - do not call manually.
     */
    fun beginFrame() {
        _frameActive.clear()
    }

    /**
     * Registers an overlay as rendered this frame. Called automatically by
     * `renderAt` ([com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition]) - do not call manually.
     */
    fun report(label: String, position: OverlayPosition, size: OverlaySize, absX: Int, absY: Int) {
        val reset = _allEntries[label]?.reset
        _allEntries[label] = FrameEntry(label, position, size, absX, absY, reset)
        _frameActive.add(label)
    }

    /**
     * Registers an overlay so it appears in the editor even before (or without) rendering.
     * Called by [com.github.mikecraft1224.simplecore.overlay.HudManager.register].
     *
     * If an entry already exists (re-registration), its size is preserved and the reset
     * callback is updated to the new element instance.
     */
    internal fun seed(label: String, position: OverlayPosition, reset: () -> Unit) {
        val x = position.x.toInt().coerceAtLeast(0)
        val y = position.y.toInt().coerceAtLeast(0)
        _allEntries[label] = FrameEntry(
            label, position,
            _allEntries[label]?.size ?: OverlaySize(80, 20),
            x, y, reset,
        )
    }

    /**
     * Removes an overlay from the registry entirely.
     * Called by [com.github.mikecraft1224.simplecore.overlay.HudManager.unregister].
     */
    internal fun unregisterElement(label: String) {
        _allEntries.remove(label)
        _frameActive.remove(label)
    }

    /**
     * Opens the interactive overlay position editor.
     *
     * Prefer [com.github.mikecraft1224.simplecore.overlay.HudManager.openEditor] or
     * `HudGroup.openEditor` over calling this directly.
     *
     * @param filter         Only show overlays whose labels are in this set. `null` shows all.
     * @param groupName      Human-readable name shown in the editor header when [filter] is set.
     * @param showOnlyActive When `true`, ghost/disabled entries are hidden; only overlays that
     *   rendered this frame are shown.
     * @param onClose        Optional callback invoked when the editor screen closes.
     */
    internal fun openEditScreen(
        filter: Set<String>? = null,
        groupName: String? = null,
        showOnlyActive: Boolean = false,
        onClose: (() -> Unit)? = null,
    ) {
        Minecraft.getInstance().setScreenAndShow(OverlayEditScreen(filter, groupName, showOnlyActive, onClose))
    }
}
