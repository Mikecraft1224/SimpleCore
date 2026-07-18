package com.github.mikecraft1224.simplecore.overlay

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW

/**
 * Interactive HUD overlay position editor.
 *
 * Opened via [HudManager.openEditor] (all overlays) or [HudGroup.openEditor] (one mod's
 * overlays). While open, the HUD still renders normally so overlays appear at their current
 * positions behind the drag handles.
 *
 * When opened via [HudGroup], only that group's elements are shown and a header line names
 * the group. When [showOnlyActive] is `true`, disabled (ghost) overlays are hidden.
 *
 * Controls:
 * - **Drag** - reposition an overlay.
 * - **Arrow keys** - nudge the selected overlay 1px (Shift = 10px).
 * - **Scroll wheel** - adjust scale of the hovered overlay (0.5x - 5.0x).
 * - **Middle-click** - reset the hovered overlay to its registration-time position and scale.
 * - **Escape** - close. Changes apply immediately; persist by saving your config.
 */
class OverlayEditScreen(
    private val filter: Set<String>? = null,
    private val groupName: String? = null,
    private val showOnlyActive: Boolean = false,
    private val onClose: (() -> Unit)? = null,
) : Screen(Component.literal(if (groupName != null) "$groupName - Overlay Editor" else "Overlay Editor")) {

    private var draggedLabel: String? = null
    private var selectedLabel: String? = null
    private var dragStartMouseX: Double = 0.0
    private var dragStartMouseY: Double = 0.0
    private var dragStartPosX: Float = 0f
    private var dragStartPosY: Float = 0f

    /**
     * The current filtered entry list. Re-evaluated every access so it always reflects the
     * latest frame state. All interaction methods (click, drag, key, scroll) read from this.
     */
    private val entries: List<OverlayRegistry.FrameEntry>
        get() {
            val source = if (showOnlyActive) OverlayRegistry.frameEntries else OverlayRegistry.allEntries
            return if (filter != null) source.filter { it.label in filter } else source
        }

    override fun removed() {
        super.removed()
        onClose?.invoke()
    }

    override fun extractBackground(state: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) = Unit

    override fun extractRenderState(state: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        state.fill(0, 0, width, height, 0x80000000.toInt())

        val snap = entries  // single snapshot for this frame

        // Header: shown when a group filter is active.
        if (filter != null) {
            val activeCount = snap.count { OverlayRegistry.isActive(it.label) }
            val totalCount  = filter.size
            val name = groupName ?: "Group"
            val countStr = if (showOnlyActive) "$activeCount active" else "$activeCount/$totalCount"
            val header = "$name  ($countStr overlay${if (totalCount != 1) "s" else ""})"
            val hw = font.width(header)
            state.text(font, header, (width - hw) / 2, 6, 0xFFCCCCCC.toInt(), true)
        }

        // Empty-state message.
        if (snap.isEmpty()) {
            val msg = when {
                filter != null && showOnlyActive -> "No overlays from this group are currently active."
                filter != null                   -> "No overlays from this group are registered."
                showOnlyActive                   -> "No overlays are currently active."
                else                             -> "No overlays are registered."
            }
            val msgW = font.width(msg)
            state.text(font, msg, (width - msgW) / 2, height / 2 - 4, 0xFF888888.toInt(), true)
        }

        // Two-pass draw: ghosts (disabled) first so active entries render on top.
        for (pass in 0..1) {
            for (entry in snap) {
                val active = OverlayRegistry.isActive(entry.label)
                if ((pass == 0) == active) continue  // pass 0 = ghosts, pass 1 = active

                val hovered  = isHovered(entry, mx, my)
                val selected = selectedLabel == entry.label
                val dragging = draggedLabel  == entry.label

                // Kept low-opacity so the overlay's own content stays legible through the handle
                // while it's being positioned - this box is a drag target, not a solid panel.
                val fillColor = when {
                    !active              -> 0x22222244
                    dragging             -> 0x503355EE.toInt()
                    selected             -> 0x382244CC.toInt()
                    hovered              -> 0x2866AAEE.toInt()
                    else                 -> 0x18333366
                }
                val borderColor = when {
                    !active              -> 0xFF444466.toInt()
                    dragging || selected -> 0xFFAABBFF.toInt()
                    hovered              -> 0xFF8899DD.toInt()
                    else                 -> 0xFF555577.toInt()
                }
                val textColor = if (active) 0xFFFFFFFF.toInt() else 0xFF888888.toInt()

                val w = entry.size.width
                val h = entry.size.height
                state.fill(entry.absX, entry.absY, entry.absX + w, entry.absY + h, fillColor)
                drawBorder(state, entry.absX, entry.absY, w, h, borderColor)

                // Label + coordinates float above the box instead of inside it, so the handle's
                // interior stays a clear preview of the overlay's actual rendered content.
                val label = if (active) entry.label else "${entry.label} (disabled)"
                val info = "(${entry.absX}, ${entry.absY})"
                val lines = if (active) listOf(label, info) else listOf(label)
                val blockH = lines.size * font.lineHeight + (lines.size - 1)
                val blockW = lines.maxOf { font.width(it) }
                val textTopY = (entry.absY - blockH - 4).coerceAtLeast(0)
                state.fill(entry.absX - 2, textTopY - 2, entry.absX + blockW + 4, textTopY + blockH + 2, 0x99000000.toInt())
                state.text(font, label, entry.absX + 1, textTopY, textColor, true)
                if (active) {
                    state.text(font, info, entry.absX + 1, textTopY + font.lineHeight + 1, 0xFFAAAAAA.toInt(), true)
                }
            }
        }

        val hint = "Drag  ·  Arrow keys (Shift=10px)  ·  Scroll to scale  ·  Middle-click to reset  ·  Escape to close"
        val hintW = font.width(hint)
        state.text(font, hint, (width - hintW) / 2, height - 14, 0xFF888888.toInt(), true)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = click.x().toInt()
        val my = click.y().toInt()
        return when (click.button()) {
            0 -> {
                val entry = entries.firstOrNull { isHovered(it, mx, my) }
                if (entry != null) {
                    draggedLabel    = entry.label
                    selectedLabel   = entry.label
                    dragStartMouseX = click.x()
                    dragStartMouseY = click.y()
                    dragStartPosX   = entry.position.x
                    dragStartPosY   = entry.position.y
                    true
                } else super.mouseClicked(click, doubled)
            }
            2 -> {
                val entry = entries.firstOrNull { isHovered(it, mx, my) }
                if (entry != null) {
                    entry.reset?.invoke()
                    selectedLabel = entry.label
                    true
                } else super.mouseClicked(click, doubled)
            }
            else -> super.mouseClicked(click, doubled)
        }
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        draggedLabel = null
        return super.mouseReleased(click)
    }

    override fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        val label = draggedLabel ?: return false
        val entry = entries.firstOrNull { it.label == label } ?: return false
        entry.position.x = (dragStartPosX + (click.x() - dragStartMouseX).toFloat())
            .coerceIn(0f, (width  - entry.size.width ).toFloat().coerceAtLeast(0f))
        entry.position.y = (dragStartPosY + (click.y() - dragStartMouseY).toFloat())
            .coerceIn(0f, (height - entry.size.height).toFloat().coerceAtLeast(0f))
        return true
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        val label = selectedLabel ?: return super.keyPressed(input)
        val entry = entries.firstOrNull { it.label == label } ?: return super.keyPressed(input)
        val step = if (input.hasShiftDown()) 10f else 1f
        when (input.key()) {
            GLFW.GLFW_KEY_LEFT  -> { entry.position.x = (entry.position.x - step).coerceAtLeast(0f); return true }
            GLFW.GLFW_KEY_RIGHT -> { entry.position.x = (entry.position.x + step).coerceAtMost((width  - entry.size.width ).toFloat().coerceAtLeast(0f)); return true }
            GLFW.GLFW_KEY_UP    -> { entry.position.y = (entry.position.y - step).coerceAtLeast(0f); return true }
            GLFW.GLFW_KEY_DOWN  -> { entry.position.y = (entry.position.y + step).coerceAtMost((height - entry.size.height).toFloat().coerceAtLeast(0f)); return true }
        }
        return super.keyPressed(input)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val entry = entries.firstOrNull { isHovered(it, mx, my) }
        if (entry != null) {
            entry.position.scale = (entry.position.scale + verticalAmount.toFloat() * 0.1f)
                .coerceIn(0.5f, 5f)
            selectedLabel = entry.label
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    // -- Helpers ---------------------------------------------------------------

    private fun isHovered(entry: OverlayRegistry.FrameEntry, mx: Int, my: Int): Boolean {
        val w = entry.size.width.coerceAtLeast(20)
        val h = entry.size.height.coerceAtLeast(20)
        return mx in entry.absX until entry.absX + w && my in entry.absY until entry.absY + h
    }

    private fun drawBorder(state: GuiGraphicsExtractor, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w <= 0 || h <= 0) return
        state.fill(x,         y,         x + w,     y + 1,     color)
        state.fill(x,         y + h - 1, x + w,     y + h,     color)
        state.fill(x,         y,         x + 1,     y + h,     color)
        state.fill(x + w - 1, y,         x + w,     y + h,     color)
    }
}
