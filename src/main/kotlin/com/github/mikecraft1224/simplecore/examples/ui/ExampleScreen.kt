package com.github.mikecraft1224.simplecore.examples.ui

import com.github.mikecraft1224.simplecore.ui.Alignment
import com.github.mikecraft1224.simplecore.ui.Justify
import com.github.mikecraft1224.simplecore.ui.Panel
import com.github.mikecraft1224.simplecore.ui.UiScreen
import com.github.mikecraft1224.simplecore.ui.UiTheme
import com.github.mikecraft1224.simplecore.ui.align
import com.github.mikecraft1224.simplecore.ui.column
import com.github.mikecraft1224.simplecore.ui.frame
import com.github.mikecraft1224.simplecore.ui.height
import com.github.mikecraft1224.simplecore.ui.hSplit
import com.github.mikecraft1224.simplecore.ui.padding
import com.github.mikecraft1224.simplecore.ui.row
import com.github.mikecraft1224.simplecore.ui.size
import com.github.mikecraft1224.simplecore.ui.vSplit
import com.github.mikecraft1224.simplecore.ui.weight
import com.github.mikecraft1224.simplecore.ui.width
import com.github.mikecraft1224.simplecore.ui.widget.Button
import com.github.mikecraft1224.simplecore.ui.widget.Divider
import com.github.mikecraft1224.simplecore.ui.widget.FormRow
import com.github.mikecraft1224.simplecore.ui.widget.Label
import com.github.mikecraft1224.simplecore.ui.widget.Spacer
import com.github.mikecraft1224.simplecore.ui.widget.TextField
import com.github.mikecraft1224.simplecore.ui.widget.Toggle
import com.github.mikecraft1224.simplecore.ui.withTheme
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

/**
 * Demonstrates all SimpleCore UI widgets, layout containers, and ergonomic API features.
 *
 * Layout overview:
 *   - Full screen VSplit: top bar (12%) / main content (88%)
 *   - Top bar: row with title label, Spacer (flex), close button
 *   - Main content: HSplit - left column (40%) / right panel (60%)
 *   - Left column: LinearLayout (vertical) showing Label, FormRow, Toggle, Divider,
 *       dynamic label, weight-based spacer, alignment demo
 *   - Right panel: column of sections - FrameLayout demo, VSplit demo, Justify demo
 *
 * Press F4 to toggle the debug bounds overlay (inherited from UiScreen).
 */
@Suppress("Unused")
class ExampleScreen(private val parent: Screen? = null) : UiScreen(Text.literal("SimpleCore UI Example")) {

    private val theme = UiTheme.DEFAULT

    // Mutable state wired to widgets
    private var toggleA   = false
    private var toggleB   = true
    private var labelText = "Dynamic label"

    override fun buildRoot(): Panel = withTheme(theme) {

        // Top bar
        val topBar = padding(
            row(spacing = 4) {
                +Label("SimpleCore UI Example", shadow = true).width(180).height(20)
                +Spacer()
                +Button("Close", onClick = { MinecraftClient.getInstance().setScreen(parent) })
                    .size(50, 20)
            },
            h = 6, v = 4,
        )

        // Left column
        val leftPanel = padding(
            column(spacing = 4) {
                +Label("Widgets", color = theme.subtext).height(20)
                +Divider().height(8)

                // FormRow for labeled inputs
                +FormRow("Toggle A") {
                    Toggle(isOn = { toggleA }, onChanged = { toggleA = it })
                }.height(20)

                +FormRow("Toggle B") {
                    Toggle(isOn = { toggleB }, onChanged = { toggleB = it })
                }.height(20)

                +Divider(label = "Alignment demo").height(14)

                // End-aligned label using textAlign
                +Label("Right-aligned", color = theme.overlay0, textAlign = Alignment.END)
                    .height(16)

                // Center-aligned label
                +Label("Centered", color = theme.accent, textAlign = Alignment.CENTER)
                    .height(16)

                // Flex spacer pushes state readout to the bottom
                +Spacer().weight(1)

                +Divider().height(8)

                // Dynamic label - text provider evaluated each frame; no anonymous class needed
                +Label({ "A=$toggleA  B=$toggleB" }).height(20)
            },
            px = 8,
        )

        // Right panel - column of sections
        val rightPanel = padding(
            column(spacing = 0) {
                // -- Fixed-position demo (FrameLayout) ------------------------
                +frame {
                    Label("FrameLayout demo", color = theme.subtext, shadow = true).at(0, 0, 200, 20)
                    Divider().at(0, 22, 200, 8)

                    Label("Name:", color = theme.subtext).at(0, 36, 56, 20)
                    TextField(placeholder = "Enter name...").at(60, 36, 140, 20)

                    Label("Value:", color = theme.subtext).at(0, 62, 56, 20)
                    TextField(placeholder = "Enter value...").at(60, 62, 140, 20)

                    Divider(vertical = true).at(212, 0, 8, 90)

                    Label("Weighted row (1:2):", color = theme.overlay0).at(224, 0, 160, 20)
                    row(spacing = 4) {
                        +Button("A", onClick = { labelText = "A pressed" }).weight(1)
                        +Button("B", onClick = { labelText = "B pressed" }).weight(2)
                    }.at(224, 22, 160, 24)

                    Label({ labelText }).at(224, 52, 160, 20)
                }.height(90)

                // -- Divider between sections ----------------------------------
                +Divider(label = "Split & Justify demos").height(14)

                // -- VSplit demo -----------------------------------------------
                +column(spacing = 4) {
                    +Label("VSplit (50/50):", color = theme.overlay0).height(14)
                    +vSplit(0.5f,
                        top    = align(Label("Top half",    color = theme.accent,  shadow = true)),
                        bottom = align(Label("Bottom half", color = theme.subtext, shadow = true)),
                    ).height(36)
                }.height(58)

                // -- Justify demo ----------------------------------------------
                +column(spacing = 4) {
                    +Label("row(justify = CENTER):", color = theme.overlay0).height(14)
                    +row(spacing = 4, justify = Justify.CENTER) {
                        +Button("A", onClick = {}).size(40, 20)
                        +Button("B", onClick = {}).size(40, 20)
                        +Button("C", onClick = {}).size(40, 20)
                    }.height(20)
                    +Label("row(justify = SPACE_BETWEEN):", color = theme.overlay0).height(14)
                    +row(spacing = 0, justify = Justify.SPACE_BETWEEN) {
                        +Button("X", onClick = {}).size(40, 20)
                        +Button("Y", onClick = {}).size(40, 20)
                        +Button("Z", onClick = {}).size(40, 20)
                    }.height(20)
                }.height(76)
            },
            px = 8,
        )

        // Compose full screen
        vSplit(0.12f,
            top    = topBar,
            bottom = hSplit(0.4f, leftPanel, rightPanel),
        )
    }
}
