package com.github.mikecraft1224.simplecore.ui.layout

import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.Widget

/**
 * Lays out children at absolute positions relative to the panel's own top-left corner.
 *
 * Each child is added with explicit `(relX, relY, w, h)` offset from the panel origin.
 * Useful for precise manual placement and for overlay-style compositions.
 *
 * ```kotlin
 * val frame = FrameLayout()
 * frame.add(Button("OK", theme), relX = 10, relY = 10, w = 60, h = 20)
 * frame.add(Label("Title", theme), relX = 10, relY = 0, w = 120, h = 16)
 * ```
 */
class FrameLayout : Panel() {

    private data class ChildSpec(val widget: Widget, val relX: Int, val relY: Int, val w: Int, val h: Int)

    private val frameSpecs = mutableListOf<ChildSpec>()

    /**
     * Adds [widget] at ([relX], [relY]) relative to this panel, with size [w] x [h].
     */
    fun add(widget: Widget, relX: Int, relY: Int, w: Int, h: Int): FrameLayout {
        frameSpecs.add(ChildSpec(widget, relX, relY, w, h))
        super.add(widget)
        return this
    }

    // Prevent the base add() from being called without positional data.
    override fun add(widget: Widget): Panel {
        throw UnsupportedOperationException("Use FrameLayout.add(widget, relX, relY, w, h)")
    }

    override fun doLayout() {
        for (spec in frameSpecs) {
            spec.widget.layout(x + spec.relX, y + spec.relY, spec.w, spec.h)
        }
    }
}
