@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.ui


/**
 * Wraps any [Widget] with optional sizing, padding, and alignment hints consumed by layouts.
 *
 * Obtain instances via the extension functions rather than calling the constructor directly:
 * ```kotlin
 * Label("Title").height(20).align(h = Alignment.CENTER)
 * Button("OK", onClick = { ... }).width(60).height(20).padding(4)
 * Spacer().weight(1)
 * ```
 *
 * All [Widget] calls are delegated to [inner] except [layout] and [contains], which operate on
 * the allocated bounds ([x], [y], [width], [height]) assigned by the parent layout.
 *
 * @param inner     the wrapped widget
 * @param prefW     preferred width in pixels; -1 = fill available space
 * @param prefH     preferred height in pixels; -1 = fill available space
 * @param weightVal share of remaining flex space; 0 = not weight-based, >0 = weighted
 * @param padTop    top padding in pixels
 * @param padRight  right padding in pixels
 * @param padBottom bottom padding in pixels
 * @param padLeft   left padding in pixels
 * @param hAlign    horizontal alignment within the allocated slot
 * @param vAlign    vertical alignment within the allocated slot
 */
class ModifiedWidget internal constructor(
    val inner: Widget,
    val prefW: Int = -1,
    val prefH: Int = -1,
    val weightVal: Int = 0,
    val padTop: Int = 0,
    val padRight: Int = 0,
    val padBottom: Int = 0,
    val padLeft: Int = 0,
    val hAlign: Alignment = Alignment.START,
    val vAlign: Alignment = Alignment.START,
) : Widget by inner {

    // Allocated bounds tracked independently of inner's rendered bounds
    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height

        // Compute content size
        val cw = if (prefW > 0) prefW.coerceAtMost(width) else width
        val ch = if (prefH > 0) prefH.coerceAtMost(height) else height

        // Apply alignment within the allocated slot
        val cx = when (hAlign) {
            Alignment.START  -> x
            Alignment.CENTER -> x + (width - cw) / 2
            Alignment.END    -> x + width - cw
        }
        val cy = when (vAlign) {
            Alignment.START  -> y
            Alignment.CENTER -> y + (height - ch) / 2
            Alignment.END    -> y + height - ch
        }

        // Apply padding and forward to inner
        inner.layout(
            cx + padLeft,
            cy + padTop,
            (cw - padLeft - padRight).coerceAtLeast(0),
            (ch - padTop - padBottom).coerceAtLeast(0),
        )
    }

    override fun contains(px: Int, py: Int): Boolean =
        px in x until x + width && py in y until y + height

    /** Returns a copy of this [ModifiedWidget] with updated fields. */
    internal fun copyWith(
        prefW: Int    = this.prefW,
        prefH: Int    = this.prefH,
        weightVal: Int = this.weightVal,
        padTop: Int   = this.padTop,
        padRight: Int = this.padRight,
        padBottom: Int = this.padBottom,
        padLeft: Int  = this.padLeft,
        hAlign: Alignment = this.hAlign,
        vAlign: Alignment = this.vAlign,
    ): ModifiedWidget = ModifiedWidget(inner, prefW, prefH, weightVal, padTop, padRight, padBottom, padLeft, hAlign, vAlign)
}

// -- Extension functions -------------------------------------------------------

/**
 * Sets the preferred width hint.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 */
fun Widget.width(px: Int): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(prefW = px) else ModifiedWidget(this, prefW = px)

/**
 * Sets the preferred height hint.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 */
fun Widget.height(px: Int): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(prefH = px) else ModifiedWidget(this, prefH = px)

/**
 * Sets both preferred width and height hints.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 */
fun Widget.size(w: Int, h: Int): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(prefW = w, prefH = h) else ModifiedWidget(this, prefW = w, prefH = h)

/**
 * Explicitly marks this widget as filling available width (the default; useful for clarity).
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 */
fun Widget.fillWidth(): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(prefW = -1) else ModifiedWidget(this, prefW = -1)

/**
 * Explicitly marks this widget as filling available height (the default; useful for clarity).
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 */
fun Widget.fillHeight(): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(prefH = -1) else ModifiedWidget(this, prefH = -1)

/**
 * Applies uniform padding on all four sides.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 */
fun Widget.padding(px: Int): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(padTop = px, padRight = px, padBottom = px, padLeft = px)
    else ModifiedWidget(this, padTop = px, padRight = px, padBottom = px, padLeft = px)

/**
 * Applies symmetric horizontal and vertical padding.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 *
 * @param h left and right padding
 * @param v top and bottom padding
 */
fun Widget.padding(h: Int = 0, v: Int = 0): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(padTop = v, padRight = h, padBottom = v, padLeft = h)
    else ModifiedWidget(this, padTop = v, padRight = h, padBottom = v, padLeft = h)

/**
 * Applies per-edge padding.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 */
fun Widget.padding(top: Int, right: Int, bottom: Int, left: Int): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(padTop = top, padRight = right, padBottom = bottom, padLeft = left)
    else ModifiedWidget(this, padTop = top, padRight = right, padBottom = bottom, padLeft = left)

/**
 * Sets the alignment within the allocated layout slot.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 *
 * @param h horizontal alignment (default [Alignment.CENTER])
 * @param v vertical alignment (default [Alignment.CENTER])
 */
fun Widget.align(h: Alignment = Alignment.CENTER, v: Alignment = Alignment.CENTER): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(hAlign = h, vAlign = v) else ModifiedWidget(this, hAlign = h, vAlign = v)

/**
 * Sets the weight for proportional space distribution in [com.github.mikecraft1224.simplecore.ui.layout.LinearLayout].
 * Items with weight > 0 share the remaining space proportionally.
 * When [this] is already a [ModifiedWidget], updates the existing spec rather than nesting.
 *
 * @param n weight value (must be >= 1 to participate in weighted distribution)
 */
fun Widget.weight(n: Int): ModifiedWidget =
    if (this is ModifiedWidget) copyWith(weightVal = n) else ModifiedWidget(this, weightVal = n)
