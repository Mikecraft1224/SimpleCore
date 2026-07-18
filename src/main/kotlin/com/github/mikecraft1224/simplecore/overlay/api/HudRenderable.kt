package com.github.mikecraft1224.simplecore.overlay.api

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * A composable display element for [HudElement] overlays.
 *
 * Each renderable knows its pixel dimensions and draws itself at a caller-supplied local offset
 * within an already-translated [GuiGraphicsExtractor]. Mouse coordinates are in element-local space
 * (i.e. `(screenMouse - elementOrigin) / scale`) so hit-testing is simply a range check.
 *
 * Build your display list with the factory functions on the companion object and override
 * [HudElement.buildContent] to return it. The framework handles layout, background, hover
 * detection, click routing, and tooltip rendering automatically.
 *
 * ```kotlin
 * override fun buildContent() = listOf(
 *     HudRenderable.text("§e§lMy Tracker"),
 *     HudRenderable.hoverable("§7Profit: §a1,234,567", tooltip = listOf("§7This session")),
 *     HudRenderable.selector("Mode", { mode }, listOf("Coins", "Items")) { mode = it },
 *     HudRenderable.clickable("§c[Reset]", tooltip = listOf("§7Resets all data")) { reset() },
 * )
 * ```
 */
sealed interface HudRenderable {
    /** Pixel width of this element (excluding padding applied by the parent container). */
    val width: Int

    /** Pixel height of this element. */
    val height: Int

    /**
     * Draws this element.
     *
     * @param state   GuiGraphicsExtractor already translated to the [HudElement]'s local origin.
     * @param localMx Mouse X in element-local pixels: `(screenMx - absX) / scale`.
     * @param localMy Mouse Y in element-local pixels: `(screenMy - absY) / scale`.
     * @param lx    Local X offset of this renderable's top-left corner.
     * @param ly    Local Y offset of this renderable's top-left corner.
     */
    fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int)

    /**
     * Routes a mouse-button press to this element.
     *
     * @return `true` if this element consumed the click.
     */
    fun mouseClicked(localMx: Int, localMy: Int, button: Int, lx: Int, ly: Int): Boolean = false

    companion object {
        /**
         * A plain colored text line. Supports Minecraft format codes (§x).
         *
         * @param color ARGB color. Format codes in the string take precedence.
         *              Defaults to opaque white. Must include the alpha byte (`0xFF` prefix) -
         *              unlike pre-26.2, [net.minecraft.client.gui.GuiGraphicsExtractor.text] now
         *              skips drawing entirely when alpha is 0, rather than treating it as opaque.
         */
        fun text(text: String, color: Int = 0xFFFFFFFF.toInt(), shadow: Boolean = true): HudRenderable =
            TextLine(text, color, shadow)

        /**
         * A text line that shows a [tooltip] when the mouse hovers over it.
         *
         * Tooltip lines support Minecraft format codes.
         */
        fun hoverable(
            text: String,
            color: Int = 0xFFFFFFFF.toInt(),
            tooltip: List<String>,
            shadow: Boolean = true,
        ): HudRenderable = HoverableLine(text, color, shadow, tooltip)

        /**
         * A text line that runs [onClick] when clicked and shows a [tooltip] on hover.
         *
         * [onClick] receives the GLFW button code (0=left, 1=right, 2=middle).
         * An underline is drawn beneath the text while the mouse hovers over it.
         */
        fun clickable(
            text: String,
            color: Int = 0xFFFFFFFF.toInt(),
            tooltip: List<String> = emptyList(),
            shadow: Boolean = true,
            onClick: (button: Int) -> Unit,
        ): HudRenderable = ClickableLine(text, color, shadow, tooltip, onClick)

        /**
         * A `§7Label §a[§eCurrentValue§a]` selector line.
         *
         * Left-click cycles forward through [options]; right-click cycles backward.
         * Hovering shows a tooltip with the full option list and a `▶` arrow at the current selection.
         *
         * @param label  Label text displayed before the bracket.
         * @param current Lambda that returns the currently selected option string each frame.
         * @param options All available options.
         * @param onChange Called with the newly selected option string when the user clicks.
         */
        fun selector(
            label: String,
            current: () -> String,
            options: List<String>,
            onChange: (String) -> Unit,
        ): HudRenderable = SelectorLine(label, current, options, onChange)

        /**
         * Stacks [children] vertically with [spacing] pixels between each element.
         *
         * This is the root container wrapping the list returned by [HudElement.buildContent] and is
         * automatically created by the framework - you typically don't need to call this directly.
         */
        fun vertical(children: List<HudRenderable>, spacing: Int = 2): HudRenderable =
            VerticalContainer(children, spacing)

        /**
         * Places [children] side by side horizontally with [spacing] pixels between each element.
         *
         * Width is the sum of all children's widths plus spacing gaps.
         * Height is the maximum child height.
         *
         * ```kotlin
         * HudRenderable.horizontal(listOf(
         *     HudRenderable.text("§7Speed: "),
         *     HudRenderable.text("§a${speed}m/s"),
         * ))
         * ```
         */
        fun horizontal(children: List<HudRenderable>, spacing: Int = 4): HudRenderable =
            HorizontalContainer(children, spacing)

        /**
         * An invisible element that occupies [height] pixels of vertical space.
         *
         * Use this to add blank lines between sections without adjusting [HudElement.lineSpacing].
         */
        fun spacer(height: Int): HudRenderable = Spacer(height)

        /**
         * A custom-drawn element with explicit pixel dimensions.
         *
         * The [draw] lambda receives the already-translated [GuiGraphicsExtractor] and the element's
         * local top-left corner coordinates. Use this for progress bars, colored indicators,
         * and anything the standard types can't express.
         *
         * ```kotlin
         * HudRenderable.custom(80, 6) { state, lx, ly ->
         *     state.fill(lx, ly, lx + 80, ly + 6, 0xFF333333.toInt())        // background
         *     state.fill(lx, ly, lx + (80 * fraction).toInt(), ly + 6, 0xFF55FF55.toInt()) // fill
         * }
         * ```
         */
        fun custom(width: Int, height: Int, draw: (GuiGraphicsExtractor, lx: Int, ly: Int) -> Unit): HudRenderable =
            CustomRenderable(width, height, draw)
    }
}

