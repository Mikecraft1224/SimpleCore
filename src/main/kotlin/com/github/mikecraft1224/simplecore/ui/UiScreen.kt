package com.github.mikecraft1224.simplecore.ui

import com.github.mikecraft1224.simplecore.ui.layout.Padding
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.Drawable
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Element
import net.minecraft.client.gui.Selectable
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW

/**
 * Base [Screen] that hosts a single root [Panel].
 *
 * Subclasses implement [buildRoot] to construct the widget tree. The tree is built once
 * on first open; on resize it is re-laid-out in-place so widget state (e.g. text field
 * content) is preserved. All input and render calls are forwarded to the root panel.
 *
 * Press F4 at any time to toggle a debug overlay that draws colored borders around every
 * widget in the tree. Panel subclasses use a blue tint; leaf widgets use a green tint.
 *
 * ```kotlin
 * class MyScreen : UiScreen(Text.literal("My Screen")) {
 *     override fun buildRoot(): Panel = frame {
 *         Button("Click me", onClick = { println("clicked") }).at(10, 10, 80, 20)
 *     }
 * }
 * ```
 */
abstract class UiScreen(title: Text) : Screen(title) {

    private var root: Panel? = null

    /**
     * When true, colored 1-pixel borders are drawn around every widget after normal rendering.
     * Toggle by pressing F4 while the screen is open.
     */
    var debugBounds: Boolean = false

    /** Registrar that delegates to the protected [addDrawableChild]. */
    private val registrar = object : ChildRegistrar {
        override fun <T> register(child: T) where T : Drawable, T : Element, T : Selectable {
            addDrawableChild(child)
        }
    }

    /**
     * Constructs the widget tree for this screen.
     * Called once on first open. On subsequent resizes the existing tree is reused;
     * only `doLayout` and `onAdded` are called again to update dimensions and re-register
     * Minecraft drawable children.
     */
    protected abstract fun buildRoot(): Panel

