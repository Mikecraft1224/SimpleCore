@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.render.hud

import com.github.mikecraft1224.simplecore.utils.Color
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor

/** Width and height of a rendered HUD element, as returned by functions that draw variable-size content. */
data class RenderSize(val width: Int, val height: Int)

/** Fills a rectangle using width/height dimensions instead of [GuiGraphicsExtractor.fill]'s x1/y1/x2/y2 form. */
fun GuiGraphicsExtractor.fillRect(x: Int, y: Int, width: Int, height: Int, color: Color) {
    fill(x, y, x + width, y + height, color.argb)
}

/**
 * Fills a rectangle with a solid background and a border of [borderWidth] pixels on each side.
 * The border is drawn on top of the fill, so the visible interior is `width - 2*borderWidth` wide.
 */
fun GuiGraphicsExtractor.drawBorderedRect(
    x: Int, y: Int,
    width: Int, height: Int,
    fillColor: Color,
    borderColor: Color,
    borderWidth: Int = 1,
) {
    // Background
    fill(x, y, x + width, y + height, fillColor.argb)
    val bc = borderColor.argb
    // Top edge
    fill(x, y, x + width, y + borderWidth, bc)
    // Bottom edge
    fill(x, y + height - borderWidth, x + width, y + height, bc)
    // Left edge
    fill(x, y + borderWidth, x + borderWidth, y + height - borderWidth, bc)
    // Right edge
    fill(x + width - borderWidth, y + borderWidth, x + width, y + height - borderWidth, bc)
}

/** Draws [text] with a drop shadow, using [Color] instead of a raw packed int. */
fun GuiGraphicsExtractor.drawText(font: Font, text: String, x: Int, y: Int, color: Color, shadow: Boolean = true) {
    text(font, text, x, y, color.argb, shadow)
}

/** Draws [text] horizontally centered around [centerX]. */
fun GuiGraphicsExtractor.drawCenteredText(font: Font, text: String, centerX: Int, y: Int, color: Color) {
    centeredText(font, text, centerX, y, color.argb)
}

/** Fills a rectangle with a top-to-bottom color gradient. */
fun GuiGraphicsExtractor.fillGradientRect(x: Int, y: Int, width: Int, height: Int, colorTop: Color, colorBottom: Color) {
    fillGradient(x, y, x + width, y + height, colorTop.argb, colorBottom.argb)
}

/**
 * Runs [block] with drawing clipped to the given pixel region, then restores the previous
 * scissor state - use for scrollable HUD panels or any content that must not draw outside a
 * fixed-size box. Always pairs [GuiGraphicsExtractor.enableScissor]/[GuiGraphicsExtractor.disableScissor],
 * even if [block] throws.
 */
inline fun GuiGraphicsExtractor.withScissor(x: Int, y: Int, width: Int, height: Int, block: () -> Unit) {
    enableScissor(x, y, x + width, y + height)
    try {
        block()
    } finally {
        disableScissor()
    }
}
