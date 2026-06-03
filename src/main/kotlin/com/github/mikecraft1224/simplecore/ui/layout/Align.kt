package com.github.mikecraft1224.simplecore.ui.layout

import com.github.mikecraft1224.simplecore.ui.Alignment
import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.Widget

/**
 * Positions a single child within this panel's bounds using [hAlign] and [vAlign].
 *
 * If [childWidth] or [childHeight] is -1, the child fills the available space on that axis.
 * Otherwise the child is given the explicit size and positioned per the alignment.
 *
 * ```kotlin
 * // Center a 34x14 toggle inside a full-width column slot
 * col.add(Align(toggle, childWidth = 34, childHeight = 14), preferredHeight = 24)
 *
 * // Right-align a label
 * col.add(Align(label, childHeight = 20, hAlign = Alignment.END), preferredHeight = 20)
 *
 * // Or use the builder shorthand
 * col.add(center(toggle, childWidth = 34, childHeight = 14), preferredHeight = 24)
 * ```
 */
class Align(
    private val child: Widget,
    val childWidth: Int  = -1,
    val childHeight: Int = -1,
    val hAlign: Alignment = Alignment.CENTER,
    val vAlign: Alignment = Alignment.CENTER,
) : Panel() {

    init { super.add(child) }

    // Align wraps exactly one child.
    override fun add(widget: Widget): Panel = error("Align wraps exactly one child; pass it in the constructor")

    override fun doLayout() {
        val cw = if (childWidth  > 0) childWidth  else width
        val ch = if (childHeight > 0) childHeight else height
        val cx = when (hAlign) {
            Alignment.START  -> x
            Alignment.CENTER -> x + (width  - cw) / 2
            Alignment.END    -> x + width  - cw
        }
        val cy = when (vAlign) {
            Alignment.START  -> y
            Alignment.CENTER -> y + (height - ch) / 2
            Alignment.END    -> y + height - ch
        }
        child.layout(cx, cy, cw, ch)
    }
}
