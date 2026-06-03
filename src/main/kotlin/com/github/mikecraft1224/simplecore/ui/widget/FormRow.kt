package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.UiTheme
import com.github.mikecraft1224.simplecore.ui.Widget
import com.github.mikecraft1224.simplecore.ui.currentTheme
import com.github.mikecraft1224.simplecore.ui.row
import com.github.mikecraft1224.simplecore.ui.width

/**
 * A label-plus-widget row for settings forms.
 *
 * Places a text label on the left at a fixed width and a content widget on the right,
 * filling the remaining horizontal space. Eliminates the need to write a `row {}` block
 * for every labeled input.
 *
 * ```kotlin
 * column(4) {
 *     +FormRow("Name")    { TextField(placeholder = "Enter name...") }
 *     +FormRow("Enabled") { Toggle(isOn = { enabled }, onChanged = { enabled = it }) }
 * }
 * ```
 *
 * @param label       label text shown on the left
 * @param theme       theme used for the label color (defaults to [currentTheme])
 * @param labelWidth  fixed pixel width of the label column (default 80)
 * @param spacing     gap in pixels between the label and the content widget (default 8)
 * @param content     factory lambda returning the right-side content widget
 */
class FormRow(
    label: String,
    theme: UiTheme = currentTheme,
    val labelWidth: Int = 80,
    val spacing: Int = 8,
    content: () -> Widget,
) : Panel() {

    private val labelWidget   = Label(label, theme, color = theme.subtext)
    private val contentWidget = content()
    private val inner = row(spacing) {
        +labelWidget.width(labelWidth)
        +contentWidget
    }

    init {
        super.add(inner)
    }

    // FormRow wraps exactly one inner layout.
    override fun add(widget: Widget): Panel =
        error("FormRow is a composite; add widgets via the content lambda in the constructor")

    override fun doLayout() {
        inner.layout(x, y, width, height)
    }
}
