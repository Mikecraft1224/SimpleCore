@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.ui

import com.github.mikecraft1224.simplecore.ui.layout.*
import com.github.mikecraft1224.simplecore.ui.layout.LinearLayout.Direction.HORIZONTAL
import com.github.mikecraft1224.simplecore.ui.layout.LinearLayout.Direction.VERTICAL

/** DSL marker preventing scope-leaking in nested builders. */
@DslMarker
annotation class UiDsl

/**
 * Receiver for [column] and [row] DSL blocks.
 *
 * Use the unary-plus operator to add a widget to the layout:
 * ```kotlin
 * column(4) {
 *     +Label("Title").height(20)
 *     +Spacer()
 *     +Button("Close", onClick = { ... }).width(60).height(20)
 * }
 * ```
 */
@UiDsl
class LinearLayoutScope(val layout: LinearLayout) {
    /** Adds this widget to the enclosing [LinearLayout]. */
    operator fun Widget.unaryPlus() {
        layout.add(this)
    }
}

/**
 * Receiver for [frame] DSL blocks.
 *
 * Use [Widget.at] to place widgets at explicit relative positions:
 * ```kotlin
 * frame {
 *     Label("Title").at(0, 0, 200, 20)
 *     Button("OK", onClick = { ... }).size(60, 20).at(10, 30)
 * }
 * ```
 */
@UiDsl
class FrameScope(val layout: FrameLayout) {
    /**
     * Adds this widget at ([relX], [relY]) relative to the frame's top-left corner.
     *
     * If [w] or [h] is omitted (< 0), the value is read from the widget's [ModifiedWidget] hints.
     * If still absent, defaults to 0.
     *
     * @param relX horizontal offset from the frame's left edge
     * @param relY vertical offset from the frame's top edge
     * @param w    widget width in pixels; -1 = read from [ModifiedWidget.prefW]
     * @param h    widget height in pixels; -1 = read from [ModifiedWidget.prefH]
     */
    fun Widget.at(relX: Int, relY: Int, w: Int = -1, h: Int = -1) {
        val rw = if (w >= 0) w else (this as? ModifiedWidget)?.prefW ?: 0
        val rh = if (h >= 0) h else (this as? ModifiedWidget)?.prefH ?: 0
        layout.add(this, relX, relY, rw, rh)
    }
}

// -- Layout builders -----------------------------------------------------------

/**
 * Creates a vertical [LinearLayout] and applies [init] to it via [LinearLayoutScope].
 *
 * ```kotlin
 * val stack = column(spacing = 4) {
 *     +Label("Title").height(20)
 *     +Toggle(isOn = { enabled }, onChanged = { enabled = it }).height(14)
 * }
 * ```
 */
fun column(spacing: Int = 0, justify: Justify = Justify.START, init: LinearLayoutScope.() -> Unit = {}): LinearLayout =
    LinearLayout(VERTICAL, spacing, justify).also { LinearLayoutScope(it).init() }

/**
 * Creates a horizontal [LinearLayout] and applies [init] to it via [LinearLayoutScope].
 *
 * ```kotlin
 * val bar = row(spacing = 4) {
 *     +Label("Name").width(80).height(20)
 *     +Spacer()
 *     +Button("OK", onClick = { ... }).width(60).height(20)
 * }
 * ```
 */
fun row(spacing: Int = 0, justify: Justify = Justify.START, init: LinearLayoutScope.() -> Unit = {}): LinearLayout =
    LinearLayout(HORIZONTAL, spacing, justify).also { LinearLayoutScope(it).init() }

/**
 * Creates a [FrameLayout] and applies [init] to it via [FrameScope].
 *
 * ```kotlin
 * val overlay = frame {
 *     Label("Hello").at(8, 8, 184, 20)
 *     Button("OK", onClick = { ... }).size(60, 20).at(10, 36)
 * }
 * ```
 */
fun frame(init: FrameScope.() -> Unit = {}): FrameLayout =
    FrameLayout().also { FrameScope(it).init() }

/**
 * Creates an [HSplit] from [left] and [right] panels at the given [ratio] (0..1).
 *
 * ```kotlin
 * val split = hSplit(0.3f, sidebar, content)
 * ```
 */
fun hSplit(ratio: Float = 0.5f, left: Panel, right: Panel): HSplit =
    HSplit(ratio).also { it.add(left); it.add(right) }

/**
 * Creates a [VSplit] from [top] and [bottom] panels at the given [ratio] (0..1).
 *
 * ```kotlin
 * val split = vSplit(0.15f, header, body)
 * ```
 */
fun vSplit(ratio: Float = 0.5f, top: Panel, bottom: Panel): VSplit =
    VSplit(ratio).also { it.add(top); it.add(bottom) }

/**
 * Creates a horizontal [LinearLayout] where each widget is given a proportional share of the
 * total width determined by its weight value.
 *
 * ```kotlin
 * val bar = weightedRow(content to 3, sidebar to 1, spacing = 4)
 * ```
 *
 * @param pairs   widget-to-weight pairs; weight must be >= 1
 * @param spacing pixel gap between children
 */
fun weightedRow(vararg pairs: Pair<Widget, Int>, spacing: Int = 0): LinearLayout =
    row(spacing) { pairs.forEach { (w, wt) -> +w.weight(wt) } }

// -- Padding -------------------------------------------------------------------

/** Wraps [child] with [px] padding on all four sides. */
fun padding(child: Widget, px: Int): Padding =
    Padding(child, top = px, right = px, bottom = px, left = px)

/** Wraps [child] with [h] horizontal (left/right) and [v] vertical (top/bottom) padding. */
fun padding(child: Widget, h: Int = 0, v: Int = 0): Padding =
    Padding(child, top = v, right = h, bottom = v, left = h)

// -- Alignment wrappers --------------------------------------------------------

/**
 * Centers [child] within the available space on both axes.
 *
 * Supply [childWidth] and/or [childHeight] when the child's natural size is smaller
 * than the slot it occupies (e.g. a toggle inside a full-width column row).
 *
 * ```kotlin
 * // Center a 34x14 toggle in a 24px-tall column slot
 * col.add(center(toggle, childWidth = 34, childHeight = 14), preferredHeight = 24)
 * ```
 */
fun center(child: Widget, childWidth: Int = -1, childHeight: Int = -1): Align =
    Align(child, childWidth, childHeight, Alignment.CENTER, Alignment.CENTER)

/**
 * Positions [child] within the available space with explicit [h] and [v] alignment.
 *
 * ```kotlin
 * // Right-align a label in a column slot
 * col.add(align(label, h = Alignment.END, childHeight = 20), preferredHeight = 20)
 * ```
 */
fun align(
    child: Widget,
    h: Alignment = Alignment.CENTER,
    v: Alignment = Alignment.CENTER,
    childWidth:  Int = -1,
    childHeight: Int = -1,
): Align = Align(child, childWidth, childHeight, h, v)
