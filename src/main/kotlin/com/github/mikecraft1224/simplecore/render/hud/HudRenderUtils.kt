@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.render.hud

import com.github.mikecraft1224.simplecore.bus.events.RenderHudEvent
import com.github.mikecraft1224.simplecore.utils.Color
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext

/** Width and height of a rendered HUD element, as returned by functions that draw variable-size content. */
data class RenderSize(val width: Int, val height: Int)

/** Fills a rectangle using width/height dimensions instead of `DrawContext.fill`'s x1/y1/x2/y2 form. */
fun DrawContext.fillRect(x: Int, y: Int, width: Int, height: Int, color: Color) {
    fill(x, y, x + width, y + height, color.argb)
}

/**
 * Fills a rectangle with a solid background and a border of [borderWidth] pixels on each side.
 * The border is drawn on top of the fill, so the visible interior is `width - 2*borderWidth` wide.
 */
fun DrawContext.drawBorderedRect(
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

/** Draws [text] at ([x], [y]) using the Minecraft text renderer. Prefer this over the raw `DrawContext.drawText` overload when working with [Color]. */
fun DrawContext.drawHudText(
    text: String,
    x: Int,
    y: Int,
    color: Color = Color.WHITE,
    shadow: Boolean = true,
) {
    val tr = MinecraftClient.getInstance().textRenderer
    drawText(tr, text, x, y, color.argb, shadow)
}

/**
 * Draws a list of strings stacked vertically with a filled background panel.
 *
 * The panel is sized to fit the longest line plus [padding] on each side.
 * Returns the [RenderSize] of the drawn panel so the caller can lay out adjacent elements.
 */
fun DrawContext.drawTextPanel(
    lines: List<String>,
    x: Int,
    y: Int,
    textColor: Color = Color.WHITE,
    bgColor: Color = Color(0, 0, 0, 160),
    padding: Int = 4,
    shadow: Boolean = true,
): RenderSize {
    val tr = MinecraftClient.getInstance().textRenderer
    val lineHeight = tr.fontHeight + 2
    val maxWidth = lines.maxOfOrNull { tr.getWidth(it) } ?: 0
    val panelWidth = maxWidth + padding * 2
    val panelHeight = lines.size * lineHeight - 2 + padding * 2

    fill(x, y, x + panelWidth, y + panelHeight, bgColor.argb)

    lines.forEachIndexed { i, line ->
        drawText(tr, line, x + padding, y + padding + i * lineHeight, textColor.argb, shadow)
    }

    return RenderSize(panelWidth, panelHeight)
}

/**
 * Draws a horizontal progress bar. [progress] is clamped to [0, 1]; the filled portion
 * scales linearly from left to right.
 */
fun DrawContext.drawProgressBar(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    progress: Float,
    foreground: Color,
    background: Color = Color(0, 0, 0, 160),
) {
    val clamped = progress.coerceIn(0f, 1f)
    fill(x, y, x + width, y + height, background.argb)
    val filledWidth = (width * clamped).toInt()
    if (filledWidth > 0) {
        fill(x, y, x + filledWidth, y + height, foreground.argb)
    }
}

/**
 * Enables a scissor rectangle specified by origin and dimensions.
 * Convenience overload for `DrawContext.enableScissor` which takes x1/y1/x2/y2.
 * Call [disableScissorRect] when done.
 */
fun DrawContext.enableScissorRect(x: Int, y: Int, width: Int, height: Int) {
    enableScissor(x, y, x + width, y + height)
}

/** Removes the active scissor region set by [enableScissorRect]. */
fun DrawContext.disableScissorRect() {
    disableScissor()
}

// -- RenderHudEvent convenience forwarders ----------------------------------------
// These delegate to the DrawContext extensions above so handler code can call them
// directly on the event without extracting `event.ctx`.

/** Forwards to [DrawContext.fillRect] on the event's draw context. */
fun RenderHudEvent.fillRect(x: Int, y: Int, width: Int, height: Int, color: Color) {
    ctx.fillRect(x, y, width, height, color)
}

/** Forwards to [DrawContext.drawHudText] on the event's draw context. */
fun RenderHudEvent.drawHudText(
    text: String,
    x: Int,
    y: Int,
    color: Color = Color.WHITE,
    shadow: Boolean = true,
) {
    ctx.drawHudText(text, x, y, color, shadow)
}

/** Forwards to [DrawContext.drawTextPanel] on the event's draw context. */
fun RenderHudEvent.drawTextPanel(
    lines: List<String>,
    x: Int,
    y: Int,
    textColor: Color = Color.WHITE,
    bgColor: Color = Color(0, 0, 0, 160),
    padding: Int = 4,
    shadow: Boolean = true,
): RenderSize {
    return ctx.drawTextPanel(lines, x, y, textColor, bgColor, padding, shadow)
}

/** Forwards to [DrawContext.drawProgressBar] on the event's draw context. */
fun RenderHudEvent.drawProgressBar(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    progress: Float,
    foreground: Color,
    background: Color = Color(0, 0, 0, 160),
) {
    ctx.drawProgressBar(x, y, width, height, progress, foreground, background)
}

/** Draws [text] horizontally centered on the screen at vertical position [y]. */
fun RenderHudEvent.drawCenteredText(
    text: String,
    y: Int,
    color: Color = Color.WHITE,
    shadow: Boolean = true,
) {
    val tr = MinecraftClient.getInstance().textRenderer
    val textWidth = tr.getWidth(text)
    val x = (screenWidth - textWidth) / 2
    ctx.drawHudText(text, x, y, color, shadow)
}
