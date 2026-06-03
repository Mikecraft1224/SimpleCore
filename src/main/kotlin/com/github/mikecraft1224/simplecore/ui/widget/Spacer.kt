package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.Widget
import net.minecraft.client.gui.DrawContext

/**
 * An invisible widget that takes up space in layouts.
 *
 * Add without a preferred size to consume all remaining flex space in a [com.github.mikecraft1224.simplecore.ui.layout.LinearLayout],
 * effectively pushing subsequent items to the far edge.
 *
 * ```kotlin
 * row {
 *     add(Label("Title", theme), preferredWidth = 120, preferredHeight = 20)
 *     add(Spacer())          // pushes the button to the right
 *     add(Button("X", theme) { close() }, preferredWidth = 24, preferredHeight = 20)
 * }
 * ```
 */
class Spacer : Widget {
    override var x      = 0
    override var y      = 0
    override var width  = 0
    override var height = 0

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {}
}
