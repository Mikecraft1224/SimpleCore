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
import com.github.mikecraft1224.simplecore.config.screen.ScTextField
import net.minecraft.client.gui.GuiGraphicsExtractor
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

    // Cached per elementIndex - buildElementEntries() constructs fresh ProcessedEntry wrapper
    // objects on every call, and ctx.scFields keys on ProcessedEntry identity. Recomputing this
    // list on every access (as a plain `get()`) would silently orphan the ScTextField backing
    // each TextEntry row every frame, resetting focus/cursor state and breaking typing.
    private var cachedElementIndex = -1
    private var cachedElementEntries: List<ProcessedEntry> = emptyList()

    private val elementEntries: List<ProcessedEntry> get() {
        val list = entry.getList()
        if (elementIndex !in list.indices) return emptyList()
        if (cachedElementIndex != elementIndex) {
            cachedElementEntries = entry.buildElementEntries(list[elementIndex])
            cachedElementIndex = elementIndex
        }
        return cachedElementEntries
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

    override fun render(state: GuiGraphicsExtractor, mx: Int, my: Int) {
        val dx = dlgX; val dy = dlgY
        state.fill(dx, dy, dx + DLG_W, dy + DLG_H, C_MANTLE)
        state.fill(dx,             dy,             dx + DLG_W, dy + 1,      C_SURFACE1)
        state.fill(dx,             dy + DLG_H - 1, dx + DLG_W, dy + DLG_H, C_SURFACE1)
        state.fill(dx,             dy,             dx + 1,     dy + DLG_H,  C_SURFACE1)
        state.fill(dx + DLG_W - 1, dy,             dx + DLG_W, dy + DLG_H, C_SURFACE1)
        val list = entry.getList()
        val title = if (elementIndex < list.size)
            "Item ${elementIndex + 1}: ${entry.elementLabel(list[elementIndex])}"
        else
            "Item ${elementIndex + 1}"
        state.centeredText(ctx.tr, title, dx + DLG_W / 2, dy + 7, C_TEXT)
        state.fill(dx + 1, dy + 19, dx + DLG_W - 1, dy + 20, C_SURFACE0)

        state.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
        val wx = wX(); val ww = wW()
        for ((i, e) in elementEntries.withIndex()) {
            val ry = lstTop + i * ROW_H_ELEM - scroll
            if (ry + ROW_H_ELEM <= lstTop || ry >= lstBot) continue
            val hov = mx in dx until dx + DLG_W && my in ry until ry + ROW_H_ELEM
            if (hov) state.fill(dx, ry, dx + DLG_W, ry + ROW_H_ELEM, C_ROW_HOVER)
            state.text(ctx.tr, e.name, dx + PAD,
                ry + (ROW_H_ELEM - ctx.tr.lineHeight) / 2, C_TEXT, false)
            if (e is ProcessedEntry.TextEntry) {
                val tf = ctx.scFields.getOrPut(e) {
                    ScTextField(wx, ry + 3, ww, ROW_H_ELEM - 6, e.get())
                        .also { it.onChange = { v -> e.set(v) } }
                }.apply { x = wx; y = ry + 3 }
                tf.render(state, mx, my)
            } else {
                ctx.drawWidget(state, e, wx, ry + 3, ww, ROW_H_ELEM - 6, mx, my)
            }
        }
        state.disableScissor()

        if (maxSc > 0) {
            val thumbH = (visH * visH.toFloat() / (elementEntries.size * ROW_H_ELEM)).toInt().coerceAtLeast(16)
            val thumbY = lstTop + ((scroll.toFloat() / maxSc) * (visH - thumbH)).toInt()
            state.fill(dx + DLG_W - 3, lstTop, dx + DLG_W - 1, lstBot, C_SCROLLBAR_TRACK)
            state.fill(dx + DLG_W - 3, thumbY, dx + DLG_W - 1, thumbY + thumbH, C_SCROLLBAR_THUMB)
        }

        state.fill(dx + 1, lstBot, dx + DLG_W - 1, lstBot + 1, C_SURFACE0)
        val bw = 74; val bh = 16
        val bx = dx + DLG_W / 2 - bw / 2
        val doneY = lstBot + 8
        val doneHov = mx in bx until bx + bw && my in doneY until doneY + bh
        state.fill(bx, doneY, bx + bw, doneY + bh, if (doneHov) C_SURFACE1 else C_SURFACE0)
        state.centeredText(ctx.tr, "Done", bx + bw / 2, doneY + (bh - ctx.tr.lineHeight) / 2, C_TEXT)

        elementDropdown?.render(state, mx, my)
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