// ---------------------------------------------------------------------------
// Concrete implementations
// ---------------------------------------------------------------------------

private val tr get() = Minecraft.getInstance().font

private class TextLine(
    private val text: String,
    private val color: Int,
    private val shadow: Boolean,
) : HudRenderable {
    override val width  get() = tr.width(text)
    override val height get() = tr.lineHeight

    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) {
        state.text(tr, text, lx, ly, color, shadow)
    }
}

private open class HoverableLine(
    val text: String,
    val color: Int,
    val shadow: Boolean,
    val tooltip: List<String>,
) : HudRenderable {
    override val width  get() = tr.width(text)
    override val height get() = tr.lineHeight

    protected fun isHovered(localMx: Int, localMy: Int, lx: Int, ly: Int) =
        localMx in lx until lx + width && localMy in ly until ly + height

    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) {
        if (tooltip.isNotEmpty() && isHovered(localMx, localMy, lx, ly)) PendingTooltip.set(tooltip)
        state.text(tr, text, lx, ly, color, shadow)
    }
}

private class ClickableLine(
    text: String,
    color: Int,
    shadow: Boolean,
    tooltip: List<String>,
    private val onClick: (Int) -> Unit,
) : HoverableLine(text, color, shadow, tooltip) {

    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) {
        super.render(state, localMx, localMy, lx, ly)
        if (isHovered(localMx, localMy, lx, ly)) {
            state.fill(lx, ly + height, lx + width, ly + height + 1, color or (0xFF shl 24))
        }
    }

    override fun mouseClicked(localMx: Int, localMy: Int, button: Int, lx: Int, ly: Int): Boolean {
        if (!isHovered(localMx, localMy, lx, ly)) return false
        onClick(button)
        return true
    }
}

private class SelectorLine(
    private val label: String,
    private val current: () -> String,
    private val options: List<String>,
    private val onChange: (String) -> Unit,
) : HudRenderable {
    private fun displayText() = "§7$label §a[§e${current()}§a]"
    override val width  get() = tr.width(displayText())
    override val height get() = tr.lineHeight

    private fun isHovered(localMx: Int, localMy: Int, lx: Int, ly: Int) =
        localMx in lx until lx + width && localMy in ly until ly + height

    private fun buildTooltip(): List<String> = buildList {
        add("§e§l$label")
        val cur = current()
        for (opt in options) {
            if (opt == cur) add("§a▶ §e$opt") else add("  §7$opt")
        }
    }

    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) {
        if (isHovered(localMx, localMy, lx, ly)) PendingTooltip.set(buildTooltip())
        state.text(tr, displayText(), lx, ly, 0xFFFFFFFF.toInt(), true)
    }

    override fun mouseClicked(localMx: Int, localMy: Int, button: Int, lx: Int, ly: Int): Boolean {
        if (!isHovered(localMx, localMy, lx, ly) || options.isEmpty()) return false
        val idx = options.indexOf(current())
        val next = if (button == 1) {
            if (idx <= 0) options.lastIndex else idx - 1
        } else {
            if (idx >= options.lastIndex) 0 else idx + 1
        }
        onChange(options[next])
        return true
    }
}

