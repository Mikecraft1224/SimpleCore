package com.github.mikecraft1224.simplecore.config.screen.overlays

import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_DIM
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_MANTLE
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SUBTEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE0
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE1
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_TEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigOverlay
import com.github.mikecraft1224.simplecore.config.screen.ConfigScreenCtx
import com.github.mikecraft1224.simplecore.config.screen.ScTextField
import com.github.mikecraft1224.simplecore.utils.Color as SimpleColor
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW

class ColorPickerOverlay(
    private val entry: ProcessedEntry.ColorEntry,
    private val ctx: ConfigScreenCtx,
    private val closeCallback: () -> Unit,
) : ConfigOverlay {

    private companion object {
        const val DRAG_NONE     = -1
        const val DRAG_SPECTRUM =  0
        const val DRAG_V        =  1
        const val DRAG_A        =  2
    }

    private var h: Float
    private var s: Float
    private var v: Float
    private var a: Int = entry.get().alpha

    init {
        val hsv = rgbToHsv((entry.get().red shl 16) or (entry.get().green shl 8) or entry.get().blue)
        h = hsv[0]; s = hsv[1]; v = hsv[2]
    }

    private var dragging = DRAG_NONE

    private val hexField = ScTextField(0, 0, 1, 16, argbToHex(currentArgb())).also { tf ->
        tf.onChange = { txt -> parseHexInput(txt) }
    }

    private val DLG_W  = 220
    private val DLG_H  = 240
    private val SPEC_W = 160
    private val SPEC_H = 100
    private val SL_W   = 160
    private val SL_H   = 12
    private val PAD    = 16

    private fun dlgX()  = (ctx.getW() - DLG_W) / 2
    private fun dlgY()  = (ctx.getH() - DLG_H) / 2
    private fun specX() = dlgX() + (DLG_W - SPEC_W) / 2
    private fun specY() = dlgY() + 26
    private fun slX()   = dlgX() + (DLG_W - SL_W) / 2
    private fun slVY()  = specY() + SPEC_H + 10
    private fun slAY()  = slVY() + SL_H + 14

    private fun currentArgb(): Int = hsvToArgb(h, s, v, a)

    private fun argbToHex(argb: Int): String {
        val r  = (argb shr 16) and 0xFF
        val g  = (argb shr  8) and 0xFF
        val b  = argb          and 0xFF
        val al = (argb ushr 24) and 0xFF
        return "#%02X%02X%02X%02X".format(r, g, b, al)
    }

    private fun parseHexInput(txt: String) {
        val clean = txt.trimStart('#')
        if (clean.length == 8) {
            runCatching {
                val rgb = clean.substring(0, 6).toLong(16).toInt()
                a = clean.substring(6, 8).toInt(16)
                val hsv = rgbToHsv(rgb)
                h = hsv[0]; s = hsv[1]; v = hsv[2]
            }
        } else if (clean.length == 6) {
            runCatching {
                val rgb = clean.toLong(16).toInt()
                val hsv = rgbToHsv(rgb)
                h = hsv[0]; s = hsv[1]; v = hsv[2]
            }
        }
    }

    private fun syncHexField() {
        if (!hexField.focused) hexField.setText(argbToHex(currentArgb()))
    }

    override fun hitTests(mx: Int, my: Int): Boolean = true

    override fun render(state: GuiGraphicsExtractor, mx: Int, my: Int) {
        val dx = dlgX(); val dy = dlgY()
        val sx = specX(); val sy = specY()

        val fieldW = DLG_W - PAD - 28 - 4 - (DLG_W - SL_W) / 2
        val fieldX = slX()
        val fieldY = slAY() + SL_H + 10
        hexField.x = fieldX; hexField.y = fieldY; hexField.w = fieldW

        state.fill(0, 0, ctx.getW(), ctx.getH(), C_DIM)
        state.fill(dx, dy, dx + DLG_W, dy + DLG_H, C_MANTLE)
        state.fill(dx,             dy,             dx + DLG_W, dy + 1,           C_SURFACE1)
        state.fill(dx,             dy + DLG_H - 1, dx + DLG_W, dy + DLG_H,      C_SURFACE1)
        state.fill(dx,             dy,             dx + 1,     dy + DLG_H,       C_SURFACE1)
        state.fill(dx + DLG_W - 1, dy,             dx + DLG_W, dy + DLG_H,      C_SURFACE1)
        state.centeredText(ctx.tr, "Edit Color", dx + DLG_W / 2, dy + 9, C_TEXT)

        for (px in 0 until SPEC_W) {
            val colH = px.toFloat() / SPEC_W * 360f
            state.fillGradient(sx + px, sy, sx + px + 1, sy + SPEC_H,
                hsvToArgb(colH, 1f, v, 0xFF),
                hsvToArgb(colH, 0f, v, 0xFF),
            )
        }
        state.fill(sx - 1, sy - 1, sx + SPEC_W + 1, sy,              C_SURFACE1)
        state.fill(sx - 1, sy + SPEC_H, sx + SPEC_W + 1, sy + SPEC_H + 1, C_SURFACE1)
        state.fill(sx - 1, sy - 1, sx,              sy + SPEC_H + 1, C_SURFACE1)
        state.fill(sx + SPEC_W, sy - 1, sx + SPEC_W + 1, sy + SPEC_H + 1, C_SURFACE1)
        val cpX = sx + (h / 360f * SPEC_W).toInt()
        val cpY = sy + ((1f - s) * SPEC_H).toInt()
        val crossShadow = SimpleColor(0, 0, 0, 0x88).argb
        val crossWhite  = SimpleColor.WHITE.argb
        state.fill(cpX - 4, cpY - 1, cpX + 5, cpY + 2, crossShadow)
        state.fill(cpX - 1, cpY - 4, cpX + 2, cpY + 5, crossShadow)
        state.fill(cpX - 3, cpY,     cpX + 4, cpY + 1, crossWhite)
        state.fill(cpX,     cpY - 3, cpX + 1, cpY + 4, crossWhite)

        val vx = slX(); val vy = slVY()
        for (px in 0 until SL_W) {
            val bv = px.toFloat() / SL_W
            state.fill(vx + px, vy, vx + px + 1, vy + SL_H, hsvToArgb(h, s, bv, 0xFF))
        }
        state.text(ctx.tr, "B", vx - 12, vy + (SL_H - ctx.tr.lineHeight) / 2, C_SUBTEXT, false)
        val vKnobX = vx + (v * SL_W).toInt()
        val vHov = dragging == DRAG_V || (mx in vx until vx + SL_W && my in vy - 3 until vy + SL_H + 3)
        state.fill(vKnobX - 3, vy - 2, vKnobX + 3, vy + SL_H + 2, if (vHov) C_TEXT else C_SUBTEXT)

        val ax = slX(); val ay = slAY()
        for (px in 0 until SL_W) {
            val opaqueCol = hsvToArgb(h, s, v, 0xFF)
            val r2 = ((opaqueCol shr 16) and 0xFF)
            val g2 = ((opaqueCol shr  8) and 0xFF)
            val b2 = (opaqueCol and 0xFF)
            val blended = (0xFF shl 24) or
                (((r2 * (px * 255 / SL_W) + 0x1E * (255 - px * 255 / SL_W)) / 255).coerceIn(0,255) shl 16) or
                (((g2 * (px * 255 / SL_W) + 0x1E * (255 - px * 255 / SL_W)) / 255).coerceIn(0,255) shl 8) or
                ((b2 * (px * 255 / SL_W) + 0x1E * (255 - px * 255 / SL_W)) / 255).coerceIn(0,255)
            state.fill(ax + px, ay, ax + px + 1, ay + SL_H, blended)
        }
        state.text(ctx.tr, "A", ax - 12, ay + (SL_H - ctx.tr.lineHeight) / 2, C_SUBTEXT, false)
        val aKnobX = ax + (a * SL_W / 255)
        val aHov = dragging == DRAG_A || (mx in ax until ax + SL_W && my in ay - 3 until ay + SL_H + 3)
        state.fill(aKnobX - 3, ay - 2, aKnobX + 3, ay + SL_H + 2, if (aHov) C_TEXT else C_SUBTEXT)

        val previewY = slAY() + SL_H + 10
        val swW = 28; val swH = 16
        val swX = dx + DLG_W - PAD - swW
        val swY = previewY
        val checkerDark  = SimpleColor(0x88, 0x88, 0x88).argb
        val checkerLight = SimpleColor(0xAA, 0xAA, 0xAA).argb
        state.fill(swX, swY, swX + swW, swY + swH, checkerDark)
        state.fill(swX,        swY,        swX + swW / 2, swY + swH / 2, checkerLight)
        state.fill(swX + swW / 2, swY + swH / 2, swX + swW, swY + swH, checkerLight)
        state.fill(swX, swY, swX + swW, swY + swH, currentArgb())
        state.fill(swX - 1, swY - 1, swX + swW + 1, swY,         C_SURFACE1)
        state.fill(swX - 1, swY + swH, swX + swW + 1, swY + swH + 1, C_SURFACE1)
        state.fill(swX - 1, swY - 1, swX,            swY + swH + 1, C_SURFACE1)
        state.fill(swX + swW, swY - 1, swX + swW + 1, swY + swH + 1, C_SURFACE1)

        val btnY    = dy + DLG_H - 24
        val bw      = 74; val bh = 16
        val applyX  = dx + DLG_W / 2 - bw - 2
        val cancelX = dx + DLG_W / 2 + 2
        val applyHov  = mx in applyX  until applyX  + bw && my in btnY until btnY + bh
        val cancelHov = mx in cancelX until cancelX + bw && my in btnY until btnY + bh
        state.fill(applyX,  btnY, applyX  + bw, btnY + bh, if (applyHov)  C_SURFACE1 else C_SURFACE0)
        state.fill(cancelX, btnY, cancelX + bw, btnY + bh, if (cancelHov) C_SURFACE1 else C_SURFACE0)
        state.centeredText(ctx.tr, "Apply",  applyX  + bw / 2, btnY + (bh - ctx.tr.lineHeight) / 2, C_TEXT)
        state.centeredText(ctx.tr, "Cancel", cancelX + bw / 2, btnY + (bh - ctx.tr.lineHeight) / 2, C_TEXT)

        hexField.render(state, mx, my)
        syncHexField()
    }

    override fun mouseClicked(mx: Int, my: Int): Boolean {
        val dx = dlgX(); val dy = dlgY()
        val sx = specX(); val sy = specY()
        if (mx in sx until sx + SPEC_W && my in sy until sy + SPEC_H) {
            dragging = DRAG_SPECTRUM; applySpectrum(mx, my); return true
        }
        val vx = slX(); val vy = slVY()
        if (mx in vx until vx + SL_W && my in vy - 4 until vy + SL_H + 4) {
            dragging = DRAG_V; applyVSlider(mx); return true
        }
        val ax = slX(); val ay = slAY()
        if (mx in ax until ax + SL_W && my in ay - 4 until ay + SL_H + 4) {
            dragging = DRAG_A; applyASlider(mx); return true
        }
        val btnY    = dy + DLG_H - 24; val bw = 74; val bh = 16
        val applyX  = dx + DLG_W / 2 - bw - 2
        val cancelX = dx + DLG_W / 2 + 2
        if (mx in applyX until applyX + bw && my in btnY until btnY + bh) {
            commitColor(); return true
        }
        if (mx in cancelX until cancelX + bw && my in btnY until btnY + bh) {
            closeCallback(); return true
        }
        if (hexField.mouseClicked(mx, my)) return true
        if (mx !in dx until dx + DLG_W || my !in dy until dy + DLG_H) closeCallback()
        return true
    }

    override fun mouseDragged(mx: Int, my: Int): Boolean {
        when (dragging) {
            DRAG_SPECTRUM -> applySpectrum(mx, my)
            DRAG_V        -> applyVSlider(mx)
            DRAG_A        -> applyASlider(mx)
            else          -> return false
        }
        return true
    }

    override fun mouseReleased(): Boolean {
        dragging = DRAG_NONE
        return false
    }

    override fun keyPressed(keyCode: Int, mods: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { closeCallback(); return true }
        if (hexField.focused) return hexField.keyPressed(keyCode, mods)
        return false
    }

    override fun charTyped(chr: Char): Boolean {
        if (hexField.focused) return hexField.charTyped(chr)
        return false
    }

    private fun applySpectrum(mx: Int, my: Int) {
        val sx = specX(); val sy = specY()
        h = ((mx - sx).toFloat() / SPEC_W * 360f).coerceIn(0f, 360f)
        s = (1f - (my - sy).toFloat() / SPEC_H).coerceIn(0f, 1f)
    }

    private fun applyVSlider(mx: Int) {
        v = ((mx - slX()).toFloat() / SL_W).coerceIn(0f, 1f)
    }

    private fun applyASlider(mx: Int) {
        a = ((mx - slX()).toFloat() / SL_W * 255f).toInt().coerceIn(0, 255)
    }

    private fun commitColor() {
        val rgb = hsvToRgb(h, s, v)
        val newColor = SimpleColor((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, a)
        entry.set(newColor.toAwtColor())
        closeCallback()
    }

    /** Converts HSV (h in 0..360, s/v in 0..1) to packed ARGB with the given alpha (0..255). */
    private fun hsvToArgb(h: Float, s: Float, v: Float, a: Int): Int {
        val rgb = hsvToRgb(h, s, v)
        return (a shl 24) or (rgb and 0x00FFFFFF)
    }

    /** Converts HSV (h in 0..360, s/v in 0..1) to packed 0xRRGGBB. */
    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val i  = (hh / 60f).toInt()
        val f  = hh / 60f - i
        val p  = v * (1f - s)
        val q  = v * (1f - s * f)
        val t  = v * (1f - s * (1f - f))
        val (r, g, b) = when (i % 6) {
            0    -> Triple(v, t, p)
            1    -> Triple(q, v, p)
            2    -> Triple(p, v, t)
            3    -> Triple(p, q, v)
            4    -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    /** Converts packed 0xRRGGBB to FloatArray(h, s, v) where h in 0..360, s/v in 0..1. */
    private fun rgbToHsv(rgb: Int): FloatArray {
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr  8) and 0xFF) / 255f
        val b =  (rgb         and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d   = max - min
        val v   = max
        val s   = if (max == 0f) 0f else d / max
        val h   = when {
            d == 0f  -> 0f
            max == r -> 60f * ((g - b) / d % 6f)
            max == g -> 60f * ((b - r) / d + 2f)
            else     -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return floatArrayOf(h, s, v)
    }
}
