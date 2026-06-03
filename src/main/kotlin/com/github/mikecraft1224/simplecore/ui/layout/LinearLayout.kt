package com.github.mikecraft1224.simplecore.ui.layout

import com.github.mikecraft1224.simplecore.ui.Alignment
import com.github.mikecraft1224.simplecore.ui.Justify
import com.github.mikecraft1224.simplecore.ui.ModifiedWidget
import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.Widget

/**
 * Stacks children in a row (horizontal) or column (vertical).
 *
 * Each child is given a preferred size either explicitly or via a [ModifiedWidget] hint.
 * Children without a preferred size (and no weight) share the remaining space equally.
 * Children with a [ModifiedWidget.weightVal] > 0 share remaining space proportionally by weight.
 * [spacing] pixels are inserted between adjacent children.
 *
 * ```kotlin
 * // Vertical stack using the DSL
 * val stack = column(spacing = 4) {
 *     +Label("Title").height(20)
 *     +Toggle(isOn = { flag }, onChanged = { flag = it }).height(14).weight(0)
 *     +Spacer().weight(1)   // pushes following items to the bottom
 *     +Button("OK", onClick = { ... }).size(60, 20)
 * }
 * ```
 */
class LinearLayout(
    val direction: Direction,
    /** Pixel gap inserted between adjacent children. */
    val spacing: Int = 0,
    /**
     * How remaining space on the main axis is distributed when no flex/weighted items are present.
     * Has no effect when any child has [ModifiedWidget.weightVal] > 0 or no explicit preferred size.
     */
    val justify: Justify = Justify.START,
) : Panel() {

    /** Direction along which children are arranged. */
    enum class Direction { HORIZONTAL, VERTICAL }

    private data class ChildSpec(
        val widget: Widget,
        /** Fixed preferred size on the main axis; -1 = flex (no explicit preferred size). */
        val prefW: Int,
        val prefH: Int,
        /** Weight for proportional space distribution; 0 = not weighted. */
        val weight: Int,
    )

    private val specs = mutableListOf<ChildSpec>()

    /**
     * Adds [widget] with an optional explicit preferred size.
     *
     * If [widget] is a [ModifiedWidget], its hints are used as defaults and the explicit
     * [preferredWidth] / [preferredHeight] parameters override them when >= 0.
     *
     * @param preferredWidth  fixed width in pixels, or -1 to use the widget's own hint (or flex)
     * @param preferredHeight fixed height in pixels, or -1 to use the widget's own hint (or flex)
     */
    fun add(widget: Widget, preferredWidth: Int = -1, preferredHeight: Int = -1): LinearLayout {
        val mw = widget as? ModifiedWidget
        val pw = if (preferredWidth  >= 0) preferredWidth  else mw?.prefW  ?: -1
        val ph = if (preferredHeight >= 0) preferredHeight else mw?.prefH  ?: -1
        val wt = mw?.weightVal ?: 0
        specs.add(ChildSpec(widget, pw, ph, wt))
        super.add(widget)
        return this
    }

    /**
     * Adds [widget] reading all sizing hints from [ModifiedWidget] if present.
     * This override makes [LinearLayout] compatible with [com.github.mikecraft1224.simplecore.ui.LinearLayoutScope].
     */
    override fun add(widget: Widget): Panel = add(widget, -1, -1)

    override fun doLayout() {
        if (specs.isEmpty()) return

        val isHoriz = direction == Direction.HORIZONTAL
        val totalSpacing = spacing * (specs.size - 1).coerceAtLeast(0)

        if (isHoriz) {
            val fixedW = specs.sumOf { if (it.prefW >= 0 && it.weight == 0) it.prefW else 0 }
            val flexItems = specs.filter { it.prefW < 0 || it.weight > 0 }
            val flexTotal = (width - fixedW - totalSpacing).coerceAtLeast(0)

            val totalWeight = flexItems.sumOf { if (it.weight > 0) it.weight else 1 }

            // Justify only applies when there are no flex items
            val (startOffsetH, betweenGapH) = if (flexItems.isEmpty() && justify != Justify.START) {
                val extra = (width - fixedW - totalSpacing).coerceAtLeast(0)
                when (justify) {
                    Justify.CENTER       -> extra / 2 to 0
                    Justify.END          -> extra to 0
                    Justify.SPACE_BETWEEN -> {
                        val gaps = (specs.size - 1).coerceAtLeast(1)
                        0 to extra / gaps
                    }
                    Justify.SPACE_AROUND -> {
                        val unit = if (specs.isEmpty()) 0 else extra / specs.size
                        unit / 2 to unit
                    }
                    Justify.SPACE_EVENLY -> {
                        val unit = extra / (specs.size + 1)
                        unit to unit
                    }
                    else -> 0 to 0
                }
            } else 0 to 0

            var cx = x + startOffsetH
            for ((idx, spec) in specs.withIndex()) {
                val cw: Int
                val ch: Int
                if (spec.prefW >= 0 && spec.weight == 0) {
                    cw = spec.prefW
                } else {
                    val itemWeight = if (spec.weight > 0) spec.weight else 1
                    cw = if (totalWeight > 0) (flexTotal * itemWeight) / totalWeight else 0
                }
                ch = if (spec.prefH >= 0) spec.prefH else height

                val crossAlign = (spec.widget as? ModifiedWidget)?.vAlign ?: Alignment.START
                val cy = when (crossAlign) {
                    Alignment.START  -> y
                    Alignment.CENTER -> y + (height - ch) / 2
                    Alignment.END    -> y + height - ch
                }

                spec.widget.layout(cx, cy, cw, ch)
                cx += cw + spacing
                if (idx < specs.size - 1) cx += betweenGapH
            }
        } else {
            val fixedH = specs.sumOf { if (it.prefH >= 0 && it.weight == 0) it.prefH else 0 }
            val flexItems = specs.filter { it.prefH < 0 || it.weight > 0 }
            val flexTotal = (height - fixedH - totalSpacing).coerceAtLeast(0)

            val totalWeight = flexItems.sumOf { if (it.weight > 0) it.weight else 1 }

            val (startOffsetV, betweenGapV) = if (flexItems.isEmpty() && justify != Justify.START) {
                val extra = (height - fixedH - totalSpacing).coerceAtLeast(0)
                when (justify) {
                    Justify.CENTER       -> extra / 2 to 0
                    Justify.END          -> extra to 0
                    Justify.SPACE_BETWEEN -> {
                        val gaps = (specs.size - 1).coerceAtLeast(1)
                        0 to extra / gaps
                    }
                    Justify.SPACE_AROUND -> {
                        val unit = if (specs.isEmpty()) 0 else extra / specs.size
                        unit / 2 to unit
                    }
                    Justify.SPACE_EVENLY -> {
                        val unit = extra / (specs.size + 1)
                        unit to unit
                    }
                    else -> 0 to 0
                }
            } else 0 to 0

            var cy = y + startOffsetV
            for ((idx, spec) in specs.withIndex()) {
                val ch: Int
                val cw: Int
                if (spec.prefH >= 0 && spec.weight == 0) {
                    ch = spec.prefH
                } else {
                    val itemWeight = if (spec.weight > 0) spec.weight else 1
                    ch = if (totalWeight > 0) (flexTotal * itemWeight) / totalWeight else 0
                }
                cw = if (spec.prefW >= 0) spec.prefW else width

                val crossAlign = (spec.widget as? ModifiedWidget)?.hAlign ?: Alignment.START
                val cx = when (crossAlign) {
                    Alignment.START  -> x
                    Alignment.CENTER -> x + (width - cw) / 2
                    Alignment.END    -> x + width - cw
                }

                spec.widget.layout(cx, cy, cw, ch)
                cy += ch + spacing
                if (idx < specs.size - 1) cy += betweenGapV
            }
        }
    }
}