    override fun init() {
        val existing = root
        if (existing == null) {
            // First open: build the tree, lay it out, register MC children.
            val panel = buildRoot()
            root = panel
            panel.layout(0, 0, width, height)
            panel.onAdded(registrar)
        } else {
            // Resize: MC already cleared its drawable-children list in clearAndInit().
            // Re-layout with new dimensions, then re-register so MC gets the children back.
            // We do NOT rebuild, preserving widget state (text field content etc.).
            existing.layout(0, 0, width, height)
            existing.onAdded(registrar)
        }
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        super.render(ctx, mx, my, delta)
        root?.render(ctx, mx, my, delta)
        if (debugBounds) {
            val hovered = mutableListOf<Widget>()
            root?.let { drawDebugBounds(ctx, it, mx, my, hovered) }
            hovered.minByOrNull { it.width * it.height }?.let { drawDebugTooltip(ctx, it) }
        }
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean =
        root?.mouseClicked(click, doubled) == true || super.mouseClicked(click, doubled)

    override fun mouseReleased(click: Click): Boolean =
        root?.mouseReleased(click) == true || super.mouseReleased(click)

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean =
        root?.mouseDragged(click, deltaX, deltaY) == true || super.mouseDragged(click, deltaX, deltaY)

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key() == GLFW.GLFW_KEY_F4) {
            debugBounds = !debugBounds
            return true
        }
        return root?.keyPressed(input) == true || super.keyPressed(input)
    }

    override fun charTyped(input: CharInput): Boolean =
        root?.charTyped(input) == true || super.charTyped(input)

    override fun tick() {
        super.tick()
        root?.tick()
    }

    // -- Debug overlay ---------------------------------------------------------

    /** Color for panel/container widget outlines in the debug overlay (blue tint, semi-transparent). */
    private val DEBUG_COLOR_PANEL  = 0x804444FF.toInt()
    /** Color for leaf widget outlines in the debug overlay (green tint, semi-transparent). */
    private val DEBUG_COLOR_WIDGET = 0x8044FF44.toInt()

    /**
     * Recursively draws debug overlays for [widget] and all its children.
     * - [Padding] and [ModifiedWidget] with padding: orange fill (padding area) + blue fill (content area)
     * - Panels: blue outline; leaf widgets: green outline
     * - Hovered widgets are collected in [hovered] for tooltip display.
     */
    private fun drawDebugBounds(ctx: DrawContext, widget: Widget, mx: Int, my: Int, hovered: MutableList<Widget>) {
        val x = widget.x; val y = widget.y; val w = widget.width; val h = widget.height
        if (w <= 0 || h <= 0) return
        when (widget) {
            is Padding -> {
                ctx.fill(x, y, x + w, y + h, 0x55FF8800)
                val cx = x + widget.left; val cy = y + widget.top
                val cw = (w - widget.left - widget.right).coerceAtLeast(0)
                val ch = (h - widget.top - widget.bottom).coerceAtLeast(0)
                if (cw > 0 && ch > 0) ctx.fill(cx, cy, cx + cw, cy + ch, 0x4477AAFF)
            }
            is ModifiedWidget -> {
                if (widget.padTop > 0 || widget.padRight > 0 || widget.padBottom > 0 || widget.padLeft > 0) {
                    ctx.fill(x, y, x + w, y + h, 0x55FF8800)
                    val cx = x + widget.padLeft; val cy = y + widget.padTop
                    val cw = (w - widget.padLeft - widget.padRight).coerceAtLeast(0)
                    val ch = (h - widget.padTop - widget.padBottom).coerceAtLeast(0)
                    if (cw > 0 && ch > 0) ctx.fill(cx, cy, cx + cw, cy + ch, 0x4477AAFF)
                }
            }
            else -> {}
        }
        val color = if (widget is Panel) DEBUG_COLOR_PANEL else DEBUG_COLOR_WIDGET
        drawOutline(ctx, x, y, w, h, color)
        if (widget.contains(mx, my)) hovered.add(widget)
        if (widget is Panel) {
            // Access the protected children field via reflection to keep Panel children encapsulated.
            // This is debug-only code; reflection cost is acceptable.
            try {
                val field = Panel::class.java.getDeclaredField("children")
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val children = field.get(widget) as? List<Widget> ?: return
                for (child in children) drawDebugBounds(ctx, child, mx, my, hovered)
            } catch (_: Exception) {
                // If reflection fails (e.g. in a security-constrained environment), skip children.
            }
        }
    }

    /**
     * Draws a small tooltip above [widget] showing its class name and pixel dimensions.
     */
    private fun drawDebugTooltip(ctx: DrawContext, widget: Widget) {
        val info = "${widget.javaClass.simpleName ?: "?"}  ${widget.width}×${widget.height}"
        val tr = textRenderer
        val lw = tr.getWidth(info) + 4; val lh = tr.fontHeight + 4
        val tx = widget.x.coerceIn(0, this.width - lw)
        val ty = (widget.y - lh - 2).coerceIn(0, this.height - lh)
        ctx.fill(tx, ty, tx + lw, ty + lh, 0xDD000000.toInt())
        ctx.drawText(tr, info, tx + 2, ty + 2, -1, false)
    }

    /**
     * Draws a 1-pixel rectangle outline at ([x], [y]) with size [w] x [h] in [color].
     * Uses four fill calls to avoid overwriting the interior.
     */
    private fun drawOutline(ctx: DrawContext, x: Int, y: Int, w: Int, h: Int, color: Int) {
        if (w <= 0 || h <= 0) return
        ctx.fill(x,         y,         x + w,     y + 1,     color) // top
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color) // bottom
        ctx.fill(x,         y,         x + 1,     y + h,     color) // left
        ctx.fill(x + w - 1, y,         x + w,     y + h,     color) // right
    }
}
