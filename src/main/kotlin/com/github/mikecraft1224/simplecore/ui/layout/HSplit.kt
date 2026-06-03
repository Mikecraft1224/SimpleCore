package com.github.mikecraft1224.simplecore.ui.layout

import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.Widget

/**
 * Splits its bounds horizontally into a left pane and a right pane.
 *
 * [ratio] controls what fraction of the total width is given to the left child.
 * For example, `HSplit(0.3f)` gives 30% to the left and 70% to the right.
 *
 * Exactly two children must be added before [layout] is called: left first, then right.
 *
 * ```kotlin
 * val split = HSplit(0.3f)
 * split.add(sidebarPanel)
 * split.add(contentPanel)
 * ```
 */
class HSplit(
    /** Fraction of width given to the left child (0.0 .. 1.0). */
    private val ratio: Float,
) : Panel() {

    init {
        require(ratio in 0f..1f) { "HSplit ratio must be in 0..1, got $ratio" }
    }

    /**
     * Adds a child. The first child becomes the left pane; the second becomes the right pane.
     * Adding more than two children is silently ignored.
     */
    override fun add(widget: Widget): Panel {
        if (children.size >= 2) return this
        return super.add(widget)
    }

    override fun doLayout() {
        val left  = children.getOrNull(0) ?: return
        val right = children.getOrNull(1) ?: return
        val leftW = (width * ratio).toInt()
        left.layout(x, y, leftW, height)
        right.layout(x + leftW, y, width - leftW, height)
    }
}