private class VerticalContainer(
    private val children: List<HudRenderable>,
    private val spacing: Int,
) : HudRenderable {
    override val width  get() = children.maxOfOrNull { it.width } ?: 0
    override val height get() = if (children.isEmpty()) 0
        else children.sumOf { it.height } + spacing * (children.size - 1)

    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) {
        var y = ly
        for (child in children) {
            child.render(state, localMx, localMy, lx, y)
            y += child.height + spacing
        }
    }

    override fun mouseClicked(localMx: Int, localMy: Int, button: Int, lx: Int, ly: Int): Boolean {
        var y = ly
        for (child in children) {
            if (child.mouseClicked(localMx, localMy, button, lx, y)) return true
            y += child.height + spacing
        }
        return false
    }
}

private class HorizontalContainer(
    private val children: List<HudRenderable>,
    private val spacing: Int,
) : HudRenderable {
    override val width  get() = if (children.isEmpty()) 0
        else children.sumOf { it.width } + spacing * (children.size - 1)
    override val height get() = children.maxOfOrNull { it.height } ?: 0

    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) {
        var x = lx
        for (child in children) {
            child.render(state, localMx, localMy, x, ly)
            x += child.width + spacing
        }
    }

    override fun mouseClicked(localMx: Int, localMy: Int, button: Int, lx: Int, ly: Int): Boolean {
        var x = lx
        for (child in children) {
            if (child.mouseClicked(localMx, localMy, button, x, ly)) return true
            x += child.width + spacing
        }
        return false
    }
}

private class Spacer(override val height: Int) : HudRenderable {
    override val width = 0
    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) = Unit
}

private class CustomRenderable(
    override val width: Int,
    override val height: Int,
    private val draw: (GuiGraphicsExtractor, Int, Int) -> Unit,
) : HudRenderable {
    override fun render(state: GuiGraphicsExtractor, localMx: Int, localMy: Int, lx: Int, ly: Int) {
        draw(state, lx, ly)
    }
}

// ---------------------------------------------------------------------------
// Tooltip singleton - rendered after all elements by HudManager
// ---------------------------------------------------------------------------

/**
 * Accumulates a pending tooltip set during the current frame's render pass.
 *
 * `HudManager` calls [clear] at the start of each frame and [renderLast] after all elements
 * have rendered, so the tooltip always appears on top of all overlay content.
 * Hovered elements call [set] during their own [HudRenderable.render] invocation.
 */
internal object PendingTooltip {
    private var lines: List<String>? = null

    fun set(tooltip: List<String>) { lines = tooltip }
    fun clear() { lines = null }

    fun renderLast(state: GuiGraphicsExtractor, screenMx: Int, screenMy: Int, screenW: Int) {
        val l = lines ?: return
        lines = null
        val tr = Minecraft.getInstance().font
        val lineH = tr.lineHeight + 1
        val pad = 4
        val totalH = l.size * lineH - 1
        val totalW = l.maxOfOrNull { tr.width(it) } ?: return
        val bw = totalW + pad * 2
        val bh = totalH + pad * 2
        var tx = screenMx + 12
        var ty = screenMy - bh - 4
        if (ty < 2) ty = screenMy + 14
        if (tx + bw > screenW - 2) tx = screenW - bw - 2
        if (tx < 2) tx = 2
        state.fill(tx - 1, ty - 1, tx + bw + 1, ty + bh + 1, 0xFF555555.toInt())
        state.fill(tx, ty, tx + bw, ty + bh, 0xEE000000.toInt())
        l.forEachIndexed { i, line ->
            state.text(tr, line, tx + pad, ty + pad + i * lineH, 0xFFFFFFFF.toInt(), true)
        }
    }
}
