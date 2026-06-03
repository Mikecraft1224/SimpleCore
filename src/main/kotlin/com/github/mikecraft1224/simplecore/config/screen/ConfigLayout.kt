@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.config.screen

import com.github.mikecraft1224.simplecore.utils.Color
import net.minecraft.client.gui.DrawContext

object ConfigLayout {
    const val CAT_W      = 120
    const val SEARCH_H   = 26
    const val ROW_H      = 26
    const val BOT_H      = 28
    const val SLIDER_INP = 36

    // Catppuccin Mocha palette
    val C_MANTLE   = 0xFF181825.toInt()
    val C_BASE     = 0xFF1E1E2E.toInt()
    val C_SURFACE0 = 0xFF313244.toInt()
    val C_SURFACE1 = 0xFF45475A.toInt()
    val C_OVERLAY0 = 0xFF6C7086.toInt()
    val C_SUBTEXT  = 0xFFBAC2DE.toInt()
    val C_TEXT     = 0xFFCDD6F4.toInt()
    val C_GREEN    = 0xFF40A02B.toInt()
    val C_BLUE     = 0xFF89B4FA.toInt()
    val C_RED      = 0xFFF38BA8.toInt()

    // UI utility colors
    val C_DIM             = Color(0,    0,    0,    0x99).argb
    val C_ROW_HOVER       = Color(255,  255,  255,  0x0A).argb
    val C_DRAG_INSERT     = Color(255,  255,  255,  0x18).argb
    val C_DRAG_GHOST      = Color(0x31, 0x32, 0x44, 0xDD).argb
    val C_SCROLLBAR_TRACK = Color(255,  255,  255,  0x22).argb
    val C_SCROLLBAR_THUMB = Color(0xBA, 0xC2, 0xDE, 0xAA).argb

    fun widgetX(screenWidth: Int) = CAT_W + ((screenWidth - CAT_W) * 0.65f).toInt()
    fun widgetW(screenWidth: Int) = ((screenWidth - CAT_W) * 0.28f).toInt()

    fun lighten(color: Int): Int {
        val r = ((color shr 16) and 0xFF).let { it + (255 - it) / 5 }.coerceAtMost(255)
        val g = ((color shr  8) and 0xFF).let { it + (255 - it) / 5 }.coerceAtMost(255)
        val b = ( color         and 0xFF).let { it + (255 - it) / 5 }.coerceAtMost(255)
        return (color and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }

    fun DrawContext.drawDialogBorder(dx: Int, dy: Int, dw: Int, dh: Int) {
        fill(dx, dy, dx + dw, dy + dh, C_MANTLE)
        fill(dx,          dy,          dx + dw, dy + 1,      C_SURFACE1)
        fill(dx,          dy + dh - 1, dx + dw, dy + dh,     C_SURFACE1)
        fill(dx,          dy,          dx + 1,  dy + dh,     C_SURFACE1)
        fill(dx + dw - 1, dy,          dx + dw, dy + dh,     C_SURFACE1)
    }
}
