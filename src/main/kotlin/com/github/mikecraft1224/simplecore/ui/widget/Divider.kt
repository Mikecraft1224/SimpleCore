package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.UiTheme
import com.github.mikecraft1224.simplecore.ui.Widget
import com.github.mikecraft1224.simplecore.ui.currentTheme
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

/**
 * A horizontal or vertical divider line with an optional centered text label.
 *
 * When [label] is empty a plain line is drawn. When [label] is set, two line
 * segments bracket the centered label text (matching the config screen separator style).
 *
 * ```kotlin
 * val div = Divider(theme, label = "Advanced")
 * val plain = Divider(theme)
 * ```
 *
 * @param theme     theme used for line and text colors
 * @param label     optional text shown at the center of the divider
 * @param vertical  if true, draws a vertical line instead of horizontal
 */
class Divider(
    private val theme: UiTheme = currentTheme,
    var label: String = "",
    private val vertical: Boolean = false,
) : Widget {

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        if (vertical) {
            val lx = x + width / 2
            ctx.fill(lx, y, lx + 1, y + height, theme.divider)
            return
        }

        val ly = y + height / 2
        if (label.isEmpty()) {
            ctx.fill(x, ly, x + width, ly + 1, theme.divider)
        } else {
            val tr  = MinecraftClient.getInstance().textRenderer
            val lw  = tr.getWidth(label)
            val cx  = x + width / 2
            val gap = 6
            ctx.fill(x,               ly, cx - lw / 2 - gap, ly + 1, theme.divider)
            ctx.fill(cx + lw / 2 + gap, ly, x + width,       ly + 1, theme.divider)
            ctx.drawCenteredTextWithShadow(tr, label, cx, ly - tr.fontHeight / 2, theme.overlay0)
        }
    }
}
