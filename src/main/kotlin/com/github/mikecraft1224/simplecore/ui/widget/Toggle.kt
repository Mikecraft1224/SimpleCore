package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.UiTheme
import com.github.mikecraft1224.simplecore.ui.Widget
import com.github.mikecraft1224.simplecore.ui.currentTheme
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext

/**
 * A pill-shaped toggle switch matching the config screen style.
 *
 * Track is 34x14 px; knob slides left (off) or right (on).
 *
 * ```kotlin
 * var enabled = false
 * val toggle = Toggle(theme, isOn = { enabled }, onChanged = { enabled = it })
 * ```
 *
 * @param theme     theme used for colors
 * @param isOn      supplier returning the current state
 * @param onChanged callback invoked with the new state when clicked
 */
class Toggle(
    private val theme: UiTheme = currentTheme,
    private val isOn: () -> Boolean,
    private val onChanged: (Boolean) -> Unit,
) : Widget {

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    private val TRACK_W = 34
    private val TRACK_H = 14
    private val KNOB_SZ = 10
    private val PAD     = 2

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        val on  = isOn()
        val hov = contains(mx, my)
        val tx  = x + (width  - TRACK_W) / 2
        val ty  = y + (height - TRACK_H) / 2

        val trackBg = when {
            on && hov -> lighten(theme.green)
            on        -> theme.green
            hov       -> theme.surface1
            else      -> theme.surface0
        }

        // Pill-shaped track: draw in sections so corners are never filled, avoiding
        // any dependency on knowing the background color.
        val c = trackBg
        ctx.fill(tx + 2, ty,             tx + TRACK_W - 2, ty + TRACK_H,     c) // centre column
        ctx.fill(tx,     ty + 2,         tx + 2,           ty + TRACK_H - 2, c) // left notch
        ctx.fill(tx + TRACK_W - 2, ty + 2, tx + TRACK_W,  ty + TRACK_H - 2, c) // right notch
        // Inner corner pixels (1x1)
        ctx.fill(tx + 1,            ty + 1,            tx + 2,            ty + 2,            c)
        ctx.fill(tx + TRACK_W - 2,  ty + 1,            tx + TRACK_W - 1,  ty + 2,            c)
        ctx.fill(tx + 1,            ty + TRACK_H - 2,  tx + 2,            ty + TRACK_H - 1,  c)
        ctx.fill(tx + TRACK_W - 2,  ty + TRACK_H - 2,  tx + TRACK_W - 1,  ty + TRACK_H - 1, c)

        // Pill-shaped knob using the same corner-free technique
        val knobX = if (on) tx + TRACK_W - KNOB_SZ - PAD else tx + PAD
        val knobY = ty + (TRACK_H - KNOB_SZ) / 2
        val k = theme.text
        ctx.fill(knobX + 1, knobY,           knobX + KNOB_SZ - 1, knobY + KNOB_SZ,     k)
        ctx.fill(knobX,     knobY + 1,       knobX + 1,            knobY + KNOB_SZ - 1, k)
        ctx.fill(knobX + KNOB_SZ - 1, knobY + 1, knobX + KNOB_SZ, knobY + KNOB_SZ - 1, k)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        if (click.button() == 0 && contains(click.x().toInt(), click.y().toInt())) {
            onChanged(!isOn())
            return true
        }
        return false
    }

    private fun lighten(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color shr 16) and 0xFF).plus(30).coerceAtMost(255)
        val g = ((color shr  8) and 0xFF).plus(30).coerceAtMost(255)
        val b = ( color         and 0xFF).plus(30).coerceAtMost(255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
