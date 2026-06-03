package com.github.mikecraft1224.simplecore.config.screen.overlays

import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_DIM
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_DRAG_GHOST
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_DRAG_INSERT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_GREEN
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
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.lighten
import com.github.mikecraft1224.simplecore.config.screen.ConfigOverlay
import com.github.mikecraft1224.simplecore.config.screen.ConfigScreenCtx
import com.github.mikecraft1224.simplecore.ui.ScTextField
import net.minecraft.client.gui.DrawContext
import kotlin.math.max

class ListOverlay(
    private val entry: ProcessedEntry.MutableListEntry,
    private val ctx: ConfigScreenCtx,
    private val onDoneClicked: (() -> Unit)? = null,
) : ConfigOverlay {

    private var activeItemDdOverlay: DropdownOverlay? = null
    private var activeItemDdIdx: Int = -1

    private val DLG_W    = 280
    private val DLG_H    = 320
    private val ITEM_H   = 26
    private val PAD      = 8
    private val DEL_W    = 20
    private val HANDLE_W = 14
    private var scroll   = 0
    private var dragIdx     = -1
    private var dragY       = 0
    private var dragOffsetY = 0

    private val list     get() = entry.getList()
    private val dlgX     get() = (ctx.getW() - DLG_W) / 2
    private val dlgY     get() = (ctx.getH() - DLG_H) / 2
    private val lstTop   get() = dlgY + 45
    private val lstBot   get() = dlgY + DLG_H - 32
    private val visH     get() = lstBot - lstTop
    private val maxSc    get() = max(0, list.size * ITEM_H - visH)

    /** ScTextField instances keyed by list index. */
    val itemTfs = HashMap<Int, ScTextField>()

    private fun fieldX() = dlgX + PAD + HANDLE_W
    private fun fieldW() = DLG_W - PAD * 2 - DEL_W - 4 - 24 - HANDLE_W

    override fun hitTests(mx: Int, my: Int): Boolean =
        mx in dlgX until dlgX + DLG_W && my in dlgY until dlgY + DLG_H

    private fun liveTarget(): Int {
        val center = dragY - dragOffsetY + ITEM_H / 2
        return ((center - lstTop + scroll) / ITEM_H).coerceIn(0, list.size - 1)
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int) {
        val dx = dlgX; val dy = dlgY
        val fx = fieldX(); val fw = fieldW()
        val delX = dx + DLG_W - PAD - DEL_W
        val isBool     = entry.elementType == ProcessedEntry.MutableListEntry.ElementType.BOOLEAN
        val isDropdown = entry.elementType == ProcessedEntry.MutableListEntry.ElementType.DROPDOWN
        val accent     = this.ctx.accentColor()

        ctx.fill(0, 0, this.ctx.getW(), this.ctx.getH(), C_DIM)
        ctx.fill(dx, dy, dx + DLG_W, dy + DLG_H, C_MANTLE)
        ctx.fill(dx,             dy,             dx + DLG_W, dy + 1,           C_SURFACE1)
        ctx.fill(dx,             dy + DLG_H - 1, dx + DLG_W, dy + DLG_H,      C_SURFACE1)
        ctx.fill(dx,             dy,             dx + 1,     dy + DLG_H,       C_SURFACE1)
        ctx.fill(dx + DLG_W - 1, dy,             dx + DLG_W, dy + DLG_H,      C_SURFACE1)

        ctx.drawCenteredTextWithShadow(this.ctx.tr, "Edit: ${entry.name}", dx + DLG_W / 2, dy + 8, C_TEXT)
        ctx.fill(dx + 1, dy + 22, dx + DLG_W - 1, dy + 23, C_SURFACE0)

        val addHov = mx in dx + PAD until dx + PAD + 54 && my in dy + 26 until dy + 42
        ctx.fill(dx + PAD, dy + 26, dx + PAD + 54, dy + 42, if (addHov) C_SURFACE1 else C_SURFACE0)
        ctx.drawCenteredTextWithShadow(this.ctx.tr, "+ Add", dx + PAD + 27, dy + 30, C_TEXT)
        ctx.fill(dx + 1, lstTop - 1, dx + DLG_W - 1, lstTop, C_SURFACE0)

        fun drawRow(origIdx: Int, value: Any, ry: Int, lifted: Boolean = false) {
            val handleHov = lifted || (mx in dx + 2 until dx + PAD + HANDLE_W && my in ry until ry + ITEM_H)
            val hColor = if (handleHov) C_SUBTEXT else C_SURFACE1
            val hx = dx + 3
            for (line in 0..2) {
                val hy = ry + (ITEM_H / 2) - 4 + line * 4
                ctx.fill(hx, hy, hx + HANDLE_W - 4, hy + 2, hColor)
            }
            val idxLabel = "${origIdx + 1}."
            ctx.drawText(this.ctx.tr, idxLabel, delX - this.ctx.tr.getWidth(idxLabel) - 4,
                ry + (ITEM_H - this.ctx.tr.fontHeight) / 2, C_OVERLAY0, false)
            if (isBool) {
                val on     = value as? Boolean ?: false
                val togHov = !lifted && mx in fx until fx + fw && my in ry + 3 until ry + ITEM_H - 3
                val bg     = if (on) if (togHov) lighten(C_GREEN) else C_GREEN
                             else    if (togHov) C_SURFACE1       else C_SURFACE0
                ctx.fill(fx, ry + 3, fx + fw, ry + ITEM_H - 3, bg)
                val label = if (on) "ON" else "OFF"
                val lw    = this.ctx.tr.getWidth(label)
                ctx.drawText(this.ctx.tr, label, fx + (fw - lw) / 2,
                    ry + (ITEM_H - this.ctx.tr.fontHeight) / 2, C_TEXT, false)
            } else if (isDropdown) {
                val idx   = value as? Int ?: 0
                val opts  = entry.dropdownOptions?.invoke() ?: emptyList()
                val label = opts.getOrElse(idx) { "?" }
                val isOpen = !lifted && activeItemDdIdx == origIdx
                val ddHov  = !lifted && mx in fx until fx + fw && my in ry + 3 until ry + ITEM_H - 3
                ctx.fill(fx, ry + 3, fx + fw, ry + ITEM_H - 3,
                    if (isOpen || ddHov) C_SURFACE1 else C_SURFACE0)
                val lw = this.ctx.tr.getWidth(label)
                ctx.drawText(this.ctx.tr, label, fx + (fw - lw) / 2,
                    ry + (ITEM_H - this.ctx.tr.fontHeight) / 2, C_TEXT, false)
                if (!lifted) {
                    val arrow = if (isOpen) "▲" else "▼"
                    ctx.drawText(this.ctx.tr, arrow, fx + fw - this.ctx.tr.getWidth(arrow) - 4,
                        ry + (ITEM_H - this.ctx.tr.fontHeight) / 2, C_OVERLAY0, false)
                }
            } else if (lifted || dragIdx >= 0) {
                ctx.fill(fx, ry + 3, fx + fw, ry + ITEM_H - 3, C_SURFACE0)
                ctx.drawText(this.ctx.tr, value.toString(), fx + 4,
                    ry + (ITEM_H - this.ctx.tr.fontHeight) / 2, C_TEXT, false)
            } else {
                val tf = itemTfs.getOrPut(origIdx) {
                    val idx = origIdx
                    ScTextField(fx, ry + 3, fw, ITEM_H - 6, value.toString()).also { f ->
                        f.onChange = { v ->
                            val parsed: Any = when (entry.elementType) {
                                ProcessedEntry.MutableListEntry.ElementType.INT ->
                                    v.toIntOrNull() ?: list.getOrNull(idx) ?: entry.defaultElement
                                else -> v
                            }
                            if (idx < list.size) list[idx] = parsed
                        }
                    }
                }.apply { x = fx; y = ry + 3 }
                if (!tf.focused) tf.setText(value.toString())
                tf.render(ctx, mx, my)
            }
            val canDelete = !(entry.requireNonEmpty && list.size <= 1)
            val delHov = !lifted && canDelete && mx in delX until delX + DEL_W && my in ry + 3 until ry + ITEM_H - 3
            ctx.fill(delX, ry + 3, delX + DEL_W, ry + ITEM_H - 3, if (delHov) C_RED else if (canDelete) C_SURFACE0 else C_MANTLE)
            val xw = this.ctx.tr.getWidth("×")
            ctx.drawText(this.ctx.tr, "×", delX + (DEL_W - xw) / 2,
                ry + (ITEM_H - this.ctx.tr.fontHeight) / 2, C_TEXT, false)
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
            ctx.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
            for ((slot, origIdx) in renderOrder.withIndex()) {
                val ry = lstTop + slot * ITEM_H - scroll
                if (ry + ITEM_H <= lstTop || ry >= lstBot) continue
                if (origIdx == -1) {
                    ctx.fill(dx + PAD, ry + 4, dx + DLG_W - PAD, ry + ITEM_H - 4, C_DRAG_INSERT)
                    ctx.fill(dx + PAD, ry + 4, dx + DLG_W - PAD, ry + 5, accent)
                    ctx.fill(dx + PAD, ry + ITEM_H - 5, dx + DLG_W - PAD, ry + ITEM_H - 4, accent)
                    continue
                }
                if (mx in dx until dx + DLG_W && my in ry until ry + ITEM_H)
                    ctx.fill(dx, ry, dx + DLG_W, ry + ITEM_H, C_ROW_HOVER)
                drawRow(origIdx, list[origIdx], ry)
            }
            ctx.disableScissor()
            val floatY = (dragY - dragOffsetY).coerceIn(lstTop - ITEM_H / 2, lstBot - ITEM_H / 2)
            ctx.fill(dx + 1, floatY, dx + DLG_W - 1, floatY + ITEM_H, C_DRAG_GHOST)
            ctx.fill(dx + 1, floatY, dx + DLG_W - 1, floatY + 1, accent)
            ctx.fill(dx + 1, floatY + ITEM_H - 1, dx + DLG_W - 1, floatY + ITEM_H, accent)
            drawRow(dragIdx, list[dragIdx], floatY, lifted = true)
        } else {
            ctx.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
            for ((i, value) in list.withIndex()) {
                val ry = lstTop + i * ITEM_H - scroll
                if (ry + ITEM_H <= lstTop || ry >= lstBot) continue
                if (mx in dx until dx + DLG_W && my in ry until ry + ITEM_H)
                    ctx.fill(dx, ry, dx + DLG_W, ry + ITEM_H, C_ROW_HOVER)
                drawRow(i, value, ry)
            }
            ctx.disableScissor()
        }

        if (maxSc > 0) {
            val thumbH = (visH * visH.toFloat() / (list.size * ITEM_H)).toInt().coerceAtLeast(16)
            val thumbY = lstTop + ((scroll.toFloat() / maxSc) * (visH - thumbH)).toInt()
            ctx.fill(dx + DLG_W - 3, lstTop, dx + DLG_W - 1, lstBot, C_SCROLLBAR_TRACK)
            ctx.fill(dx + DLG_W - 3, thumbY, dx + DLG_W - 1, thumbY + thumbH, C_SCROLLBAR_THUMB)
        }

        ctx.fill(dx + 1, lstBot, dx + DLG_W - 1, lstBot + 1, C_SURFACE0)
        val bw    = 74; val bh = 16
        val bx    = dx + DLG_W / 2 - bw / 2
        val doneY = lstBot + 8
        val doneHov = mx in bx until bx + bw && my in doneY until doneY + bh
        ctx.fill(bx, doneY, bx + bw, doneY + bh, if (doneHov) C_SURFACE1 else C_SURFACE0)
        ctx.drawCenteredTextWithShadow(this.ctx.tr, "Done", bx + bw / 2, doneY + (bh - this.ctx.tr.fontHeight) / 2, C_TEXT)

        activeItemDdOverlay?.render(ctx, mx, my)
    }

    override fun mouseClicked(mx: Int, my: Int): Boolean {
        activeItemDdOverlay?.let { dd ->
            if (dd.mouseClicked(mx, my)) return true
        }
        val dx = dlgX; val dy = dlgY
        if (mx !in dx until dx + DLG_W || my !in dy until dy + DLG_H) {
            return false
        }
        if (mx in dx + PAD until dx + PAD + 54 && my in dy + 26 until dy + 42) {
            activeItemDdOverlay = null; activeItemDdIdx = -1
            list.add(entry.defaultElement)
            itemTfs.clear()
            scroll = scroll.coerceIn(0, maxSc)
            return true
        }
        val bw = 74; val bh = 16
        val bx = dx + DLG_W / 2 - bw / 2
        if (mx in bx until bx + bw && my in lstBot + 8 until lstBot + 8 + bh) {
            onDoneClicked?.invoke()
            return true
        }
        if (my !in lstTop until lstBot) return true
        val fx = fieldX(); val fw = fieldW()
        for (i in list.indices) {
            val ry   = lstTop + i * ITEM_H - scroll
            if (my !in ry until ry + ITEM_H) continue
            val delX = dx + DLG_W - PAD - DEL_W
            if (mx in delX until delX + DEL_W && my in ry + 3 until ry + ITEM_H - 3) {
                if (i < list.size && !(entry.requireNonEmpty && list.size <= 1)) {
                    list.removeAt(i)
                    itemTfs.clear()
                    activeItemDdOverlay = null; activeItemDdIdx = -1
                    scroll = scroll.coerceIn(0, maxSc)
                }
                return true
            }
            if (mx in dx + 2 until dx + PAD + HANDLE_W && my in ry until ry + ITEM_H) {
                dragIdx = i; dragY = my; dragOffsetY = my - ry; return true
            }
            if (entry.elementType == ProcessedEntry.MutableListEntry.ElementType.BOOLEAN &&
                mx in fx until fx + fw && my in ry + 3 until ry + ITEM_H - 3
            ) {
                if (i < list.size) list[i] = !(list[i] as? Boolean ?: false)
                return true
            }
            if (entry.elementType == ProcessedEntry.MutableListEntry.ElementType.DROPDOWN &&
                mx in fx until fx + fw && my in ry + 3 until ry + ITEM_H - 3
            ) {
                if (activeItemDdIdx == i) {
                    activeItemDdOverlay = null; activeItemDdIdx = -1
                } else {
                    openItemDropdown(i, ry + ITEM_H - 3)
                }
                return true
            }
            if (entry.elementType != ProcessedEntry.MutableListEntry.ElementType.BOOLEAN &&
                entry.elementType != ProcessedEntry.MutableListEntry.ElementType.DROPDOWN &&
                mx in fx until fx + fw && my in ry + 3 until ry + ITEM_H - 3
            ) {
                itemTfs.values.forEach { it.focused = false }
                ctx.scFields.values.forEach { it.focused = false }
                val tf = itemTfs.getOrPut(i) {
                    val idx = i
                    ScTextField(fx, ry + 3, fw, ITEM_H - 6, list.getOrNull(i)?.toString() ?: "").also { f ->
                        f.onChange = { v ->
                            val parsed: Any = when (entry.elementType) {
                                ProcessedEntry.MutableListEntry.ElementType.INT ->
                                    v.toIntOrNull() ?: list.getOrNull(idx) ?: entry.defaultElement
                                else -> v
                            }
                            if (idx < list.size) list[idx] = parsed
                        }
                    }
                }.apply { x = fx; y = ry + 3 }
                tf.mouseClicked(mx, my)
                return true
            }
        }
        return true
    }

    override fun mouseDragged(mx: Int, my: Int): Boolean {
        if (dragIdx < 0) return false
        dragY = my
        return true
    }

    override fun mouseReleased(): Boolean {
        if (dragIdx < 0) return false
        val target = liveTarget()
        if (target != dragIdx) {
            val item = list.removeAt(dragIdx)
            list.add(target, item)
        }
        dragIdx = -1
        itemTfs.clear()
        activeItemDdOverlay = null; activeItemDdIdx = -1
        return true
    }

    override fun mouseScrolled(vAmt: Double): Boolean {
        if (activeItemDdOverlay != null) return activeItemDdOverlay!!.mouseScrolled(vAmt)
        scroll = (scroll - (vAmt * ITEM_H).toInt()).coerceIn(0, maxSc)
        return true
    }

    override fun keyPressed(keyCode: Int, mods: Int): Boolean {
        itemTfs.values.firstOrNull { it.focused }?.keyPressed(keyCode, mods)
        return true
    }

    override fun charTyped(chr: Char): Boolean {
        itemTfs.values.firstOrNull { it.focused }?.charTyped(chr)
        return true
    }

    private fun openItemDropdown(itemIdx: Int, anchorBottom: Int) {
        val opts = entry.dropdownOptions ?: return
        val tempEntry = ProcessedEntry.DropdownEntry(
            name = "", description = "",
            options = opts,
            get = { (list.getOrNull(itemIdx) as? Int ?: 0).coerceIn(0, (opts().size - 1).coerceAtLeast(0)) },
            set = { idx -> if (itemIdx < list.size) list[itemIdx] = idx },
        )
        activeItemDdOverlay = DropdownOverlay(
            entry         = tempEntry,
            anchorX       = fieldX(),
            anchorBottom  = anchorBottom,
            anchorW       = fieldW(),
            screenHeight  = ctx.getH,
            accentColor   = ctx.accentColor,
            tr            = ctx.tr,
            closeCallback = { activeItemDdOverlay = null; activeItemDdIdx = -1 },
        )
        activeItemDdIdx = itemIdx
    }
}
