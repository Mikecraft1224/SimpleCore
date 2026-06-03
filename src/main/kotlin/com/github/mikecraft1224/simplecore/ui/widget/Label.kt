package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.Alignment
import com.github.mikecraft1224.simplecore.ui.UiTheme
import com.github.mikecraft1224.simplecore.ui.Widget
import com.github.mikecraft1224.simplecore.ui.currentTheme
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

/**
 * A non-interactive text label.
 *
 * Accepts either a static string or a provider lambda for dynamic text:
 * ```kotlin
 * // Static label
 * val label = Label("Hello world")
 *
 * // Dynamic label - text is re-evaluated every frame
 * val live = Label { "A=$toggleA  B=$toggleB" }
 *
 * // Still mutable after construction
 * label.text = "Updated"
 * ```
 *
 * @param textProvider  lambda that returns the text to display each frame
 * @param theme         theme for default color fallback (defaults to [currentTheme])
 * @param color         explicit text color; -1 means use [UiTheme.text]
 * @param shadow        whether to draw a drop shadow
 */
open class Label(
    private var textProvider: () -> String,
    private val theme: UiTheme = currentTheme,
    private var color: Int = -1,
    private val shadow: Boolean = false,
    val textAlign: Alignment = Alignment.START,
) : Widget {

    /**
     * Convenience constructor for static text.
     *
     * @param text      text to display
     * @param theme     theme for default color fallback (defaults to [currentTheme])
     * @param color     explicit text color; -1 means use [UiTheme.text]
     * @param shadow    whether to draw a drop shadow
     * @param textAlign horizontal alignment of the text within the widget's bounds
     */
    constructor(
        text: String,
        theme: UiTheme = currentTheme,
        color: Int = -1,
        shadow: Boolean = false,
        textAlign: Alignment = Alignment.START,
    ) : this({ text }, theme, color, shadow, textAlign)

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    /** The currently displayed text. Getting evaluates the provider; setting replaces it with a constant. */
    var text: String
        get() = textProvider()
        set(value) {
            textProvider = { value }
        }

    private val resolvedColor get() = if (color == -1) theme.text else color

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        val tr = MinecraftClient.getInstance().textRenderer
        val ty = y + (height - tr.fontHeight) / 2
        val t  = text
        val tw = tr.getWidth(t)
        val tx = when (textAlign) {
            Alignment.START  -> x
            Alignment.CENTER -> x + (width - tw) / 2
            Alignment.END    -> x + width - tw
        }
        if (shadow) {
            ctx.drawTextWithShadow(tr, t, tx, ty, resolvedColor)
        } else {
            ctx.drawText(tr, t, tx, ty, resolvedColor, false)
        }
    }
}
