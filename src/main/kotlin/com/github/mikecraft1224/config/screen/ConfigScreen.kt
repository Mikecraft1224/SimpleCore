package com.github.mikecraft1224.config.screen

import com.github.mikecraft1224.config.ConfigManager
import com.github.mikecraft1224.config.ProcessedCategory
import com.github.mikecraft1224.config.ProcessedConfig
import com.github.mikecraft1224.config.ProcessedEntry
import com.github.mikecraft1224.config.KeybindPacked
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.client.input.KeyInput
import net.minecraft.client.util.InputUtil
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.max
import kotlin.math.round

/**
 * Two-panel config screen built from a [ProcessedConfig].
 *
 * Left: category list. Top-right: title bar + search. Right: scrollable entry rows.
 * All config widgets are custom-drawn - only [TextFieldWidget] children are used (search + text entries).
 */
class ConfigScreen(
    private val parent: Screen?,
    private val model: ProcessedConfig,
    private val manager: ConfigManager<*>,
    title: String = "Config",
) : Screen(Text.literal("Config")) {

    private val displayTitle    = model.title.ifEmpty { title }
    private val displaySubtitle = model.subtitle

    // ── Layout ────────────────────────────────────────────────────────────

    companion object {
        private const val CAT_W      = 120
        private const val SEARCH_H   = 26
        private const val ROW_H      = 26
        private const val BOT_H      = 28
        private const val SLIDER_INP = 36  // width of the inline value input next to sliders

        // Catppuccin Mocha
        private val C_MANTLE    = 0xFF181825.toInt()
        private val C_BASE      = 0xFF1E1E2E.toInt()
        private val C_SURFACE0  = 0xFF313244.toInt()
        private val C_SURFACE1  = 0xFF45475A.toInt()
        private val C_OVERLAY0  = 0xFF6C7086.toInt()
        private val C_SUBTEXT   = 0xFFBAC2DE.toInt()
        private val C_TEXT      = 0xFFCDD6F4.toInt()
        private val C_GREEN     = 0xFF40A02B.toInt()
        private val C_BLUE      = 0xFF89B4FA.toInt()
        private val C_RED       = 0xFFF38BA8.toInt()

        // Color overlay slider palette
        private val C_RED_CH    = 0xFFED8796.toInt()
        private val C_GRN_CH    = 0xFFA6E3A1.toInt()
        private val C_BLU_CH    = 0xFF89B4FA.toInt()
        private val C_ALPHA_CH  = 0xFFBAC2DE.toInt()
    }

    // Header height grows by 12 when a subtitle is present
    private val headerH     get() = if (displaySubtitle.isEmpty()) 22 else 34

    private val entryLeft   get() = CAT_W
    private val entryRight  get() = width
    private val entryTop    get() = headerH + SEARCH_H
    private val entryBottom get() = height - BOT_H
    private val entryW      get() = entryRight - entryLeft
    private val visH        get() = entryBottom - entryTop
    private fun widgetX()   = entryLeft + (entryW * 0.58f).toInt()
    private fun widgetW()   = (entryW * 0.36f).toInt()

    // ── State ─────────────────────────────────────────────────────────────

    private var selCat           = 0
    private var selSubCat        = -1   // -1 = parent category's own entries
    private var scroll           = 0
    private var searchText       = ""
    private var searchField: TextFieldWidget? = null
    private var capturingKeybind: ProcessedEntry.KeybindEntry? = null
    private var draggingSlider:   ProcessedEntry.SliderEntry?  = null
    private var pendingTooltip:   Pair<String, Pair<Int, Int>>? = null
    private var colorOverlay:     ColorOverlay?                = null
    private var dropdownOverlay:  DropdownOverlay?             = null
    private var listOverlay:      ListOverlay?                 = null

    private val selCategory  get() = model.categories.getOrNull(selCat)
    private val selEntries   get() = if (selSubCat >= 0)
        selCategory?.subcategories?.getOrNull(selSubCat)?.entries ?: emptyList()
    else
        selCategory?.entries ?: emptyList()

    // ── Sidebar items ─────────────────────────────────────────────────────

    private data class SidebarItem(
        val catIdx:    Int,
        val subCatIdx: Int,     // -1 = parent category itself
        val name:      String,
        val y:         Int,
        val h:         Int,
    )

    private fun buildSidebarItems(): List<SidebarItem> = buildList {
        var y = headerH + 10
        for ((i, cat) in model.categories.withIndex()) {
            add(SidebarItem(i, -1, cat.name, y, 22))
            y += 22
            if (i == selCat && cat.subcategories.isNotEmpty()) {
                for ((j, sub) in cat.subcategories.withIndex()) {
                    add(SidebarItem(i, j, sub.name, y, 20))
                    y += 20
                }
            }
        }
    }

    // ── Row model ─────────────────────────────────────────────────────────

    private sealed class Row {
        data class EntryRow(val entry: ProcessedEntry, val indent: Int) : Row()
        data class ColHeader(val group: ProcessedEntry.CollapsibleGroup, val indent: Int) : Row()
        data class SearchHeader(val name: String) : Row()
        data class Sep(val sep: ProcessedEntry.SeparatorEntry) : Row()
    }

    private var rows: List<Row> = emptyList()
    private val maxScroll get() = max(0, rows.size * ROW_H - visH)

    private fun buildRows(entries: List<ProcessedEntry>, indent: Int = 0): List<Row> = buildList {
        for (e in entries) when {
            e is ProcessedEntry.CollapsibleGroup -> {
                add(Row.ColHeader(e, indent))
                if (!e.collapsed) addAll(buildRows(e.children, indent + 1))
            }
            e is ProcessedEntry.SeparatorEntry  -> add(Row.Sep(e))
            else                                -> add(Row.EntryRow(e, indent))
        }
    }

    private fun flattenEntries(entries: List<ProcessedEntry>): List<ProcessedEntry> = buildList {
        for (e in entries) when (e) {
            is ProcessedEntry.CollapsibleGroup -> addAll(flattenEntries(e.children))
            !is ProcessedEntry.SeparatorEntry  -> add(e)
            else                               -> {}
        }
    }

    /**
     * During search: keeps category structure - one [Row.SearchHeader] per category that has
     * matches, followed by its matching entries. Empty categories are omitted entirely.
     */
    private fun buildSearchRows(query: String): List<Row> = buildList {
        fun matches(e: ProcessedEntry) =
            e.name.contains(query, ignoreCase = true) ||
            e.searchTags.any { it.contains(query, ignoreCase = true) }
        for (cat in model.categories) {
            val directMatching = flattenEntries(cat.entries).filter { matches(it) }
            if (directMatching.isNotEmpty()) {
                add(Row.SearchHeader(cat.name))
                directMatching.forEach { add(Row.EntryRow(it, 1)) }
            }
            for (sub in cat.subcategories) {
                val subMatching = flattenEntries(sub.entries).filter { matches(it) }
                if (subMatching.isNotEmpty()) {
                    add(Row.SearchHeader("${cat.name} > ${sub.name}"))
                    subMatching.forEach { add(Row.EntryRow(it, 1)) }
                }
            }
        }
    }

    private fun rebuildRows() {
        rows = if (searchText.isBlank()) buildRows(selEntries)
               else                      buildSearchRows(searchText)
        scroll = scroll.coerceIn(0, maxScroll)
    }

    // ── Init / widget management ──────────────────────────────────────────

    override fun init() {
        if (searchField == null) {
            searchField = TextFieldWidget(
                textRenderer, entryLeft + 6, headerH + 4, entryW - 12, 18, Text.empty(),
            ).also {
                it.setChangedListener { v -> searchText = v; rebuildRows(); syncWidgets() }
            }
        } else {
            searchField!!.apply {
                x = entryLeft + 6; y = headerH + 4; width = entryW - 12
            }
        }
        rebuildRows()
        syncWidgets()
    }

    private fun syncWidgets() {
        clearChildren()
        // When the list overlay is open it owns all text field children.
        val lo = listOverlay
        if (lo != null) { lo.syncTextFields(); return }
        // Re-add color overlay hex field if color overlay is open
        colorOverlay?.initHexField()

        addDrawableChild(searchField!!)
        val wx = widgetX(); val ww = widgetW()
        for ((i, row) in rows.withIndex()) {
            if (row !is Row.EntryRow) continue
            val ry = entryTop + i * ROW_H - scroll
            if (ry + ROW_H <= entryTop || ry >= entryBottom) continue
            when (val e = row.entry) {
                is ProcessedEntry.TextEntry -> addDrawableChild(
                    TextFieldWidget(textRenderer, wx, ry + 3, ww, ROW_H - 6, Text.empty()).also {
                        it.setText(e.get())
                        it.setChangedListener { v -> e.set(v) }
                    }
                )
                is ProcessedEntry.SliderEntry -> {
                    val dec   = if (e.step >= 1.0) 0 else e.step.toString().substringAfter('.').trimEnd('0').length
                    val inpX  = wx + ww - SLIDER_INP
                    val inpW  = SLIDER_INP
                    addDrawableChild(
                        TextFieldWidget(textRenderer, inpX, ry + 3, inpW, ROW_H - 6, Text.empty()).also { tf ->
                            tf.setText("%.${dec}f".format(e.get()))
                            tf.setChangedListener { v ->
                                val parsed = v.toDoubleOrNull() ?: return@setChangedListener
                                val stepped = (round(parsed / e.step) * e.step).coerceIn(e.min, e.max)
                                e.set(stepped)
                            }
                        }
                    )
                }
                else -> {}
            }
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────

    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderBackground(context, mouseX, mouseY, delta) // blur - once per frame

        // Panel fills
        context.fill(0, 0, CAT_W, height, C_MANTLE)
        context.fill(CAT_W, 0, width, height, C_BASE)

        // Header bar
        context.fill(0, 0, width, headerH, C_MANTLE)
        context.fill(0, headerH - 1, width, headerH, C_SURFACE0)
        if (displaySubtitle.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, displayTitle, width / 2, (headerH - textRenderer.fontHeight) / 2, C_TEXT)
        } else {
            context.drawCenteredTextWithShadow(textRenderer, displayTitle,    width / 2, 7,  C_TEXT)
            context.drawCenteredTextWithShadow(textRenderer, displaySubtitle, width / 2, 20, C_OVERLAY0)
        }

        // Dividers
        context.fill(CAT_W - 1, headerH,      CAT_W, height,          C_SURFACE0)
        context.fill(CAT_W,     entryTop - 1, width, entryTop,        C_SURFACE0)
        context.fill(0,         height - BOT_H, width, height - BOT_H + 1, C_SURFACE0)

        // Search placeholder
        if (searchText.isEmpty()) {
            context.drawText(textRenderer, "Search...", entryLeft + 8, headerH + 8, C_OVERLAY0, false)
        }

        // Category sidebar (accordion: sub-categories expand below selected parent)
        for (item in buildSidebarItems()) {
            val cat = model.categories[item.catIdx]
            val isParent  = item.subCatIdx < 0
            val isSelected = item.catIdx == selCat && item.subCatIdx == selSubCat
            val hasMatch = searchText.isBlank() || run {
                val entries = if (isParent) cat.entries else cat.subcategories.getOrNull(item.subCatIdx)?.entries ?: emptyList()
                flattenEntries(entries).any { it.name.contains(searchText, ignoreCase = true) } ||
                isParent && cat.subcategories.any { sub ->
                    flattenEntries(sub.entries).any { it.name.contains(searchText, ignoreCase = true) }
                }
            }
            val hov = mouseX in 0 until CAT_W && mouseY in item.y until item.y + item.h
            if (isSelected) context.fill(4, item.y - 1, CAT_W - 4, item.y + item.h - 1, C_SURFACE1)
            val col = when {
                isSelected -> C_TEXT
                !hasMatch  -> C_SURFACE0
                hov        -> C_SUBTEXT
                else       -> C_OVERLAY0
            }
            val indentX = if (isParent) 10 else 18
            val prefix  = if (!isParent) "• " else ""
            context.drawText(textRenderer, "$prefix${item.name}", indentX, item.y + (item.h - textRenderer.fontHeight) / 2, col, false)
        }

        // Entry rows (scissored)
        pendingTooltip = null
        context.enableScissor(entryLeft, entryTop, entryRight, entryBottom)

        for ((rowIdx, row) in rows.withIndex()) {
            val ry  = entryTop + rowIdx * ROW_H - scroll
            if (ry + ROW_H <= entryTop || ry >= entryBottom) continue

            val hov = mouseX in entryLeft until entryRight && mouseY in ry until ry + ROW_H
            // ColHeader/Sep manage their own background; generic hover only for regular entry rows
            if (hov && row !is Row.SearchHeader && row !is Row.ColHeader && row !is Row.Sep)
                context.fill(entryLeft, ry, entryRight - 4, ry + ROW_H, 0x0AFFFFFF)

            when (row) {
                is Row.Sep -> {
                    val ly = ry + ROW_H / 2
                    val label = row.sep.label
                    if (label.isEmpty()) {
                        context.fill(entryLeft + 8, ly, entryRight - 8, ly + 1, 0x44CDD6F4)
                    } else {
                        val lw    = textRenderer.getWidth(label)
                        val cx    = (entryLeft + entryRight) / 2
                        val gap   = 6
                        context.fill(entryLeft + 8,    ly, cx - lw / 2 - gap, ly + 1, 0x44CDD6F4)
                        context.fill(cx + lw / 2 + gap, ly, entryRight - 8,    ly + 1, 0x44CDD6F4)
                        context.drawCenteredTextWithShadow(textRenderer, label, cx, ly - textRenderer.fontHeight / 2, C_OVERLAY0)
                    }
                }
                is Row.SearchHeader -> {
                    context.fill(entryLeft, ry, entryRight, ry + ROW_H, 0x22FFFFFF)
                    context.drawText(
                        textRenderer, row.name.uppercase(),
                        entryLeft + 8, ry + (ROW_H - textRenderer.fontHeight) / 2,
                        C_BLUE, false,
                    )
                }
                is Row.ColHeader -> {
                    val indentX = row.indent * 10
                    // Accordion header background
                    context.fill(entryLeft, ry, entryRight - 4, ry + ROW_H, if (hov) C_SURFACE1 else C_SURFACE0)
                    // Guide lines at parent levels (matches EntryRow style)
                    for (level in 0 until row.indent) {
                        val gx = entryLeft + level * 10 + 1
                        context.fill(gx, ry, gx + 1, ry + ROW_H, 0x22FFFFFF)
                    }
                    // Left accent bar (depth-colored: blue at root, dimmer for nested)
                    val barColor = if (row.indent == 0) C_BLUE else C_OVERLAY0
                    context.fill(entryLeft + indentX, ry + 1, entryLeft + indentX + 3, ry + ROW_H - 1, barColor)
                    // Arrow and name
                    val tx  = entryLeft + indentX + 8
                    val ty  = ry + (ROW_H - textRenderer.fontHeight) / 2
                    val arrow = if (row.group.collapsed) "+" else "-"
                    context.drawText(textRenderer, "$arrow  ${row.group.name}", tx, ty, C_TEXT, true)
                    // Bottom hairline separator when expanded to visually anchor children
                    if (!row.group.collapsed)
                        context.fill(entryLeft + indentX + 3, ry + ROW_H - 1, entryRight - 4, ry + ROW_H, 0x33FFFFFF)
                    if (row.group.description.isNotEmpty() && hov)
                        pendingTooltip = row.group.description to (mouseX to mouseY)
                }
                is Row.EntryRow -> {
                    val e  = row.entry
                    val tx = entryLeft + 8 + row.indent * 10
                    val ty = ry + (ROW_H - textRenderer.fontHeight) / 2
                    // Vertical guide lines for each level of nesting (tree-view style)
                    for (level in 0 until row.indent) {
                        val gx = entryLeft + level * 10 + 1
                        context.fill(gx, ry, gx + 1, ry + ROW_H, 0x22FFFFFF)
                    }
                    if (e is ProcessedEntry.InfoEntry) {
                        context.drawText(textRenderer, e.name, tx, ty, C_SUBTEXT, false)
                        context.drawText(textRenderer, e.getText(), widgetX(), ty, C_BLUE, false)
                    } else {
                        context.drawText(textRenderer, e.name, tx, ty, C_TEXT, false)
                        if (e.description.isNotEmpty() && hov)
                            pendingTooltip = e.description to (mouseX to mouseY)
                        if (e !is ProcessedEntry.TextEntry)
                            drawEntryWidget(context, e, widgetX(), ry + 3, widgetW(), ROW_H - 6, mouseX, mouseY)
                    }
                }
            }
        }
        context.disableScissor()

        // List overlay background drawn here so MC's text field children appear on top
        listOverlay?.renderBackground(context, mouseX, mouseY)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)

        // Scrollbar
        if (maxScroll > 0) {
            val thumbH = (visH * visH.toFloat() / (rows.size * ROW_H)).toInt().coerceAtLeast(16)
            val thumbY = entryTop + ((scroll.toFloat() / maxScroll) * (visH - thumbH)).toInt()
            context.fill(entryRight - 3, entryTop,  entryRight, entryBottom, 0x22FFFFFF)
            context.fill(entryRight - 3, thumbY, entryRight, thumbY + thumbH, 0xAABAC2DE.toInt())
        }

        // Done button
        val bw = 80; val bh = 20
        val bx = width / 2 - bw / 2
        val by = height - BOT_H + (BOT_H - bh) / 2
        val doneHov = mouseX in bx until bx + bw && mouseY in by until by + bh
        context.fill(bx, by, bx + bw, by + bh, if (doneHov) C_SURFACE1 else C_SURFACE0)
        context.drawCenteredTextWithShadow(textRenderer, "Done", bx + bw / 2, by + (bh - textRenderer.fontHeight) / 2, C_TEXT)

        // Tooltip (suppressed when any overlay is open)
        if (colorOverlay == null && dropdownOverlay == null && listOverlay == null)
            pendingTooltip?.let { (msg, pos) -> context.drawTooltip(Text.literal(msg), pos.first, pos.second) }

        // Overlays - rendered last, on top of everything
        colorOverlay?.render(context, mouseX, mouseY)
        dropdownOverlay?.render(context, mouseX, mouseY)
        listOverlay?.renderForeground(context, mouseX, mouseY)
    }

    // ── Widget drawing ────────────────────────────────────────────────────

    private fun drawEntryWidget(ctx: DrawContext, entry: ProcessedEntry, x: Int, y: Int, w: Int, h: Int, mx: Int, my: Int) {
        val hov = mx in x until x + w && my in y until y + h
        when (entry) {
            is ProcessedEntry.BoolEntry        -> drawToggle(ctx, entry.get(), x, y, w, h, hov)
            is ProcessedEntry.SliderEntry      -> drawSlider(ctx, entry, x, y, w, h, hov)
            is ProcessedEntry.DropdownEntry    -> drawDropdown(ctx, entry, x, y, w, h, hov)
            is ProcessedEntry.ButtonEntry      -> drawBox(ctx, entry.buttonText, x, y, w, h, hov)
            is ProcessedEntry.ColorEntry       -> drawColorSwatch(ctx, entry.get(), x, y, w, h, hov)
            is ProcessedEntry.KeybindEntry     -> drawKeybind(ctx, entry, x, y, w, h, hov)
            is ProcessedEntry.MutableListEntry -> drawBox(ctx, "Edit (${entry.getList().size} items)", x, y, w, h, hov)
            else                               -> {}
        }
    }

    /**
     * Draws a pill-shaped toggle switch centered in the widget area.
     * Track is 34x14, knob is 10x10, knob slides left/right based on [on].
     */
    private fun drawToggle(ctx: DrawContext, on: Boolean, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        val trackW = 34
        val trackH = 14
        val knobSz = 10
        val pad    = 2
        val tx     = x + (w - trackW) / 2
        val ty     = y + (h - trackH) / 2
        val trackBg = when {
            on && hov  -> lighten(C_GREEN)
            on         -> C_GREEN
            hov        -> C_SURFACE1
            else       -> C_SURFACE0
        }
        // Track background
        ctx.fill(tx,              ty,              tx + trackW, ty + trackH, trackBg)
        // Rounded corners (2px corner cut)
        ctx.fill(tx,              ty,              tx + 2,      ty + 2,      C_BASE)
        ctx.fill(tx + trackW - 2, ty,              tx + trackW, ty + 2,      C_BASE)
        ctx.fill(tx,              ty + trackH - 2, tx + 2,      ty + trackH, C_BASE)
        ctx.fill(tx + trackW - 2, ty + trackH - 2, tx + trackW, ty + trackH, C_BASE)
        // Knob
        val knobX = if (on) tx + trackW - knobSz - pad else tx + pad
        val knobY = ty + (trackH - knobSz) / 2
        ctx.fill(knobX,          knobY,          knobX + knobSz, knobY + knobSz, C_TEXT)
        // Rounded knob corners
        ctx.fill(knobX,              knobY,              knobX + 1, knobY + 1,           trackBg)
        ctx.fill(knobX + knobSz - 1, knobY,              knobX + knobSz, knobY + 1,      trackBg)
        ctx.fill(knobX,              knobY + knobSz - 1, knobX + 1, knobY + knobSz,      trackBg)
        ctx.fill(knobX + knobSz - 1, knobY + knobSz - 1, knobX + knobSz, knobY + knobSz, trackBg)
    }

    private fun drawSlider(ctx: DrawContext, e: ProcessedEntry.SliderEntry, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        val sliderW = w - SLIDER_INP - 4
        val ratio   = ((e.get() - e.min) / (e.max - e.min)).coerceIn(0.0, 1.0)
        val fillW   = (sliderW * ratio).toInt()
        val midY    = y + h / 2
        ctx.fill(x, midY - 2, x + sliderW, midY + 2, C_SURFACE0)
        ctx.fill(x, midY - 2, x + fillW,   midY + 2, C_BLUE)
        val tx = x + fillW
        ctx.fill(tx - 3, y + 2, tx + 3, y + h - 2, if (hov || draggingSlider === e) C_TEXT else C_SUBTEXT)
        // The value text field is rendered as a child widget (see syncWidgets)
    }

    private fun drawDropdown(ctx: DrawContext, e: ProcessedEntry.DropdownEntry, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        val isOpen = dropdownOverlay?.entry === e
        ctx.fill(x, y, x + w, y + h, if (isOpen || hov) C_SURFACE1 else C_SURFACE0)
        val label = e.options.getOrElse(e.get().coerceIn(0, e.options.lastIndex)) { "?" }
        val lw    = textRenderer.getWidth(label)
        val ty    = y + (h - textRenderer.fontHeight) / 2
        ctx.drawText(textRenderer, label, x + (w - lw) / 2, ty, C_TEXT, false)
        val arrow = if (isOpen) "▲" else "▼"
        ctx.drawText(textRenderer, arrow, x + w - textRenderer.getWidth(arrow) - 4, ty, C_OVERLAY0, false)
    }

    private fun drawBox(ctx: DrawContext, label: String, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        ctx.fill(x, y, x + w, y + h, if (hov) C_SURFACE1 else C_SURFACE0)
        val lw = textRenderer.getWidth(label)
        ctx.drawText(textRenderer, label, x + (w - lw) / 2, y + (h - textRenderer.fontHeight) / 2, C_TEXT, false)
    }

    private fun drawColorSwatch(ctx: DrawContext, color: Color, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        // Full widget width is the clickable swatch area
        val argb = (color.alpha shl 24) or (color.red shl 16) or (color.green shl 8) or color.blue
        // Checkerboard background for alpha visibility
        ctx.fill(x, y + 1, x + w, y + h - 1, 0xFF888888.toInt())
        ctx.fill(x,         y + 1,     x + w / 2, y + h / 2, 0xFFAAAAAA.toInt())
        ctx.fill(x + w / 2, y + h / 2, x + w,     y + h - 1, 0xFFAAAAAA.toInt())
        ctx.fill(x, y + 1, x + w, y + h - 1, argb)
        // Border - brighter when hovered to signal clickability
        val borderCol = if (hov) C_SUBTEXT else C_SURFACE1
        ctx.fill(x - 1,     y,         x + w + 1, y + 1,     borderCol)
        ctx.fill(x - 1,     y + h,     x + w + 1, y + h + 1, borderCol)
        ctx.fill(x - 1,     y,         x,         y + h + 1, borderCol)
        ctx.fill(x + w,     y,         x + w + 1, y + h + 1, borderCol)
        // "click to edit" hint text inside swatch when hovered
        if (hov) {
            val hint = "click to edit"
            val hw   = textRenderer.getWidth(hint)
            if (hw + 4 <= w)
                ctx.drawText(textRenderer, hint, x + (w - hw) / 2, y + (h - textRenderer.fontHeight) / 2, 0xAAFFFFFF.toInt(), false)
        }
    }

    private fun drawKeybind(ctx: DrawContext, e: ProcessedEntry.KeybindEntry, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        val capturing = capturingKeybind === e
        ctx.fill(x, y, x + w, y + h, when { capturing -> C_RED; hov -> C_SURFACE1; else -> C_SURFACE0 })
        val label = if (capturing) {
            "Press a key..."
        } else {
            val packed = e.get()
            val keyCode = KeybindPacked.keyCode(packed)
            val mods    = KeybindPacked.modifiers(packed)
            val keyName = InputUtil.fromKeyCode(KeyInput(keyCode, 0, 0)).getLocalizedText().string
            buildString {
                if (mods.ctrl)  append("Ctrl + ")
                if (mods.shift) append("Shift + ")
                if (mods.alt)   append("Alt + ")
                append(keyName)
            }
        }
        val lw = textRenderer.getWidth(label)
        ctx.drawText(textRenderer, label, x + (w - lw) / 2, y + (h - textRenderer.fontHeight) / 2, C_TEXT, false)
    }

    // ── Color picker overlay ──────────────────────────────────────────────

    /**
     * HSV-based color picker with:
     * - 2D hue/saturation spectrum (drag to set H+S)
     * - Brightness (V) slider
     * - Opacity (A) slider
     * - Inline hex field (#RRGGBBAA)
     * - Live preview swatch
     * - Apply / Cancel buttons
     */
    private inner class ColorOverlay(val entry: ProcessedEntry.ColorEntry) {
        // Working state in HSV + alpha
        private var h: Float
        private var s: Float
        private var v: Float
        private var a: Int = entry.get().alpha

        init {
            val hsv = rgbToHsv((entry.get().red shl 16) or (entry.get().green shl 8) or entry.get().blue)
            h = hsv[0]; s = hsv[1]; v = hsv[2]
        }

        // Drag state: 0=spectrum, 1=V slider, 2=A slider, -1=none
        var dragging = -1

        // Hex text field
        private var hexField: TextFieldWidget? = null
        private var hexDirty = false  // true while user is editing hex (suppress feedback loop)

        private val DLG_W    = 220
        private val DLG_H    = 240
        private val SPEC_W   = 160
        private val SPEC_H   = 100
        private val SL_W     = 160
        private val SL_H     = 12
        private val PAD      = 16

        private fun dlgX()   = (width  - DLG_W) / 2
        private fun dlgY()   = (height - DLG_H) / 2
        private fun specX()  = dlgX() + (DLG_W - SPEC_W) / 2
        private fun specY()  = dlgY() + 26
        private fun slX()    = dlgX() + (DLG_W - SL_W) / 2
        private fun slVY()   = specY() + SPEC_H + 10
        private fun slAY()   = slVY() + SL_H + 14

        /** Returns the current color as packed ARGB using HSV state + alpha. */
        private fun currentArgb(): Int = hsvToArgb(h, s, v, a)

        /** Rebuilds the hex text field at the correct position. */
        fun initHexField() {
            val fieldW = SL_W
            val fieldX = slX()
            val fieldY = slAY() + SL_H + 10
            if (hexField == null) {
                hexField = TextFieldWidget(textRenderer, fieldX, fieldY, fieldW, 16, Text.empty()).also { tf ->
                    tf.setMaxLength(9)
                    tf.setText(argbToHex(currentArgb()))
                    tf.setChangedListener { txt ->
                        if (!hexDirty) return@setChangedListener
                        val clean = txt.trimStart('#')
                        if (clean.length == 8) {
                            runCatching {
                                val rgb = clean.substring(0, 6).toLong(16).toInt()
                                a = clean.substring(6, 8).toInt(16)
                                val hsv = rgbToHsv(rgb)
                                h = hsv[0]; s = hsv[1]; v = hsv[2]
                            }
                        } else if (clean.length == 6) {
                            runCatching {
                                val rgb = clean.toLong(16).toInt()
                                val hsv = rgbToHsv(rgb)
                                h = hsv[0]; s = hsv[1]; v = hsv[2]
                            }
                        }
                    }
                }
                addDrawableChild(hexField!!)
            } else {
                hexField!!.apply {
                    x = fieldX; y = fieldY; width = fieldW
                }
            }
        }

        private fun syncHexField() {
            if (!hexDirty) {
                hexField?.setText(argbToHex(currentArgb()))
            }
        }

        private fun argbToHex(argb: Int): String {
            val r = (argb shr 16) and 0xFF
            val g = (argb shr  8) and 0xFF
            val b =  argb         and 0xFF
            val al = (argb ushr 24) and 0xFF
            return "#%02X%02X%02X%02X".format(r, g, b, al)
        }

        fun render(ctx: DrawContext, mx: Int, my: Int) {
            val dx = dlgX(); val dy = dlgY()
            val sx = specX(); val sy = specY()

            // Dim backdrop
            ctx.fill(0, 0, width, height, 0x99000000.toInt())
            // Dialog shell
            ctx.fill(dx, dy, dx + DLG_W, dy + DLG_H, C_MANTLE)
            ctx.fill(dx,             dy,             dx + DLG_W, dy + 1,           C_SURFACE1)
            ctx.fill(dx,             dy + DLG_H - 1, dx + DLG_W, dy + DLG_H,      C_SURFACE1)
            ctx.fill(dx,             dy,             dx + 1,     dy + DLG_H,       C_SURFACE1)
            ctx.fill(dx + DLG_W - 1, dy,             dx + DLG_W, dy + DLG_H,      C_SURFACE1)
            ctx.drawCenteredTextWithShadow(textRenderer, "Edit Color", dx + DLG_W / 2, dy + 9, C_TEXT)

            // ── 2D Hue/Saturation spectrum ────────────────────────────────────
            // Rendered column by column: each column is a fixed hue, darkening left to right
            // is saturation (0=white on left, 1=full hue on right)
            for (px in 0 until SPEC_W) {
                val colH = px.toFloat() / SPEC_W * 360f
                for (py in 0 until SPEC_H) {
                    // y=0 (top) = full saturation, y=SPEC_H = desaturated
                    val colS = 1f - py.toFloat() / SPEC_H
                    val argb = hsvToArgb(colH, colS, v, 0xFF)
                    ctx.fill(sx + px, sy + py, sx + px + 1, sy + py + 1, argb)
                }
            }
            // Spectrum border
            ctx.fill(sx - 1, sy - 1, sx + SPEC_W + 1, sy,             C_SURFACE1)
            ctx.fill(sx - 1, sy + SPEC_H, sx + SPEC_W + 1, sy + SPEC_H + 1, C_SURFACE1)
            ctx.fill(sx - 1, sy - 1, sx,             sy + SPEC_H + 1, C_SURFACE1)
            ctx.fill(sx + SPEC_W, sy - 1, sx + SPEC_W + 1, sy + SPEC_H + 1, C_SURFACE1)
            // Selection crosshair on spectrum
            val cpX = sx + (h / 360f * SPEC_W).toInt()
            val cpY = sy + ((1f - s) * SPEC_H).toInt()
            ctx.fill(cpX - 4, cpY - 1, cpX + 5, cpY + 2, 0x88000000.toInt())
            ctx.fill(cpX - 1, cpY - 4, cpX + 2, cpY + 5, 0x88000000.toInt())
            ctx.fill(cpX - 3, cpY,     cpX + 4, cpY + 1, 0xFFFFFFFF.toInt())
            ctx.fill(cpX,     cpY - 3, cpX + 1, cpY + 4, 0xFFFFFFFF.toInt())

            // ── Brightness slider ─────────────────────────────────────────────
            val vx = slX(); val vy = slVY()
            for (px in 0 until SL_W) {
                val bv = px.toFloat() / SL_W
                val col = hsvToArgb(h, s, bv, 0xFF)
                ctx.fill(vx + px, vy, vx + px + 1, vy + SL_H, col)
            }
            ctx.drawText(textRenderer, "B", vx - 12, vy + (SL_H - textRenderer.fontHeight) / 2, C_SUBTEXT, false)
            val vKnobX = vx + (v * SL_W).toInt()
            val vHov = dragging == 1 || (mx in vx until vx + SL_W && my in vy - 3 until vy + SL_H + 3)
            ctx.fill(vKnobX - 3, vy - 2, vKnobX + 3, vy + SL_H + 2, if (vHov) C_TEXT else C_SUBTEXT)

            // ── Opacity slider ────────────────────────────────────────────────
            val ax = slX(); val ay = slAY()
            for (px in 0 until SL_W) {
                val av = px.toFloat() / SL_W
                val opaqueCol  = hsvToArgb(h, s, v, 0xFF)
                val r2 = ((opaqueCol shr 16) and 0xFF)
                val g2 = ((opaqueCol shr  8) and 0xFF)
                val b2 = (opaqueCol and 0xFF)
                val blended = (0xFF shl 24) or
                    (((r2 * (px * 255 / SL_W) + 0x1E * (255 - px * 255 / SL_W)) / 255).coerceIn(0,255) shl 16) or
                    (((g2 * (px * 255 / SL_W) + 0x1E * (255 - px * 255 / SL_W)) / 255).coerceIn(0,255) shl 8) or
                    ((b2 * (px * 255 / SL_W) + 0x1E * (255 - px * 255 / SL_W)) / 255).coerceIn(0,255)
                ctx.fill(ax + px, ay, ax + px + 1, ay + SL_H, blended)
            }
            ctx.drawText(textRenderer, "A", ax - 12, ay + (SL_H - textRenderer.fontHeight) / 2, C_SUBTEXT, false)
            val aKnobX = ax + (a * SL_W / 255)
            val aHov = dragging == 2 || (mx in ax until ax + SL_W && my in ay - 3 until ay + SL_H + 3)
            ctx.fill(aKnobX - 3, ay - 2, aKnobX + 3, ay + SL_H + 2, if (aHov) C_TEXT else C_SUBTEXT)

            // ── Preview swatch ────────────────────────────────────────────────
            val previewY = slAY() + SL_H + 10
            val previewX = slX() + SL_W + 6
            val previewW = DLG_W - PAD - (SL_W + 6) - (DLG_W - slX() - SL_W - 6)
            // The hex field sits to the left; preview fills the right side of that row
            val swW = 28; val swH = 16
            val swX = dx + DLG_W - PAD - swW
            val swY = previewY
            val previewArgb = currentArgb()
            ctx.fill(swX, swY, swX + swW, swY + swH, 0xFF888888.toInt())
            ctx.fill(swX,        swY,        swX + swW / 2, swY + swH / 2, 0xFFAAAAAA.toInt())
            ctx.fill(swX + swW / 2, swY + swH / 2, swX + swW, swY + swH, 0xFFAAAAAA.toInt())
            ctx.fill(swX, swY, swX + swW, swY + swH, previewArgb)
            ctx.fill(swX - 1, swY - 1, swX + swW + 1, swY,         C_SURFACE1)
            ctx.fill(swX - 1, swY + swH, swX + swW + 1, swY + swH + 1, C_SURFACE1)
            ctx.fill(swX - 1, swY - 1, swX,            swY + swH + 1, C_SURFACE1)
            ctx.fill(swX + swW, swY - 1, swX + swW + 1, swY + swH + 1, C_SURFACE1)

            // ── Buttons ───────────────────────────────────────────────────────
            val btnY    = dy + DLG_H - 24
            val bw      = 74; val bh = 16
            val applyX  = dx + DLG_W / 2 - bw - 2
            val cancelX = dx + DLG_W / 2 + 2
            val applyHov  = mx in applyX  until applyX  + bw && my in btnY until btnY + bh
            val cancelHov = mx in cancelX until cancelX + bw && my in btnY until btnY + bh
            ctx.fill(applyX,  btnY, applyX  + bw, btnY + bh, if (applyHov)  C_SURFACE1 else C_SURFACE0)
            ctx.fill(cancelX, btnY, cancelX + bw, btnY + bh, if (cancelHov) C_SURFACE1 else C_SURFACE0)
            ctx.drawCenteredTextWithShadow(textRenderer, "Apply",  applyX  + bw / 2, btnY + (bh - textRenderer.fontHeight) / 2, C_TEXT)
            ctx.drawCenteredTextWithShadow(textRenderer, "Cancel", cancelX + bw / 2, btnY + (bh - textRenderer.fontHeight) / 2, C_TEXT)

            syncHexField()
        }

        /** Returns true if the click was consumed (always true when overlay is open). */
        fun mouseClicked(mx: Int, my: Int): Boolean {
            val dx = dlgX(); val dy = dlgY()
            val sx = specX(); val sy = specY()
            // Spectrum
            if (mx in sx until sx + SPEC_W && my in sy until sy + SPEC_H) {
                dragging = 0; applySpectrum(mx, my); return true
            }
            // Brightness slider
            val vx = slX(); val vy = slVY()
            if (mx in vx until vx + SL_W && my in vy - 4 until vy + SL_H + 4) {
                dragging = 1; applyVSlider(mx); return true
            }
            // Opacity slider
            val ax = slX(); val ay = slAY()
            if (mx in ax until ax + SL_W && my in ay - 4 until ay + SL_H + 4) {
                dragging = 2; applyASlider(mx); return true
            }
            // Buttons
            val btnY    = dy + DLG_H - 24; val bw = 74; val bh = 16
            val applyX  = dx + DLG_W / 2 - bw - 2
            val cancelX = dx + DLG_W / 2 + 2
            if (mx in applyX until applyX + bw && my in btnY until btnY + bh) {
                commitColor(); return true
            }
            if (mx in cancelX until cancelX + bw && my in btnY until btnY + bh) {
                closeOverlay(); return true
            }
            // Hex field - let it focus via super
            val fieldY = slAY() + SL_H + 10
            if (mx in slX() until slX() + SL_W && my in fieldY until fieldY + 16) {
                hexDirty = true; return false  // let text field handle it
            }
            // Click outside dialog = cancel
            if (mx !in dx until dx + DLG_W || my !in dy until dy + DLG_H) closeOverlay()
            return true
        }

        fun mouseDragged(mx: Int, my: Int): Boolean {
            when (dragging) {
                0    -> applySpectrum(mx, my)
                1    -> applyVSlider(mx)
                2    -> applyASlider(mx)
                else -> return false
            }
            return true
        }

        private fun applySpectrum(mx: Int, my: Int) {
            val sx = specX(); val sy = specY()
            h = ((mx - sx).toFloat() / SPEC_W * 360f).coerceIn(0f, 360f)
            s = (1f - (my - sy).toFloat() / SPEC_H).coerceIn(0f, 1f)
        }

        private fun applyVSlider(mx: Int) {
            v = ((mx - slX()).toFloat() / SL_W).coerceIn(0f, 1f)
        }

        private fun applyASlider(mx: Int) {
            a = ((mx - slX()).toFloat() / SL_W * 255f).toInt().coerceIn(0, 255)
        }

        private fun commitColor() {
            val rgb = hsvToRgb(h, s, v)
            entry.set(Color((rgb shr 16) and 0xFF, (rgb shr 8) and 0xFF, rgb and 0xFF, a))
            closeOverlay()
        }

        fun closeOverlay() {
            hexField?.let { remove(it) }
            hexField = null
            colorOverlay = null
            syncWidgets()
        }
    }

    // ── List editor overlay ───────────────────────────────────────────────

    private inner class ListOverlay(val entry: ProcessedEntry.MutableListEntry) {
        private val DLG_W    = 280
        private val DLG_H    = 320
        private val ITEM_H   = 26
        private val PAD      = 8
        private val DEL_W    = 20
        private val HANDLE_W = 14  // width of the drag handle column on the left of each row
        private var scroll   = 0
        private var dragIdx     = -1  // index of the row being dragged, -1 = no drag
        private var dragY       = 0   // current mouse Y during drag
        private var dragOffsetY = 0   // offset from mouse to item top when drag started
        private val list     get() = entry.getList()
        private val dlgX     get() = (width  - DLG_W) / 2
        private val dlgY     get() = (height - DLG_H) / 2
        private val lstTop   get() = dlgY + 45
        private val lstBot   get() = dlgY + DLG_H - 32
        private val visH     get() = lstBot - lstTop
        private val maxSc    get() = max(0, list.size * ITEM_H - visH)

        // Effective text field width and x-offset after reserving space for the drag handle
        private fun fieldX() = dlgX + PAD + HANDLE_W
        private fun fieldW() = DLG_W - PAD * 2 - DEL_W - 4 - 24 - HANDLE_W

        /** Slot index (0..list.size-1) the dragged item's center currently hovers over. */
        private fun liveTarget(): Int {
            val center = dragY - dragOffsetY + ITEM_H / 2
            return ((center - lstTop + scroll) / ITEM_H).coerceIn(0, list.size - 1)
        }

        /** Called from [syncWidgets] to add [TextFieldWidget] children for visible STRING/INT rows. */
        fun syncTextFields() {
            if (entry.elementType == ProcessedEntry.MutableListEntry.ElementType.BOOLEAN) return
            if (dragIdx >= 0) return  // text fields removed during drag; restored in endDrag
            val fx = fieldX(); val fw = fieldW()
            for ((i, value) in list.withIndex()) {
                val ry = lstTop + i * ITEM_H - scroll
                if (ry + ITEM_H <= lstTop || ry >= lstBot) continue
                val idx = i
                addDrawableChild(
                    TextFieldWidget(textRenderer, fx, ry + 3, fw, ITEM_H - 6, Text.empty()).also {
                        it.setText(value.toString())
                        it.setChangedListener { newVal ->
                            val parsed: Any = when (entry.elementType) {
                                ProcessedEntry.MutableListEntry.ElementType.INT ->
                                    newVal.toIntOrNull() ?: list.getOrNull(idx) ?: entry.defaultElement
                                else -> newVal
                            }
                            if (idx < list.size) list[idx] = parsed
                        }
                    }
                )
            }
        }

        /** Dim backdrop + dialog shell + header + add button. Called before super.render() so text fields draw on top. */
        fun renderBackground(ctx: DrawContext, mx: Int, my: Int) {
            val dx = dlgX; val dy = dlgY
            ctx.fill(0, 0, width, height, 0x99000000.toInt())
            // Dialog background + border
            ctx.fill(dx, dy, dx + DLG_W, dy + DLG_H, C_MANTLE)
            ctx.fill(dx,             dy,             dx + DLG_W, dy + 1,           C_SURFACE1)
            ctx.fill(dx,             dy + DLG_H - 1, dx + DLG_W, dy + DLG_H,      C_SURFACE1)
            ctx.fill(dx,             dy,             dx + 1,     dy + DLG_H,       C_SURFACE1)
            ctx.fill(dx + DLG_W - 1, dy,             dx + DLG_W, dy + DLG_H,      C_SURFACE1)
            // Title + divider
            ctx.drawCenteredTextWithShadow(textRenderer, "Edit: ${entry.name}", dx + DLG_W / 2, dy + 8, C_TEXT)
            ctx.fill(dx + 1, dy + 22, dx + DLG_W - 1, dy + 23, C_SURFACE0)
            // Add button
            val addHov = mx in dx + PAD until dx + PAD + 54 && my in dy + 26 until dy + 42
            ctx.fill(dx + PAD, dy + 26, dx + PAD + 54, dy + 42, if (addHov) C_SURFACE1 else C_SURFACE0)
            ctx.drawCenteredTextWithShadow(textRenderer, "+ Add", dx + PAD + 27, dy + 30, C_TEXT)
            ctx.fill(dx + 1, lstTop - 1, dx + DLG_W - 1, lstTop, C_SURFACE0)
        }

        /** Item rows + delete buttons + scrollbar + done button. Called after super.render() so text fields appear below row chrome. */
        fun renderForeground(ctx: DrawContext, mx: Int, my: Int) {
            val dx   = dlgX
            val fx   = fieldX(); val fw = fieldW()
            val delX = dx + DLG_W - PAD - DEL_W
            val isBool = entry.elementType == ProcessedEntry.MutableListEntry.ElementType.BOOLEAN

            // Shared row renderer used by both drag and normal paths.
            // When lifted=true (floating drag item), text values are drawn directly
            // because TextFieldWidget children are removed during drag.
            fun drawRow(origIdx: Int, value: Any, ry: Int, lifted: Boolean = false) {
                val handleHov = lifted || (mx in dx + 2 until dx + PAD + HANDLE_W && my in ry until ry + ITEM_H)
                val hColor = if (handleHov) C_SUBTEXT else C_SURFACE1
                val hx = dx + 3
                for (line in 0..2) {
                    val hy = ry + (ITEM_H / 2) - 4 + line * 4
                    ctx.fill(hx, hy, hx + HANDLE_W - 4, hy + 2, hColor)
                }
                val idxLabel = "${origIdx + 1}."
                ctx.drawText(textRenderer, idxLabel, delX - textRenderer.getWidth(idxLabel) - 4,
                    ry + (ITEM_H - textRenderer.fontHeight) / 2, C_OVERLAY0, false)
                if (isBool) {
                    val on     = value as? Boolean ?: false
                    val togHov = !lifted && mx in fx until fx + fw && my in ry + 3 until ry + ITEM_H - 3
                    val bg     = if (on) if (togHov) lighten(C_GREEN) else C_GREEN
                                 else    if (togHov) C_SURFACE1       else C_SURFACE0
                    ctx.fill(fx, ry + 3, fx + fw, ry + ITEM_H - 3, bg)
                    val label  = if (on) "ON" else "OFF"
                    val lw     = textRenderer.getWidth(label)
                    ctx.drawText(textRenderer, label, fx + (fw - lw) / 2,
                        ry + (ITEM_H - textRenderer.fontHeight) / 2, C_TEXT, false)
                } else if (lifted) {
                    // Draw value text directly for the floating item (no text field available)
                    val text = value.toString()
                    ctx.fill(fx, ry + 3, fx + fw, ry + ITEM_H - 3, C_SURFACE0)
                    ctx.drawText(textRenderer, text, fx + 4,
                        ry + (ITEM_H - textRenderer.fontHeight) / 2, C_TEXT, false)
                }
                val delHov = !lifted && mx in delX until delX + DEL_W && my in ry + 3 until ry + ITEM_H - 3
                ctx.fill(delX, ry + 3, delX + DEL_W, ry + ITEM_H - 3, if (delHov) C_RED else C_SURFACE0)
                val xw = textRenderer.getWidth("×")
                ctx.drawText(textRenderer, "×", delX + (DEL_W - xw) / 2,
                    ry + (ITEM_H - textRenderer.fontHeight) / 2, C_TEXT, false)
            }

            if (dragIdx >= 0) {
                // Live reorder: remaining items shift around a ghost slot; dragged item floats
                val target = liveTarget()
                // Build slot -> origIdx mapping (-1 = ghost slot)
                val renderOrder = buildList<Int> {
                    var slot = 0
                    for (origIdx in list.indices) {
                        if (origIdx == dragIdx) continue
                        if (slot == target) add(-1)
                        add(origIdx); slot++
                    }
                    if (-1 !in this) add(-1)  // ghost at end when target == filtered size
                }
                ctx.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
                for ((slot, origIdx) in renderOrder.withIndex()) {
                    val ry = lstTop + slot * ITEM_H - scroll
                    if (ry + ITEM_H <= lstTop || ry >= lstBot) continue
                    if (origIdx == -1) {
                        // Ghost slot: shows where item will land
                        ctx.fill(dx + PAD, ry + 4, dx + DLG_W - PAD, ry + ITEM_H - 4, 0x18FFFFFF)
                        ctx.fill(dx + PAD, ry + 4, dx + DLG_W - PAD, ry + 5, C_BLUE)
                        ctx.fill(dx + PAD, ry + ITEM_H - 5, dx + DLG_W - PAD, ry + ITEM_H - 4, C_BLUE)
                        continue
                    }
                    if (mx in dx until dx + DLG_W && my in ry until ry + ITEM_H)
                        ctx.fill(dx, ry, dx + DLG_W, ry + ITEM_H, 0x0AFFFFFF)
                    drawRow(origIdx, list[origIdx], ry)
                }
                ctx.disableScissor()
                // Dragged item floats above the list, following the cursor
                val floatY = (dragY - dragOffsetY).coerceIn(lstTop - ITEM_H / 2, lstBot - ITEM_H / 2)
                ctx.fill(dx + 1, floatY, dx + DLG_W - 1, floatY + ITEM_H, 0xDD313244.toInt())
                ctx.fill(dx + 1, floatY, dx + DLG_W - 1, floatY + 1, C_BLUE)
                ctx.fill(dx + 1, floatY + ITEM_H - 1, dx + DLG_W - 1, floatY + ITEM_H, C_BLUE)
                drawRow(dragIdx, list[dragIdx], floatY, lifted = true)
            } else {
                // Normal render (no drag)
                ctx.enableScissor(dx, lstTop, dx + DLG_W, lstBot)
                for ((i, value) in list.withIndex()) {
                    val ry = lstTop + i * ITEM_H - scroll
                    if (ry + ITEM_H <= lstTop || ry >= lstBot) continue
                    if (mx in dx until dx + DLG_W && my in ry until ry + ITEM_H)
                        ctx.fill(dx, ry, dx + DLG_W, ry + ITEM_H, 0x0AFFFFFF)
                    drawRow(i, value, ry)
                }
                ctx.disableScissor()
            }
            // Scrollbar
            if (maxSc > 0) {
                val thumbH = (visH * visH.toFloat() / (list.size * ITEM_H)).toInt().coerceAtLeast(16)
                val thumbY = lstTop + ((scroll.toFloat() / maxSc) * (visH - thumbH)).toInt()
                ctx.fill(dx + DLG_W - 3, lstTop, dx + DLG_W - 1, lstBot, 0x22FFFFFF)
                ctx.fill(dx + DLG_W - 3, thumbY, dx + DLG_W - 1, thumbY + thumbH, 0xAABAC2DE.toInt())
            }
            // Done button
            ctx.fill(dx + 1, lstBot, dx + DLG_W - 1, lstBot + 1, C_SURFACE0)
            val bw    = 74; val bh = 16
            val bx    = dx + DLG_W / 2 - bw / 2
            val doneY = lstBot + 8
            val doneHov = mx in bx until bx + bw && my in doneY until doneY + bh
            ctx.fill(bx, doneY, bx + bw, doneY + bh, if (doneHov) C_SURFACE1 else C_SURFACE0)
            ctx.drawCenteredTextWithShadow(textRenderer, "Done", bx + bw / 2, doneY + (bh - textRenderer.fontHeight) / 2, C_TEXT)
        }

        fun mouseClicked(mx: Int, my: Int): Boolean {
            val dx = dlgX; val dy = dlgY
            if (mx !in dx until dx + DLG_W || my !in dy until dy + DLG_H) {
                listOverlay = null; syncWidgets(); return true
            }
            if (mx in dx + PAD until dx + PAD + 54 && my in dy + 26 until dy + 42) {
                list.add(entry.defaultElement)
                scroll = scroll.coerceIn(0, maxSc)
                syncWidgets(); return true
            }
            val bw = 74; val bh = 16
            val bx = dx + DLG_W / 2 - bw / 2
            if (mx in bx until bx + bw && my in lstBot + 8 until lstBot + 8 + bh) {
                listOverlay = null; syncWidgets(); return true
            }
            if (my !in lstTop until lstBot) return true
            val fx = fieldX(); val fw = fieldW()
            for (i in list.indices) {
                val ry   = lstTop + i * ITEM_H - scroll
                if (my !in ry until ry + ITEM_H) continue
                val delX = dx + DLG_W - PAD - DEL_W
                if (mx in delX until delX + DEL_W && my in ry + 3 until ry + ITEM_H - 3) {
                    if (i < list.size) { list.removeAt(i); scroll = scroll.coerceIn(0, maxSc); syncWidgets() }
                    return true
                }
                // Drag handle area - start drag
                if (mx in dx + 2 until dx + PAD + HANDLE_W && my in ry until ry + ITEM_H) {
                    dragIdx = i; dragY = my; dragOffsetY = my - ry; syncWidgets(); return true
                }
                if (entry.elementType == ProcessedEntry.MutableListEntry.ElementType.BOOLEAN &&
                    mx in fx until fx + fw && my in ry + 3 until ry + ITEM_H - 3
                ) {
                    if (i < list.size) list[i] = !(list[i] as? Boolean ?: false)
                    return true
                }
                // For STRING/INT lists return false so the text field child can receive focus
                if (entry.elementType != ProcessedEntry.MutableListEntry.ElementType.BOOLEAN) return false
            }
            return true
        }

        /** Updates the drag target position while dragging. Returns true if currently dragging. */
        fun mouseDragged(my: Int): Boolean {
            if (dragIdx < 0) return false
            dragY = my
            return true
        }

        /** Commits a completed drag: moves the dragged item to the live target position. */
        fun endDrag(): Boolean {
            if (dragIdx < 0) return false
            val target = liveTarget()
            if (target != dragIdx) {
                val item = list.removeAt(dragIdx)
                list.add(target, item)
            }
            dragIdx = -1
            syncWidgets()
            return true
        }

        fun mouseScrolled(vAmt: Double): Boolean {
            scroll = (scroll - (vAmt * ITEM_H).toInt()).coerceIn(0, maxSc)
            syncWidgets()
            return true
        }
    }

    // ── Dropdown overlay ──────────────────────────────────────────────────

    private inner class DropdownOverlay(
        val entry: ProcessedEntry.DropdownEntry,
        val anchorX: Int,
        val anchorBottom: Int,
        val anchorW: Int,
    ) {
        private val ITEM_H  = 20
        private val MAX_VIS = 6
        private val popW    get() = anchorW
        private val popH    get() = minOf(entry.options.size, MAX_VIS) * ITEM_H + 2
        private val maxSc   get() = maxOf(0, entry.options.size * ITEM_H - (popH - 2))
        private var scroll  = run {
            val visContent  = minOf(entry.options.size, MAX_VIS) * ITEM_H
            val idealTop    = entry.get() * ITEM_H - visContent / 2 + ITEM_H / 2
            idealTop.coerceIn(0, maxOf(0, entry.options.size * ITEM_H - visContent))
        }

        private fun popX() = anchorX
        private fun popY(): Int {
            val below = anchorBottom
            return if (below + popH <= height - BOT_H) below
                   else anchorBottom - (ROW_H - 6) - popH
        }

        fun render(ctx: DrawContext, mx: Int, my: Int) {
            val px = popX(); val py = popY()
            // Background + border
            ctx.fill(px,            py,            px + popW, py + popH,     C_MANTLE)
            ctx.fill(px,            py,            px + popW, py + 1,         C_SURFACE1)
            ctx.fill(px,            py + popH - 1, px + popW, py + popH,     C_SURFACE1)
            ctx.fill(px,            py,            px + 1,    py + popH,     C_SURFACE1)
            ctx.fill(px + popW - 1, py,            px + popW, py + popH,     C_SURFACE1)

            ctx.enableScissor(px + 1, py + 1, px + popW - 1, py + popH - 1)
            for (i in entry.options.indices) {
                val iy = py + 1 + i * ITEM_H - scroll
                if (iy + ITEM_H <= py + 1 || iy >= py + popH - 1) continue
                val isHov = mx in px + 1 until px + popW - 1 && my in iy until iy + ITEM_H
                val isSel = i == entry.get()
                when {
                    isHov -> ctx.fill(px + 1, iy, px + popW - 1, iy + ITEM_H, C_SURFACE1)
                    isSel -> ctx.fill(px + 1, iy, px + popW - 1, iy + ITEM_H, C_SURFACE0)
                }
                val col   = if (isSel) C_BLUE else C_TEXT
                val label = entry.options[i]
                val lw    = textRenderer.getWidth(label)
                ctx.drawText(textRenderer, label, px + (popW - lw) / 2, iy + (ITEM_H - textRenderer.fontHeight) / 2, col, false)
            }
            ctx.disableScissor()

            // Scrollbar
            if (maxSc > 0) {
                val visH   = popH - 2
                val thumbH = (visH * visH.toFloat() / (entry.options.size * ITEM_H)).toInt().coerceAtLeast(8)
                val thumbY = py + 1 + ((scroll.toFloat() / maxSc) * (visH - thumbH)).toInt()
                ctx.fill(px + popW - 3, py + 1,  px + popW - 1, py + popH - 1, 0x22FFFFFF)
                ctx.fill(px + popW - 3, thumbY,  px + popW - 1, thumbY + thumbH, 0xAABAC2DE.toInt())
            }
        }

        fun mouseClicked(mx: Int, my: Int): Boolean {
            val px = popX(); val py = popY()
            if (mx in px until px + popW && my in py until py + popH) {
                val i = (my - py - 1 + scroll) / ITEM_H
                if (i in entry.options.indices) { entry.set(i); dropdownOverlay = null }
                return true
            }
            dropdownOverlay = null
            return true
        }

        fun mouseScrolled(vAmt: Double): Boolean {
            scroll = (scroll - (vAmt * ITEM_H).toInt()).coerceIn(0, maxSc)
            return true
        }
    }

    // ── Input ─────────────────────────────────────────────────────────────

    override fun mouseScrolled(mouseX: Double, mouseY: Double, hAmt: Double, vAmt: Double): Boolean {
        listOverlay?.let { return it.mouseScrolled(vAmt) }
        dropdownOverlay?.let { return it.mouseScrolled(vAmt) }
        if (colorOverlay != null) return true
        if (mouseX >= entryLeft) {
            scroll = (scroll - (vAmt * ROW_H).toInt()).coerceIn(0, maxScroll)
            syncWidgets(); return true
        }
        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean {
        val mx = click.x().toInt(); val my = click.y().toInt(); val btn = click.button()

        // Overlays intercept all input while open (list overlay may return false to let text fields focus)
        listOverlay?.let { lo ->
            if (lo.mouseClicked(mx, my)) return true
            return super.mouseClicked(click, doubled)
        }
        dropdownOverlay?.let { return it.mouseClicked(mx, my) }
        colorOverlay?.let { return it.mouseClicked(mx, my) }

        // Done button
        val bw = 80; val bh = 20
        val bx = width / 2 - bw / 2; val by = height - BOT_H + (BOT_H - bh) / 2
        if (mx in bx until bx + bw && my in by until by + bh) {
            manager.save(); client?.setScreen(parent); return true
        }

        // Category panel - also clears search
        if (mx < CAT_W) {
            for (item in buildSidebarItems()) {
                if (my in item.y until item.y + item.h) {
                    selCat = item.catIdx; selSubCat = item.subCatIdx
                    scroll = 0; searchText = ""
                    searchField?.setText(""); rebuildRows(); syncWidgets()
                    dropdownOverlay = null
                    break
                }
            }
            return true
        }

        if (super.mouseClicked(click, doubled)) return true

        if (mx !in entryLeft until entryRight || my !in entryTop until entryBottom) return false

        for ((rowIdx, row) in rows.withIndex()) {
            val ry = entryTop + rowIdx * ROW_H - scroll
            if (my !in ry until ry + ROW_H) continue
            if (ry + ROW_H <= entryTop || ry >= entryBottom) continue
            return when (row) {
                is Row.ColHeader -> {
                    if (searchText.isBlank()) { row.group.collapsed = !row.group.collapsed; rebuildRows(); syncWidgets() }
                    true
                }
                is Row.EntryRow -> handleEntryClick(row.entry, mx, ry + 3, btn)
                else -> false
            }
        }
        return false
    }

    private fun handleEntryClick(entry: ProcessedEntry, mx: Int, wy: Int, btn: Int): Boolean {
        val wx = widgetX(); val ww = widgetW()
        if (mx !in wx until wx + ww) return false
        when (entry) {
            is ProcessedEntry.BoolEntry     -> entry.set(!entry.get())
            is ProcessedEntry.SliderEntry   -> {
                // Only start slider drag when clicking the track portion (not the input field)
                val inputX = wx + ww - SLIDER_INP
                if (mx < inputX) { draggingSlider = entry; applySliderX(entry, mx) }
            }
            is ProcessedEntry.DropdownEntry -> {
                if (dropdownOverlay?.entry === entry) dropdownOverlay = null
                else dropdownOverlay = DropdownOverlay(entry, wx, wy + ROW_H - 3, ww)
            }
            is ProcessedEntry.ButtonEntry      -> entry.action()
            is ProcessedEntry.ColorEntry       -> {
                colorOverlay = ColorOverlay(entry)
                colorOverlay!!.initHexField()
            }
            is ProcessedEntry.KeybindEntry     -> capturingKeybind = entry
            is ProcessedEntry.MutableListEntry -> { listOverlay = ListOverlay(entry); syncWidgets() }
            else -> return false
        }
        return true
    }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean {
        listOverlay?.let { if (it.mouseDragged(click.y().toInt())) return true }
        colorOverlay?.let { return it.mouseDragged(click.x().toInt(), click.y().toInt()) }
        val drag = draggingSlider
        if (drag != null) { applySliderX(drag, click.x().toInt()); return true }
        return super.mouseDragged(click, deltaX, deltaY)
    }

    override fun mouseReleased(click: Click): Boolean {
        listOverlay?.endDrag()
        colorOverlay?.dragging = -1
        if (draggingSlider != null) {
            draggingSlider = null
            syncWidgets()  // refresh slider text fields with final value
        }
        return super.mouseReleased(click)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        // Overlays eat ESC
        if (listOverlay != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) { listOverlay = null; syncWidgets(); return true }
            return true
        }
        if (dropdownOverlay != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) { dropdownOverlay = null; return true }
            return true
        }
        if (colorOverlay != null) {
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                colorOverlay?.closeOverlay()
                return true
            }
            return true
        }
        val capturing = capturingKeybind
        if (capturing != null) {
            val keyCode = input.key()
            // Ignore bare modifier presses - wait for a real key
            if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL  || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
                keyCode == GLFW.GLFW_KEY_LEFT_SHIFT     || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT   ||
                keyCode == GLFW.GLFW_KEY_LEFT_ALT       || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
            ) return true
            val mods    = input.modifiers()
            val ctrl    = (mods and GLFW.GLFW_MOD_CONTROL) != 0
            val shift   = (mods and GLFW.GLFW_MOD_SHIFT)   != 0
            val alt     = (mods and GLFW.GLFW_MOD_ALT)     != 0
            val packed  = KeybindPacked.pack(keyCode, com.github.mikecraft1224.input.api.Modifiers(ctrl, shift, alt))
            capturing.set(packed)
            capturing.onChanged?.invoke(packed)
            capturingKeybind = null
            return true
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) { manager.save(); client?.setScreen(parent); return true }
        return super.keyPressed(input)
    }

    override fun shouldCloseOnEsc() = false

    fun refresh() { rebuildRows(); syncWidgets() }

    private fun sliderTrackW() = widgetW() - SLIDER_INP - 4

    private fun applySliderX(e: ProcessedEntry.SliderEntry, mx: Int) {
        val ratio   = ((mx - widgetX()).toDouble() / sliderTrackW()).coerceIn(0.0, 1.0)
        val stepped = (round((e.min + ratio * (e.max - e.min)) / e.step) * e.step).coerceIn(e.min, e.max)
        e.set(stepped)
    }

    private fun lighten(color: Int): Int {
        val r = ((color shr 16) and 0xFF).let { it + (255 - it) / 5 }.coerceAtMost(255)
        val g = ((color shr  8) and 0xFF).let { it + (255 - it) / 5 }.coerceAtMost(255)
        val b = ( color         and 0xFF).let { it + (255 - it) / 5 }.coerceAtMost(255)
        return (color and 0xFF000000.toInt()) or (r shl 16) or (g shl 8) or b
    }

    // ── HSV <-> RGB helpers ───────────────────────────────────────────────

    /** Converts HSV (h in 0..360, s/v in 0..1) to packed ARGB with full alpha. */
    private fun hsvToArgb(h: Float, s: Float, v: Float, a: Int): Int {
        val rgb = hsvToRgb(h, s, v)
        return (a shl 24) or (rgb and 0x00FFFFFF)
    }

    /** Converts HSV (h in 0..360, s/v in 0..1) to packed 0xRRGGBB. */
    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val i  = (hh / 60f).toInt()
        val f  = hh / 60f - i
        val p  = v * (1f - s)
        val q  = v * (1f - s * f)
        val t  = v * (1f - s * (1f - f))
        val (r, g, b) = when (i % 6) {
            0    -> Triple(v, t, p)
            1    -> Triple(q, v, p)
            2    -> Triple(p, v, t)
            3    -> Triple(p, q, v)
            4    -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }

    /** Converts packed 0xRRGGBB to FloatArray(h, s, v) where h in 0..360, s/v in 0..1. */
    private fun rgbToHsv(rgb: Int): FloatArray {
        val r = ((rgb shr 16) and 0xFF) / 255f
        val g = ((rgb shr  8) and 0xFF) / 255f
        val b =  (rgb         and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d   = max - min
        val v   = max
        val s   = if (max == 0f) 0f else d / max
        val h   = when {
            d == 0f  -> 0f
            max == r -> 60f * ((g - b) / d % 6f)
            max == g -> 60f * ((b - r) / d + 2f)
            else     -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return floatArrayOf(h, s, v)
    }
}
