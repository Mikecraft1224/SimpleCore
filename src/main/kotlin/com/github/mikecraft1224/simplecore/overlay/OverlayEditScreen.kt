package com.github.mikecraft1224.simplecore.overlay

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

/**
 * Interactive HUD overlay position editor.
 *
 * Opened via [OverlayRegistry.openEditScreen]. While open, the HUD still renders normally
 * (HudRenderCallback fires before screen rendering), so overlays appear at their current
 * positions behind the drag handles.
 *
 * Controls:
 * - **Drag** — click and drag any overlay handle to reposition it.
 * - **Arrow keys** — nudge the selected overlay by 1px (hold Shift for 10px steps).
 * - **Scroll wheel** — adjust the scale of the hovered overlay (0.5× – 3.0×).
 * - **Escape** — close the editor. Changes are applied immediately to each overlay's
 *   [com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition]; persist by saving your config.
 */
class OverlayEditScreen(private val onClose: (() -> Unit)? = null) : Screen(Text.literal("Overlay Editor")) {

    private var draggedLabel: String? = null
    private var selectedLabel: String? = null
    private var dragStartMouseX: Double = 0.0
    private var dragStartMouseY: Double = 0.0
    private var dragStartPosX: Float = 0f
    private var dragStartPosY: Float = 0f

    override fun removed() {
        super.removed()
        onClose?.invoke()
    }

    // Render the game HUD in the background; do not draw Screen's opaque background.
    override fun renderBackground(ctx: DrawContext, mx: Int, my: Int, delta: Float) = Unit

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        // Semi-transparent dark vignette so handles are readable without hiding the overlays.
        ctx.fill(0, 0, width, height, 0x80000000.toInt())

        val entries = OverlayRegistry.frameEntries
        if (entries.isEmpty()) {
            val msg = "No overlays are currently visible."
            val msgW = textRenderer.getWidth(msg)
            ctx.drawText(textRenderer, msg, (width - msgW) / 2, height / 2 - 4, 0x888888, true)
        }

        for (entry in entries) {
            val hovered  = isHovered(entry, mx, my)
            val selected = selectedLabel == entry.label
            val dragging = draggedLabel == entry.label

            val fillColor = when {
                dragging -> 0xCC3355EE.toInt()
                selected -> 0xAA2244CC.toInt()
                hovered  -> 0x8866AAEE.toInt()
                else     -> 0x55333366
            }
            val borderColor = when {
                dragging || selected -> 0xFFAABBFF.toInt()
                hovered              -> 0xFF8899DD.toInt()
                else                 -> 0xFF555577.toInt()
            }

            val w = entry.size.width
            val h = entry.size.height
            ctx.fill(entry.absX, entry.absY, entry.absX + w, entry.absY + h, fillColor)
            drawBorder(ctx, entry.absX, entry.absY, w, h, borderColor)

            val info = "${entry.label}  (${entry.absX}, ${entry.absY})"
            ctx.drawText(textRenderer, info, entry.absX + 3, entry.absY + 3, 0xFFFFFF, true)
        }

        val hint = "Drag to reposition  \u00b7  Arrow keys to nudge (Shift = 10px)  \u00b7  Scroll to scale  \u00b7  Escape to close"
        val hintW = textRenderer.getWidth(hint)
        ctx.drawText(textRenderer, hint, (width - hintW) / 2, height - 14, 0x888888, true)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() != 0) return super.mouseClicked(click, doubled)
        val mx = click.x().toInt()
        val my = click.y().toInt()
        val entry = OverlayRegistry.frameEntries.firstOrNull { isHovered(it, mx, my) }
        if (entry != null) {
            draggedLabel    = entry.label
            selectedLabel   = entry.label
            dragStartMouseX = click.x()
            dragStartMouseY = click.y()
            dragStartPosX   = entry.position.x
            dragStartPosY   = entry.position.y
            return true
        }
        return super.mouseClicked(click, doubled)
    }

    override fun mouseReleased(click: Click): Boolean {
        draggedLabel = null
        return super.mouseReleased(click)
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        val label = draggedLabel ?: return false
        val entry = OverlayRegistry.frameEntries.firstOrNull { it.label == label } ?: return false
        entry.position.x = (dragStartPosX + (click.x() - dragStartMouseX).toFloat())
            .coerceIn(0f, (width - entry.size.width).toFloat().coerceAtLeast(0f))
        entry.position.y = (dragStartPosY + (click.y() - dragStartMouseY).toFloat())
            .coerceIn(0f, (height - entry.size.height).toFloat().coerceAtLeast(0f))
        return true
    }

    override fun keyPressed(input: KeyInput): Boolean {
        val label = selectedLabel ?: return super.keyPressed(input)
        val entry = OverlayRegistry.frameEntries.firstOrNull { it.label == label }
            ?: return super.keyPressed(input)
        val win = MinecraftClient.getInstance().window.handle
        val shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT)  == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS
        val step = if (shift) 10f else 1f
        when (input.key()) {
            GLFW.GLFW_KEY_LEFT  -> { entry.position.x = (entry.position.x - step).coerceAtLeast(0f); return true }
            GLFW.GLFW_KEY_RIGHT -> { entry.position.x = (entry.position.x + step).coerceAtMost((width  - entry.size.width ).toFloat().coerceAtLeast(0f)); return true }
            GLFW.GLFW_KEY_UP    -> { entry.position.y = (entry.position.y - step).coerceAtLeast(0f); return true }
            GLFW.GLFW_KEY_DOWN  -> { entry.position.y = (entry.position.y + step).coerceAtMost((height - entry.size.height).toFloat().coerceAtLeast(0f)); return true }
        }
        return super.keyPressed(input)
    }

    /**
     * Scroll while hovering an overlay to adjust its [com.github.mikecraft1224.simplecore.overlay.api.OverlayPosition.scale].
     * Range is clamped to [0.5, 3.0] in 0.1 steps.
     */
    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val entry = OverlayRegistry.frameEntries.firstOrNull { isHovered(it, mx, my) }
        if (entry != null) {
            entry.position.scale = (entry.position.scale + verticalAmount.toFloat() * 0.1f)
                .coerceIn(0.5f, 3f)
            selectedLabel = entry.label
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    // -- Helpers ---------------------------------------------------------------

    /**
     * Returns true if ([mx], [my]) falls within [entry]'s drag handle.
     * A minimum hit area of 20x20 is enforced so tiny overlays remain clickable.
     */
    private fun isHovered(entry: OverlayRegistry.FrameEntry, mx: Int, my: Int): Boolean {
        val w = entry.size.width.coerceAtLeast(20)
        val h = entry.size.height.coerceAtLeast(20)
        return mx in entry.absX until entry.absX + w && my in entry.absY until entry.absY + h
    }

    private fun drawBorder(ctx: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w <= 0 || h <= 0) return
        ctx.fill(x,         y,         x + w,     y + 1,     color) // top
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color) // bottom
        ctx.fill(x,         y,         x + 1,     y + h,     color) // left
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color) // right
    }
}
