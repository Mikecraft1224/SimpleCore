package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.UiTheme
import com.github.mikecraft1224.simplecore.ui.Widget
import com.github.mikecraft1224.simplecore.ui.currentTheme
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext

/**
 * A clickable button that renders a filled rectangle with centered label text.
 *
 * ```kotlin
 * val btn = Button("Save", theme) { manager.save() }
 * ```
 *
 * @param label   text displayed inside the button
 * @param theme   theme used for colors (defaults to [UiTheme.DEFAULT])
 * @param onClick callback invoked on left-click
 */
class Button(
    var label: String,
    private val theme: UiTheme = currentTheme,
    private val onClick: () -> Unit = {},
) : Widget {

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        val hov = contains(mx, my)
        val bg  = if (hov) theme.surface1 else theme.surface0
        ctx.fill(x, y, x + width, y + height, bg)

        val tr = MinecraftClient.getInstance().textRenderer
        val lw = tr.getWidth(label)
        ctx.drawText(tr, label, x + (width - lw) / 2, y + (height - tr.fontHeight) / 2, theme.text, false)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() == 0 && contains(click.x().toInt(), click.y().toInt())) {
            onClick()
            return true
        }
        return false
    }
}
