package com.github.mikecraft1224.simplecore.config.screen.overlays

import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_DIM
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_DRAG_GHOST
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_DRAG_INSERT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_MANTLE
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_OVERLAY0
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_RED
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_ROW_HOVER
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SCROLLBAR_THUMB
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SCROLLBAR_TRACK
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SUBTEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE0
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE1
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_TEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigOverlay
import com.github.mikecraft1224.simplecore.config.screen.ConfigScreenCtx
import com.github.mikecraft1224.simplecore.config.screen.OverlayLayer
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW
import kotlin.math.max

class ObjectListOverlay(
    private val entry: ProcessedEntry.ObjectListEntry,
    private val ctx: ConfigScreenCtx,
    private val layer: OverlayLayer,
) : ConfigOverlay {

    val DLG_W    = 300
    private val EDIT_DLG_W = 300
    private val SIDE_GAP   = 10
    val sideBySide get() = elementEditor != null
    private val DLG_H    = 340
    private val ITEM_H   = 28
    private val PAD      = 8
    private val DEL_W    = 20
    private val EDIT_W   = 40
    private val HANDLE_W = 14
    private var scroll   = 0
    private var dragIdx     = -1
    private var dragY       = 0
    private var dragOffsetY = 0

    private val list     get() = entry.getList()
    val dlgX get() = if (sideBySide) (ctx.getW() - DLG_W - EDIT_DLG_W - SIDE_GAP) / 2
                     else            (ctx.getW() - DLG_W) / 2
    val dlgY get() = (ctx.getH() - DLG_H) / 2
    private val lstTop   get() = dlgY + 45
    private val lstBot   get() = dlgY + DLG_H - 32
    private val visH     get() = lstBot - lstTop
    private val maxSc    get() = max(0, list.size * ITEM_H - visH)

    private var elementEditor: ElementEditOverlay? = null

    private fun liveTarget(): Int {
        val center = dragY - dragOffsetY + ITEM_H / 2
        return ((center - lstTop + scroll) / ITEM_H).coerceIn(0, list.size - 1)
    }

    override fun hitTests(mx: Int, my: Int): Boolean =
        mx in dlgX until dlgX + DLG_W && my in dlgY until dlgY + DLG_H

    override fun render(state: GuiGraphicsExtractor, mx: Int, my: Int) {
        val dx   = dlgX; val dy = dlgY
        val delX = dx + DLG_W - PAD - DEL_W
        val editX = delX - EDIT_W - 4
        val accent = ctx.accentColor()

        state.fill(0, 0, ctx.getW(), ctx.getH(), C_DIM)
        state.fill(dx, dy, dx + DLG_W, dy + DLG_H, C_MANTLE)
        state.fill(dx,             dy,             dx + DLG_W, dy + 1,      C_SURFACE1)
        state.fill(dx,             dy + DLG_H - 1, dx + DLG_W, dy + DLG_H, C_SURFACE1)
        state.fill(dx,             dy,             dx + 1,     dy + DLG_H,  C_SURFACE1)
        state.fill(dx + DLG_W - 1, dy,             dx + DLG_W, dy + DLG_H, C_SURFACE1)
        state.centeredText(ctx.tr, "Edit: ${entry.name}", dx + DLG_W / 2, dy + 8, C_TEXT)
        state.fill(dx + 1, dy + 22, dx + DLG_W - 1, dy + 23, C_SURFACE0)

        val addHov = mx in dx + PAD until dx + PAD + 54 && my in dy + 26 until dy + 42
        state.fill(dx + PAD, dy + 26, dx + PAD + 54, dy + 42, if (addHov) C_SURFACE1 else C_SURFACE0)
        state.centeredText(ctx.tr, "+ Add", dx + PAD + 27, dy + 30, C_TEXT)
        state.fill(dx + 1, lstTop - 1, dx + DLG_W - 1, lstTop, C_SURFACE0)

        fun drawRow(origIdx: Int, value: Any, ry: Int, lifted: Boolean = false) {
            val handleHov = lifted || (mx in dx + 2 until dx + PAD + HANDLE_W && my in ry until ry + ITEM_H)
            val hColor = if (handleHov) C_SUBTEXT else C_SURFACE1
            val hx = dx + 3
            for (line in 0..2) {
                val hy = ry + (ITEM_H / 2) - 4 + line * 4
                state.fill(hx, hy, hx + HANDLE_W - 4, hy + 2, hColor)
            }
            val idxLabel = "${origIdx + 1}."
            val idxW = ctx.tr.width("${list.size + 1}.")
            state.text(ctx.tr, idxLabel, dx + PAD + HANDLE_W,
                ry + (ITEM_H - ctx.tr.lineHeight) / 2, C_OVERLAY0, false)
            val labelX = dx + PAD + HANDLE_W + idxW + 4
            val labelMaxW = editX - labelX - 4
            val label = entry.elementLabel(value)
            val truncated = if (ctx.tr.width(label) <= labelMaxW) label
                            else ctx.tr.plainSubstrByWidth(label, labelMaxW - ctx.tr.width("…")) + "…"
            state.text(ctx.tr, truncated, labelX,
                ry + (ITEM_H - ctx.tr.lineHeight) / 2, C_TEXT, false)
            if (!lifted) {
                val isEditing = elementEditor?.elementIndex == origIdx
                val editHov = mx in editX until editX + EDIT_W && my in ry + 3 until ry + ITEM_H - 3
                state.fill(editX, ry + 3, editX + EDIT_W, ry + ITEM_H - 3,
                    if (isEditing) accent else if (editHov) C_SURFACE1 else C_SURFACE0)
                val ew = ctx.tr.width("Edit")
                state.text(ctx.tr, "Edit", editX + (EDIT_W - ew) / 2,
                    ry + (ITEM_H - ctx.tr.lineHeight) / 2, C_TEXT, false)
            }
            val delHov = !lifted && mx in delX until delX + DEL_W && my in ry + 3 until ry + ITEM_H - 3
            state.fill(delX, ry + 3, delX + DEL_W, ry + ITEM_H - 3, if (delHov) C_RED else C_SURFACE0)
            val xw = ctx.tr.width("×")
            state.text(ctx.tr, "×", delX + (DEL_W - xw) / 2,
                ry + (ITEM_H - ctx.tr.lineHeight) / 2, C_TEXT, false)
        }

        if (dragIdx >= 0) {
            val target = liveTarget()
            val renderOrder = buildList<Int> {
                var slot = 0
                for (origIdx in list.indices) {
                    if (origIdx == dragIdx) continue
                    if (slot == target) add(-1)
                    add(origIdx); slot++
                }
                if (-1 !in this) add(-1)
            }
            state.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
            for ((slot, origIdx) in renderOrder.withIndex()) {
                val ry = lstTop + slot * ITEM_H - scroll
                if (ry + ITEM_H <= lstTop || ry >= lstBot) continue
                if (origIdx == -1) {
                    state.fill(dx + PAD, ry + 4, dx + DLG_W - PAD, ry + ITEM_H - 4, C_DRAG_INSERT)
                    state.fill(dx + PAD, ry + 4, dx + DLG_W - PAD, ry + 5, accent)
                    state.fill(dx + PAD, ry + ITEM_H - 5, dx + DLG_W - PAD, ry + ITEM_H - 4, accent)
                    continue
                }
                if (mx in dx until dx + DLG_W && my in ry until ry + ITEM_H)
                    state.fill(dx, ry, dx + DLG_W, ry + ITEM_H, C_ROW_HOVER)
                drawRow(origIdx, list[origIdx], ry)
            }
            state.disableScissor()
            val floatY = (dragY - dragOffsetY).coerceIn(lstTop - ITEM_H / 2, lstBot - ITEM_H / 2)
            state.fill(dx + 1, floatY, dx + DLG_W - 1, floatY + ITEM_H, C_DRAG_GHOST)
            state.fill(dx + 1, floatY, dx + DLG_W - 1, floatY + 1, accent)
            state.fill(dx + 1, floatY + ITEM_H - 1, dx + DLG_W - 1, floatY + ITEM_H, accent)
            drawRow(dragIdx, list[dragIdx], floatY, lifted = true)
        } else {
            state.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
            for ((i, value) in list.withIndex()) {
                val ry = lstTop + i * ITEM_H - scroll
                if (ry + ITEM_H <= lstTop || ry >= lstBot) continue
                if (mx in dx until dx + DLG_W && my in ry until ry + ITEM_H)
                    state.fill(dx, ry, dx + DLG_W, ry + ITEM_H, C_ROW_HOVER)
                drawRow(i, value, ry)
            }
            state.disableScissor()
        }

        if (maxSc > 0) {
            val thumbH = (visH * visH.toFloat() / (list.size * ITEM_H)).toInt().coerceAtLeast(16)
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
    }

    private fun openEditor(idx: Int) {
        val newEditor = ElementEditOverlay(
            entry         = entry,
            elementIndex  = idx,
            ctx           = ctx,
            dlgXProvider  = { dlgX + DLG_W + SIDE_GAP },
            dlgYProvider  = { dlgY },
            closeCallback = { closeEditor() },
        )
        layer.replacePeer(elementEditor, newEditor)
        elementEditor = newEditor
    }

    private fun closeEditor() {
        elementEditor?.let { layer.removePeer(it) }
        elementEditor = null
    }

    override fun mouseClicked(mx: Int, my: Int): Boolean {
        val dx = dlgX; val dy = dlgY
        if (mx !in dx until dx + DLG_W || my !in dy until dy + DLG_H) {
            return false
        }
        if (mx in dx + PAD until dx + PAD + 54 && my in dy + 26 until dy + 42) {
            list.add(entry.createElement())
            scroll = scroll.coerceIn(0, maxSc)
            return true
        }
        val bw = 74; val bh = 16
        val bx = dx + DLG_W / 2 - bw / 2
        if (mx in bx until bx + bw && my in lstBot + 8 until lstBot + 8 + bh) {
            closeEditor()
            return false
        }
        if (my !in lstTop until lstBot) return true
        val delX = dx + DLG_W - PAD - DEL_W
        val editX = delX - EDIT_W - 4
        for (i in list.indices) {
            val ry = lstTop + i * ITEM_H - scroll
            if (my !in ry until ry + ITEM_H) continue
            if (mx in delX until delX + DEL_W && my in ry + 3 until ry + ITEM_H - 3) {
                if (i < list.size) {
                    if (elementEditor?.elementIndex == i) closeEditor()
                    else if (elementEditor != null && (elementEditor?.elementIndex ?: 0) > i) {
                        // Shift the editor's index down because the item above it was deleted
                        elementEditor?.elementIndex = (elementEditor?.elementIndex ?: 0) - 1
                    }
                    list.removeAt(i)
                    scroll = scroll.coerceIn(0, maxSc)
                }
                return true
            }
            if (mx in editX until editX + EDIT_W && my in ry + 3 until ry + ITEM_H - 3) {
                if (elementEditor?.elementIndex == i) closeEditor()
                else openEditor(i)
                return true
            }
            if (mx in dx + 2 until dx + PAD + HANDLE_W && my in ry until ry + ITEM_H) {
                closeEditor()
                dragIdx = i; dragY = my; dragOffsetY = my - ry
                return true
            }
            return true
        }
        return true
    }

    override fun mouseDragged(mx: Int, my: Int): Boolean {
        if (dragIdx < 0) return false
        dragY = my; return true
    }

    override fun mouseReleased(): Boolean {
        if (dragIdx < 0) return false
        val target = liveTarget()
        if (target != dragIdx) {
            val item = list.removeAt(dragIdx)
            list.add(target, item)
        }
        dragIdx = -1
        return true
    }

    override fun mouseScrolled(vAmt: Double): Boolean {
        if (elementEditor != null) {
            elementEditor?.mouseScrolled(vAmt)
            return true
        }
        scroll = (scroll - (vAmt * ITEM_H).toInt()).coerceIn(0, maxSc)
        return true
    }

    override fun keyPressed(keyCode: Int, mods: Int): Boolean {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && elementEditor != null) {
            closeEditor(); return true
        }
        ctx.scFields.values.firstOrNull { it.focused }?.keyPressed(keyCode, mods)
        return false
    }

    override fun onClose() {
        closeEditor()
    }
}
