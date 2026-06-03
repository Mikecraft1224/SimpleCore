package com.github.mikecraft1224.simplecore.config.screen.overlays

import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.BOT_H
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_MANTLE
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SCROLLBAR_THUMB
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SCROLLBAR_TRACK
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE0
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE1
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_TEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.ROW_H
import com.github.mikecraft1224.simplecore.config.screen.ConfigOverlay
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext

class DropdownOverlay(
    val entry: ProcessedEntry.DropdownEntry,
    private val anchorX: Int,
    private val anchorBottom: Int,
    private val anchorW: Int,
    private val screenHeight: () -> Int,
    private val accentColor: () -> Int,
    private val tr: TextRenderer,
    private val closeCallback: () -> Unit,
) : ConfigOverlay {

    private val ITEM_H  = 20
    private val MAX_VIS = 6
    private val popW    get() = anchorW
    private val popH    get() = minOf(entry.options().size, MAX_VIS) * ITEM_H + 2
    private val maxSc   get() = maxOf(0, entry.options().size * ITEM_H - (popH - 2))
    private var scroll  = run {
        val opts       = entry.options()
        val visContent = minOf(opts.size, MAX_VIS) * ITEM_H
        val idealTop   = entry.get() * ITEM_H - visContent / 2 + ITEM_H / 2
        idealTop.coerceIn(0, maxOf(0, opts.size * ITEM_H - visContent))
    }

    private fun popX() = anchorX
    private fun popY(): Int {
        val below = anchorBottom
        return if (below + popH <= screenHeight() - BOT_H) below
               else anchorBottom - (ROW_H - 6) - popH
    }

    // Full-screen consumer — click outside = close
    override fun hitTests(mx: Int, my: Int): Boolean = true

    override fun render(ctx: DrawContext, mx: Int, my: Int) {
        val px = popX(); val py = popY()
        val opts = entry.options()
        val accent = accentColor()
        ctx.fill(px,            py,            px + popW, py + popH,     C_MANTLE)
        ctx.fill(px,            py,            px + popW, py + 1,         C_SURFACE1)
        ctx.fill(px,            py + popH - 1, px + popW, py + popH,     C_SURFACE1)
        ctx.fill(px,            py,            px + 1,    py + popH,     C_SURFACE1)
        ctx.fill(px + popW - 1, py,            px + popW, py + popH,     C_SURFACE1)

        ctx.enableScissor(px + 1, py + 1, px + popW - 1, py + popH - 1)
        for (i in opts.indices) {
            val iy = py + 1 + i * ITEM_H - scroll
            if (iy + ITEM_H <= py + 1 || iy >= py + popH - 1) continue
            val isHov = mx in px + 1 until px + popW - 1 && my in iy until iy + ITEM_H
            val isSel = i == entry.get()
            when {
                isHov -> ctx.fill(px + 1, iy, px + popW - 1, iy + ITEM_H, C_SURFACE1)
                isSel -> ctx.fill(px + 1, iy, px + popW - 1, iy + ITEM_H, C_SURFACE0)
            }
            val col   = if (isSel) accent else C_TEXT
            val label = opts[i]
            val lw    = tr.getWidth(label)
            ctx.drawText(tr, label, px + (popW - lw) / 2, iy + (ITEM_H - tr.fontHeight) / 2, col, false)
        }
        ctx.disableScissor()

        if (maxSc > 0) {
            val visH   = popH - 2
            val thumbH = (visH * visH.toFloat() / (opts.size * ITEM_H)).toInt().coerceAtLeast(8)
            val thumbY = py + 1 + ((scroll.toFloat() / maxSc) * (visH - thumbH)).toInt()
            ctx.fill(px + popW - 3, py + 1,  px + popW - 1, py + popH - 1, C_SCROLLBAR_TRACK)
            ctx.fill(px + popW - 3, thumbY,  px + popW - 1, thumbY + thumbH, C_SCROLLBAR_THUMB)
        }
    }

    override fun mouseClicked(mx: Int, my: Int): Boolean {
        val px = popX(); val py = popY()
        if (mx in px until px + popW && my in py until py + popH) {
            val opts = entry.options()
            val i = (my - py - 1 + scroll) / ITEM_H
            if (i in opts.indices) { entry.set(i); closeCallback() }
            return true
        }
        closeCallback()
        return true
    }

    override fun mouseScrolled(vAmt: Double): Boolean {
        scroll = (scroll - (vAmt * ITEM_H).toInt()).coerceIn(0, maxSc)
        return true
    }
}
