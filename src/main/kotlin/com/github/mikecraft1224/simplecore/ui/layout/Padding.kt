package com.github.mikecraft1224.simplecore.ui.layout

import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.Widget

/**
 * Insets a single child by the specified edge padding values.
 *
 * ```kotlin
 * // Uniform padding
 * Padding(content, all = 8)
 *
 * // Symmetric padding (8px left/right, 4px top/bottom)
 * Padding(content, h = 8, v = 4)
 *
 * // Per-edge padding
 * Padding(content, top = 4, right = 8, bottom = 4, left = 8)
 *
 * // Builder shorthand
 * padding(content, 8)
 * padding(content, h = 8, v = 4)
 * ```
 */
class Padding(
    private val child: Widget,
    val top:    Int = 0,
    val right:  Int = 0,
    val bottom: Int = 0,
    val left:   Int = 0,
) : Panel() {

    init { super.add(child) }

    // Padding wraps exactly one child.
    override fun add(widget: Widget): Panel = error("Padding wraps exactly one child; pass it in the constructor")

    override fun doLayout() {
        child.layout(
            x + left,
            y + top,
            (width  - left - right).coerceAtLeast(0),
            (height - top  - bottom).coerceAtLeast(0),
        )
    }
}
