package com.github.mikecraft1224.simplecore.ui

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW

/**
 * Minimal single-line text input that renders entirely inline via [DrawContext].
 *
 * Unlike [net.minecraft.client.gui.widget.TextFieldWidget] it is never registered as a
 * child widget — callers pass raw mouse/key/char events directly. This means the field
 * always looks like an editable input regardless of whether an overlay sits in front
 * of it, a dialog is being dragged, or the containing scroll list has moved.
 *
 * Visual design: dark fill, bottom underline (accent blue when focused, dim otherwise).
 * No border box, no mode switching.
 */
class ScTextField(
    var x: Int,
    var y: Int,
    var w: Int,
    var h: Int,
    initialText: String = "",
    val maxLength: Int = 512,
) {
    var text: String = initialText.take(maxLength)
        private set

    private var cursor   = text.length
    private var anchor   = cursor
    private var scrollPx = 0

    var focused  = false
    var onChange: ((String) -> Unit)? = null

    private val selMin get() = minOf(cursor, anchor)
    private val selMax get() = maxOf(cursor, anchor)

    fun setText(v: String) {
        val clamped = v.take(maxLength)
        if (clamped == text) return
        text   = clamped
        cursor = cursor.coerceAtMost(text.length)
        anchor = anchor.coerceAtMost(text.length)
        clampScroll()
    }

    // -- Rendering ------------------------------------------------------------

    fun render(ctx: DrawContext, @Suppress("UNUSED_PARAMETER") _mx: Int, @Suppress("UNUSED_PARAMETER") _my: Int) {
        val tr    = MinecraftClient.getInstance().textRenderer
        val pad   = 3
        val textY = y + (h - tr.fontHeight) / 2

        ctx.fill(x, y, x + w, y + h - 1, 0xFF313244.toInt())
        val lineCol = if (focused) 0xFF89B4FA.toInt() else 0xFF45475A.toInt()
        ctx.fill(x, y + h - 1, x + w, y + h, lineCol)

        val innerW = w - pad * 2
        ctx.enableScissor(x + pad, y, x + pad + innerW, y + h)

        if (cursor != anchor) {
            val s = (tr.getWidth(text.substring(0, selMin)) - scrollPx).coerceAtLeast(0)
            val e = (tr.getWidth(text.substring(0, selMax)) - scrollPx).coerceAtMost(innerW)
            if (e > s) ctx.fill(x + pad + s, y + 1, x + pad + e, y + h - 2, 0x4489B4FA)
        }

        val textCol = if (focused) 0xFFCDD6F4.toInt() else 0xFF9399B2.toInt()
        ctx.drawText(tr, text, x + pad - scrollPx, textY, textCol, false)

        if (focused && (System.currentTimeMillis() / 530) % 2 == 0L) {
            val cx = x + pad + tr.getWidth(text.substring(0, cursor)) - scrollPx
            ctx.fill(cx, y + 2, cx + 1, y + h - 2, 0xFFCDD6F4.toInt())
        }

        ctx.disableScissor()
    }

    // -- Mouse ----------------------------------------------------------------

    fun mouseClicked(mx: Int, my: Int): Boolean {
        if (mx !in x until x + w || my !in y until y + h) {
            focused = false; return false
        }
        focused = true
        cursor  = charIndexAt(mx - x - 3 + scrollPx)
        anchor  = cursor
        return true
    }

    // -- Keyboard -------------------------------------------------------------

    fun keyPressed(keyCode: Int, modifiers: Int): Boolean {
        if (!focused) return false
        val ctrl  = modifiers and GLFW.GLFW_MOD_CONTROL != 0
        val shift = modifiers and GLFW.GLFW_MOD_SHIFT   != 0
        when (keyCode) {
            GLFW.GLFW_KEY_LEFT      -> move((if (ctrl) prevWord() else cursor - 1).coerceAtLeast(0), shift)
            GLFW.GLFW_KEY_RIGHT     -> move((if (ctrl) nextWord() else cursor + 1).coerceAtMost(text.length), shift)
            GLFW.GLFW_KEY_HOME      -> move(0, shift)
            GLFW.GLFW_KEY_END       -> move(text.length, shift)
            GLFW.GLFW_KEY_BACKSPACE -> deleteBack(ctrl)
            GLFW.GLFW_KEY_DELETE    -> deleteForward(ctrl)
            GLFW.GLFW_KEY_ESCAPE    -> { focused = false }
            GLFW.GLFW_KEY_A -> if (ctrl) { anchor = 0; cursor = text.length }
            GLFW.GLFW_KEY_C -> if (ctrl && cursor != anchor) copyToClipboard()
            GLFW.GLFW_KEY_X -> if (ctrl && cursor != anchor) { copyToClipboard(); deleteSelection() }
            GLFW.GLFW_KEY_V -> if (ctrl) paste()
            else            -> return false
        }
        clampScroll()
        return true
    }

    fun charTyped(chr: Char): Boolean {
        if (!focused || chr.code < 32) return false
        if (cursor != anchor) deleteSelection()
        if (text.length >= maxLength) return true
        text   = text.substring(0, cursor) + chr + text.substring(cursor)
        cursor++; anchor = cursor
        onChange?.invoke(text)
        clampScroll()
        return true
    }

    // -- Internals ------------------------------------------------------------

    private fun move(pos: Int, shift: Boolean) {
        cursor = pos; if (!shift) anchor = cursor
    }

    private fun deleteBack(word: Boolean) {
        if (cursor != anchor) { deleteSelection(); return }
        if (cursor == 0) return
        val to = if (word) prevWord() else cursor - 1
        text = text.removeRange(to, cursor)
        cursor = to; anchor = cursor
        onChange?.invoke(text)
    }

    private fun deleteForward(word: Boolean) {
        if (cursor != anchor) { deleteSelection(); return }
        if (cursor == text.length) return
        val to = if (word) nextWord() else cursor + 1
        text = text.removeRange(cursor, to)
        onChange?.invoke(text)
    }

    private fun deleteSelection() {
        text   = text.removeRange(selMin, selMax)
        cursor = selMin; anchor = cursor
        onChange?.invoke(text)
    }

    private fun copyToClipboard() {
        MinecraftClient.getInstance().keyboard.clipboard = text.substring(selMin, selMax)
    }

    private fun paste() {
        if (cursor != anchor) deleteSelection()
        val clip = MinecraftClient.getInstance().keyboard.clipboard
            .filter { it.code >= 32 && it != '\n' && it != '\r' }
            .take(maxLength - text.length)
        if (clip.isEmpty()) return
        text   = text.substring(0, cursor) + clip + text.substring(cursor)
        cursor += clip.length; anchor = cursor
        onChange?.invoke(text)
    }

    private fun prevWord(): Int {
        var i = cursor
        while (i > 0 && text[i - 1] == ' ') i--
        while (i > 0 && text[i - 1] != ' ') i--
        return i
    }

    private fun nextWord(): Int {
        var i = cursor
        while (i < text.length && text[i] != ' ') i++
        while (i < text.length && text[i] == ' ') i++
        return i
    }

    private fun charIndexAt(px: Int): Int {
        val tr = MinecraftClient.getInstance().textRenderer
        for (i in 1..text.length) {
            if (tr.getWidth(text.substring(0, i)) > px) return i - 1
        }
        return text.length
    }

    private fun clampScroll() {
        val tr    = MinecraftClient.getInstance().textRenderer
        val inner = w - 6
        val curPx = tr.getWidth(text.substring(0, cursor))
        if (curPx - scrollPx > inner) scrollPx = curPx - inner
        if (curPx < scrollPx)         scrollPx = curPx
        scrollPx = scrollPx.coerceAtLeast(0)
    }
}
