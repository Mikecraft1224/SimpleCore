package com.github.mikecraft1224.simplecore.ui.layout

import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.Widget

/**
 * Splits its bounds vertically into a top pane and a bottom pane.
 *
 * [ratio] controls what fraction of the total height is given to the top child.
 * For example, `VSplit(0.25f)` gives 25% to the top and 75% to the bottom.
 *
 * Exactly two children must be added before [layout] is called: top first, then bottom.
 *
 * ```kotlin
 * val split = VSplit(0.25f)
 * split.add(headerPanel)
 * split.add(bodyPanel)
 * ```
 */
class VSplit(
    /** Fraction of height given to the top child (0.0 .. 1.0). */
    private val ratio: Float,
) : Panel() {

    init {
        require(ratio in 0f..1f) { "VSplit ratio must be in 0..1, got $ratio" }
    }

    /**
     * Adds a child. The first child becomes the top pane; the second becomes the bottom pane.
     * Adding more than two children is silently ignored.
     */
    override fun add(widget: Widget): Panel {
        if (children.size >= 2) return this
        return super.add(widget)
    }

    override fun doLayout() {
        val top    = children.getOrNull(0) ?: return
        val bottom = children.getOrNull(1) ?: return
        val topH   = (height * ratio).toInt()
        top.layout(x, y, width, topH)
        bottom.layout(x, y + topH, width, height - topH)
    }
}
