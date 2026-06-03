package com.github.mikecraft1224.simplecore.config.screen.overlays

import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_MANTLE
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_ROW_HOVER
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SCROLLBAR_THUMB
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SCROLLBAR_TRACK
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE0
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE1
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_TEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.SLIDER_INP
import com.github.mikecraft1224.simplecore.config.screen.ConfigOverlay
import com.github.mikecraft1224.simplecore.config.screen.ConfigScreenCtx
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import kotlin.math.max

class ElementEditOverlay(
    private val entry: ProcessedEntry.ObjectListEntry,
    var elementIndex: Int,
    private val ctx: ConfigScreenCtx,
    private val dlgXProvider: () -> Int,
    private val dlgYProvider: () -> Int,
    private val closeCallback: () -> Unit,
) : ConfigOverlay {

    val DLG_W  = 300
    val DLG_H  = 320
    private val ROW_H_ELEM = 26
    private val PAD        = 8
    private var scroll     = 0
    var elementDropdown: DropdownOverlay? = null

    private val elementEntries: List<ProcessedEntry> get() {
        val list = entry.getList()
        return if (elementIndex < list.size) entry.buildElementEntries(list[elementIndex]) else emptyList()
    }

    val dlgX   get() = dlgXProvider()
    val dlgY   get() = dlgYProvider()
    private val lstTop get() = dlgY + 34
    private val lstBot get() = dlgY + DLG_H - 32
    private val visH   get() = lstBot - lstTop
    private val maxSc  get() = max(0, elementEntries.size * ROW_H_ELEM - visH)
    private fun wX()   = dlgX + (DLG_W * 0.58f).toInt()
    private fun wW()   = (DLG_W * 0.34f).toInt()

    override fun hitTests(mx: Int, my: Int): Boolean =
        mx in dlgX until dlgX + DLG_W && my in dlgY until dlgY + DLG_H

    override fun render(ctx: DrawContext, mx: Int, my: Int) {
        val dx = dlgX; val dy = dlgY
        ctx.fill(dx, dy, dx + DLG_W, dy + DLG_H, C_MANTLE)
        ctx.fill(dx,             dy,             dx + DLG_W, dy + 1,      C_SURFACE1)
        ctx.fill(dx,             dy + DLG_H - 1, dx + DLG_W, dy + DLG_H, C_SURFACE1)
        ctx.fill(dx,             dy,             dx + 1,     dy + DLG_H,  C_SURFACE1)
        ctx.fill(dx + DLG_W - 1, dy,             dx + DLG_W, dy + DLG_H, C_SURFACE1)
        val list = entry.getList()
        val title = if (elementIndex < list.size)
            "Item ${elementIndex + 1}: ${entry.elementLabel(list[elementIndex])}"
        else
            "Item ${elementIndex + 1}"
        ctx.drawCenteredTextWithShadow(this.ctx.tr, title, dx + DLG_W / 2, dy + 7, C_TEXT)
        ctx.fill(dx + 1, dy + 19, dx + DLG_W - 1, dy + 20, C_SURFACE0)

        ctx.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
        val wx = wX(); val ww = wW()
        for ((i, e) in elementEntries.withIndex()) {
            val ry = lstTop + i * ROW_H_ELEM - scroll
            if (ry + ROW_H_ELEM <= lstTop || ry >= lstBot) continue
            val hov = mx in dx until dx + DLG_W && my in ry until ry + ROW_H_ELEM
            if (hov) ctx.fill(dx, ry, dx + DLG_W, ry + ROW_H_ELEM, C_ROW_HOVER)
            ctx.drawText(this.ctx.tr, e.name, dx + PAD,
                ry + (ROW_H_ELEM - this.ctx.tr.fontHeight) / 2, C_TEXT, false)
            if (e is ProcessedEntry.TextEntry) {
                val tf = this.ctx.scFields.getOrPut(e) {
                    com.github.mikecraft1224.simplecore.ui.ScTextField(wx, ry + 3, ww, ROW_H_ELEM - 6, e.get())
                        .also { it.onChange = { v -> e.set(v) } }
                }.apply { x = wx; y = ry + 3 }
                tf.render(ctx, mx, my)
            } else {
                this.ctx.drawWidget(ctx, e, wx, ry + 3, ww, ROW_H_ELEM - 6, mx, my)
            }
        }
        ctx.disableScissor()

        if (maxSc > 0) {
            val thumbH = (visH * visH.toFloat() / (elementEntries.size * ROW_H_ELEM)).toInt().coerceAtLeast(16)
            val thumbY = lstTop + ((scroll.toFloat() / maxSc) * (visH - thumbH)).toInt()
            ctx.fill(dx + DLG_W - 3, lstTop, dx + DLG_W - 1, lstBot, C_SCROLLBAR_TRACK)
            ctx.fill(dx + DLG_W - 3, thumbY, dx + DLG_W - 1, thumbY + thumbH, C_SCROLLBAR_THUMB)
        }

        ctx.fill(dx + 1, lstBot, dx + DLG_W - 1, lstBot + 1, C_SURFACE0)
        val bw = 74; val bh = 16
        val bx = dx + DLG_W / 2 - bw / 2
        val doneY = lstBot + 8
        val doneHov = mx in bx until bx + bw && my in doneY until doneY + bh
        ctx.fill(bx, doneY, bx + bw, doneY + bh, if (doneHov) C_SURFACE1 else C_SURFACE0)
        ctx.drawCenteredTextWithShadow(this.ctx.tr, "Done", bx + bw / 2, doneY + (bh - this.ctx.tr.fontHeight) / 2, C_TEXT)

        elementDropdown?.render(ctx, mx, my)
    }

    override fun mouseClicked(mx: Int, my: Int): Boolean {
        elementDropdown?.let { dd ->
            if (dd.mouseClicked(mx, my)) return true
        }
        val dx = dlgX; val dy = dlgY
        if (mx !in dx until dx + DLG_W || my !in dy until dy + DLG_H) {
            closeCallback(); return false
        }
        val bw = 74; val bh = 16
        val bx = dx + DLG_W / 2 - bw / 2
        if (mx in bx until bx + bw && my in lstBot + 8 until lstBot + 8 + bh) {
            closeCallback(); return true
        }
        if (my !in lstTop until lstBot) return true
        val wx = wX(); val ww = wW()
        for ((i, e) in elementEntries.withIndex()) {
            val ry = lstTop + i * ROW_H_ELEM - scroll
            if (my !in ry until ry + ROW_H_ELEM) continue
            if (mx !in wx until wx + ww) return true
            ctx.scFields.values.forEach { it.focused = false }
            when (e) {
                is ProcessedEntry.TextEntry -> {
                    ctx.scFields[e]?.mouseClicked(mx, my)
                    return ctx.scFields[e]?.focused ?: false
                }
                is ProcessedEntry.BoolEntry     -> { e.set(!e.get()); return true }
                is ProcessedEntry.DropdownEntry -> {
                    if (elementDropdown?.entry === e) elementDropdown = null
                    else elementDropdown = DropdownOverlay(
                        entry = e,
                        anchorX = wx,
                        anchorBottom = ry + ROW_H_ELEM - 3,
                        anchorW = ww,
                        screenHeight = ctx.getH,
                        accentColor = ctx.accentColor,
                        tr = ctx.tr,
                        closeCallback = { elementDropdown = null },
                    )
                    return true
                }
                is ProcessedEntry.SliderEntry -> {
                    val inputX = wx + ww - SLIDER_INP
                    if (mx < inputX) ctx.startSliderDrag(e, mx, wx, ww - SLIDER_INP - 4)
                    else ctx.scFields[e]?.mouseClicked(mx, my)
                    return true
                }
                is ProcessedEntry.ButtonEntry  -> { e.action(); return true }
                is ProcessedEntry.KeybindEntry -> { ctx.captureKeybind(e); return true }
                else -> return false
            }
        }
        return true
    }

    override fun mouseScrolled(vAmt: Double): Boolean {
        if (elementDropdown != null) { elementDropdown!!.mouseScrolled(vAmt); return true }
        scroll = (scroll - (vAmt * ROW_H_ELEM).toInt()).coerceIn(0, maxSc)
        return true
    }

    override fun keyPressed(keyCode: Int, mods: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { closeCallback(); return true }
        ctx.scFields.values.firstOrNull { it.focused }?.keyPressed(keyCode, mods)
        return true
    }

    override fun charTyped(chr: Char): Boolean {
        ctx.scFields.values.firstOrNull { it.focused }?.charTyped(chr)
        return true
    }
}
