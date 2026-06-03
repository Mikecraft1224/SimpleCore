@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.ChildRegistrar
import com.github.mikecraft1224.simplecore.ui.UiTheme
import com.github.mikecraft1224.simplecore.ui.Widget
import com.github.mikecraft1224.simplecore.ui.currentTheme
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text

/**
 * A single-line text input wrapping Minecraft's [TextFieldWidget].
 *
 * The underlying [TextFieldWidget] is registered as a drawable child via [onAdded],
 * so it participates in Minecraft's normal focus and keyboard routing automatically.
 *
 * ```kotlin
 * var name = ""
 * val field = TextField(theme, placeholder = "Enter name...") { name = it }
 * ```
 *
 * @param theme       theme used for hint text color
 * @param placeholder hint text shown when the field is empty
 * @param onChanged   callback invoked with each new value as the user types
 */
class TextField(
    private val theme: UiTheme = currentTheme,
    private val placeholder: String = "",
    private val onChanged: (String) -> Unit = {},
) : Widget {

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    private var field: TextFieldWidget? = null

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
        // Reposition the underlying widget if it already exists (e.g. on resize)
        field?.apply {
            this.x     = x
            this.y     = y + (height - this.height) / 2
            this.width = width
        }
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        // TextFieldWidget renders as a Drawable child - nothing extra needed here.
        // Draw placeholder when field is empty and does not have focus.
        val tf = field ?: return
        if (tf.text.isEmpty() && placeholder.isNotEmpty()) {
            val tr = MinecraftClient.getInstance().textRenderer
            ctx.drawText(tr, placeholder, x + 4, y + (height - tr.fontHeight) / 2, theme.overlay0, false)
        }
    }

    override fun onAdded(registrar: ChildRegistrar) {
        val existing = field
        if (existing != null) {
            registrar.register(existing)
            return
        }
        val tr = MinecraftClient.getInstance().textRenderer
        val fieldH = 16
        val tf = TextFieldWidget(tr, x, y + (height - fieldH).coerceAtLeast(0) / 2, width, fieldH, Text.empty())
        tf.setChangedListener { v -> onChanged(v) }
        field = tf
        registrar.register(tf)
    }

    override fun onRemoved() {
        field = null
    }

    override fun keyPressed(input: KeyInput): Boolean = field?.keyPressed(input) ?: false

    override fun charTyped(input: CharInput): Boolean = field?.charTyped(input) ?: false

    /** Returns the current text content of the field. */
    fun getText(): String = field?.text ?: ""

    /** Programmatically sets the text. Does not fire [onChanged]. */
    fun setText(value: String) {
        field?.text = value
    }
}
