package com.github.mikecraft1224.simplecore.config.screen

import com.github.mikecraft1224.simplecore.config.ConfigManager
import com.github.mikecraft1224.simplecore.config.KeybindPacked
import com.github.mikecraft1224.simplecore.config.ProcessedConfig
import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.BOT_H
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_BLUE
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_BASE
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_GREEN
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_MANTLE
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_OVERLAY0
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_RED
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SUBTEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE0
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_SURFACE1
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.C_TEXT
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.CAT_W
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.ROW_H
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.SEARCH_H
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.SLIDER_INP
import com.github.mikecraft1224.simplecore.config.screen.ConfigLayout.lighten
import com.github.mikecraft1224.simplecore.config.screen.overlays.ColorPickerOverlay
import com.github.mikecraft1224.simplecore.config.screen.overlays.DropdownOverlay
import com.github.mikecraft1224.simplecore.config.screen.overlays.ListOverlay
import com.github.mikecraft1224.simplecore.config.screen.overlays.MultiSelectOverlay
import com.github.mikecraft1224.simplecore.config.screen.overlays.ObjectListOverlay
import com.github.mikecraft1224.simplecore.utils.Color as SimpleColor
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import com.github.mikecraft1224.simplecore.input.api.Modifiers
import net.minecraft.network.chat.Component
import org.lwjgl.glfw.GLFW
import java.util.IdentityHashMap
import kotlin.math.max
import kotlin.math.round

/**
 * Two-panel config screen built from a [ProcessedConfig].
 *
 * Left: category list. Top-right: title bar + search. Right: scrollable entry rows.
 *
 * Overlay management is handled by [OverlayStack] - all overlays (dropdown, color picker,
 * list editors) are pushed as [OverlayLayer] instances onto the stack and receive events
 * through [ConfigOverlay] callbacks. See [OverlayStack], [ConfigLayout], and the overlay
 * classes in `overlays/` for implementation details.
 */
@Suppress("Unused")
class ConfigScreen(
    private val parent: Screen?,
    private val model: ProcessedConfig,
    private val manager: ConfigManager<*>,
    title: String = "Config",
) : Screen(Component.literal("Config")) {

    private val displayTitle    = model.title.ifEmpty { title }
    private val displaySubtitle = model.subtitle

    /** Resolved accent color - falls back to C_BLUE when model.accentColor is 0. */
    private val accentColor get() = if (model.accentColor != 0) model.accentColor else C_BLUE

    // -- Layout helpers -------------------------------------------------------

    private val headerH          get() = if (displaySubtitle.isEmpty()) 22 else 34
    private val effectiveSearchH get() = if (model.searchEnabled) SEARCH_H else 0

    private val entryLeft   get() = CAT_W
    private val entryRight  get() = width
    private val entryTop    get() = headerH + effectiveSearchH
    private val entryBottom get() = height - BOT_H
    private val entryW      get() = entryRight - entryLeft
    private val visH        get() = entryBottom - entryTop
    private fun widgetX()   = entryLeft + (entryW * 0.65f).toInt()
    private fun widgetW()   = (entryW * 0.28f).toInt()

    // -- State ----------------------------------------------------------------

    private var selCat           = resolveDefaultCat()
    private var selSubCat        = -1
    private var scroll           = 0
    private var searchText       = ""
    private var searchField: EditBox? = null
    private var capturingKeybind: ProcessedEntry.KeybindEntry? = null
    private var draggingSlider:   ProcessedEntry.SliderEntry?  = null
    private var sliderDragTrackX: Int = 0
    private var sliderDragTrackW: Int = 0
    private var pendingTooltip:   Pair<String, Pair<Int, Int>>? = null

    /** Single overlay stack replacing all individual overlay fields. */
    private val overlayStack = OverlayStack()

    /** ScTextField instances for TextEntry and SliderEntry rows - keyed by entry identity. */
    private val scTextFields = IdentityHashMap<ProcessedEntry, ScTextField>()

    private val anyOverlayOpen get() = overlayStack.isOpen

    /** Context object passed to overlay classes. Initialised lazily so [font] is ready. */
    private val ctx: ConfigScreenCtx by lazy {
        ConfigScreenCtx(
            tr              = font,
            getW            = { width },
            getH            = { height },
            scFields        = scTextFields,
            accentColor     = { accentColor },
            startSliderDrag = { e, mx, trackX, trackW -> startSliderDragImpl(e, mx, trackX, trackW) },
            captureKeybind  = { e -> capturingKeybind = e },
            drawWidget      = { drawState, entry, x, y, w, h, mx, my ->
                drawEntryWidget(drawState, entry, x, y, w, h, mx, my)
            },
        )
    }

    private val selCategory  get() = model.categories.getOrNull(selCat)
    private val selEntries   get() = if (selSubCat >= 0)
        selCategory?.subcategories?.getOrNull(selSubCat)?.entries ?: emptyList()
    else
        selCategory?.entries ?: emptyList()

    private fun resolveDefaultCat(): Int {
        val name = model.defaultCategory
        if (name.isEmpty()) return 0
        return model.categories.indexOfFirst { it.name == name }.takeIf { it >= 0 } ?: 0
    }

    // -- Sidebar items --------------------------------------------------------

    private data class SidebarItem(
        val catIdx:    Int,
        val subCatIdx: Int,
        val name:      String,
        val y:         Int,
        val h:         Int,
    )

    private fun buildSidebarItems(): List<SidebarItem> = buildList {
        var y = headerH + 10
        for ((i, cat) in model.categories.withIndex()) {
            if (!cat.condition()) continue
            add(SidebarItem(i, -1, cat.name, y, 22))
            y += 22
            if (i == selCat && cat.subcategories.isNotEmpty()) {
                for ((j, sub) in cat.subcategories.withIndex()) {
                    if (!sub.condition()) continue
                    add(SidebarItem(i, j, sub.name, y, 20))
                    y += 20
                }
            }
        }
    }

    // -- Row model ------------------------------------------------------------

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
            !e.visible()                         -> {}
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

    private fun buildSearchRows(query: String): List<Row> = buildList {
        fun matches(e: ProcessedEntry) =
            e.name.contains(query, ignoreCase = true) ||
            e.searchTags.any { it.contains(query, ignoreCase = true) }
        for (cat in model.categories) {
            if (!cat.condition()) continue
            val catNameMatch = cat.name.contains(query, ignoreCase = true)
            val directMatching = flattenEntries(cat.entries).filter { it.visible() && (catNameMatch || matches(it)) }
            if (directMatching.isNotEmpty()) {
                add(Row.SearchHeader(cat.name))
                directMatching.forEach { add(Row.EntryRow(it, 1)) }
            }
            for (sub in cat.subcategories) {
                if (!sub.condition()) continue
                val subNameMatch = catNameMatch || sub.name.contains(query, ignoreCase = true)
                val subMatching = flattenEntries(sub.entries).filter { it.visible() && (subNameMatch || matches(it)) }
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

    private fun pruneInvalidDropdownSelections() {
        var changed = false
        for (cat in model.categories) {
            for (e in flattenEntries(cat.entries) + cat.subcategories.flatMap { flattenEntries(it.entries) }) {
                if (e !is ProcessedEntry.MutableListEntry) continue
                if (e.elementType != ProcessedEntry.MutableListEntry.ElementType.DROPDOWN) continue
                val opts = e.dropdownOptions?.invoke() ?: continue
                if (opts.isEmpty()) continue
                val list = e.getList()
                val maxIdx = opts.lastIndex
                val before = list.size
                list.removeIf { (it as? Int ?: 0) > maxIdx }
                if (list.size != before) changed = true
            }
        }
        if (changed) syncWidgets()
    }

    private fun rebuildRowsLive() {
        pruneInvalidDropdownSelections()
        val curCat = model.categories.getOrNull(selCat)
        if (curCat == null || !curCat.condition()) {
            selCat    = model.categories.indexOfFirst { it.condition() }.coerceAtLeast(0)
            selSubCat = -1
        } else if (selSubCat >= 0) {
            val sub = curCat.subcategories.getOrNull(selSubCat)
            if (sub == null || !sub.condition()) selSubCat = -1
        }
        val oldRows = rows
        rebuildRows()
        if (rows != oldRows) syncWidgets()
    }

    // -- Screen lifecycle -------------------------------------------------------

    override fun init() {
        overlayStack.clear()
        super.init()

        if (model.searchEnabled) {
            if (searchField == null) {
                searchField = EditBox(
                    font, entryLeft + 6, headerH + 4, entryW - 12, 18, Component.empty(),
                ).also {
                    it.setResponder { v -> searchText = v; rebuildRows(); syncWidgets() }
                }
            } else {
                searchField!!.apply {
                    x = entryLeft + 6; y = headerH + 4; width = entryW - 12
                }
            }
            searchField?.let { addRenderableWidget(it) }
        }

        if (model.autoFocusSearch && model.searchEnabled) {
            searchField?.setFocused(true)
        }

        rebuildRows()
        syncWidgets()
    }

    private fun syncWidgets() {
        // Overlays manage their own fields; no per-frame init needed here.
    }

    // -- Rendering ------------------------------------------------------------

    override fun extractBackground(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        rebuildRowsLive()
        super.extractBackground(context, mouseX, mouseY, delta)

        // Panel fills
        context.fill(0, 0, CAT_W, height, C_MANTLE)
        context.fill(CAT_W, 0, width, height, C_BASE)

        // Header bar
        context.fill(0, 0, width, headerH, C_MANTLE)
        context.fill(0, headerH - 1, width, headerH, C_SURFACE0)
        if (displaySubtitle.isEmpty()) {
            context.centeredText(font, displayTitle, width / 2, (headerH - font.lineHeight) / 2, C_TEXT)
        } else {
            context.centeredText(font, displayTitle,    width / 2, 7,  C_TEXT)
            context.centeredText(font, displaySubtitle, width / 2, 20, C_OVERLAY0)
        }

        // Dividers
        context.fill(CAT_W - 1, headerH,        CAT_W, height,             C_SURFACE0)
        context.fill(CAT_W,     entryTop - 1,   width, entryTop,           C_SURFACE0)
        context.fill(0,         height - BOT_H, width, height - BOT_H + 1, C_SURFACE0)

        // Search placeholder
        if (model.searchEnabled && searchText.isEmpty()) {
            context.text(font, "Search...", entryLeft + 8, headerH + 8, C_OVERLAY0, false)
        }

        // Category sidebar
        for (item in buildSidebarItems()) {
            val cat = model.categories[item.catIdx]
            val isParent   = item.subCatIdx < 0
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
            context.text(font, "$prefix${item.name}", indentX, item.y + (item.h - font.lineHeight) / 2, col, false)
        }

        // Entry rows (scissored)
        pendingTooltip = null
        context.enableScissor(entryLeft, entryTop, entryRight, entryBottom)

        for ((rowIdx, row) in rows.withIndex()) {
            val ry  = entryTop + rowIdx * ROW_H - scroll
            if (ry + ROW_H <= entryTop || ry >= entryBottom) continue

            val hov = mouseX in entryLeft until entryRight && mouseY in ry until ry + ROW_H
            if (hov && !anyOverlayOpen && row !is Row.SearchHeader && row !is Row.ColHeader && row !is Row.Sep)
                context.fill(entryLeft, ry, entryRight - 4, ry + ROW_H, 0x0AFFFFFF)

            when (row) {
                is Row.Sep -> {
                    val ly = ry + ROW_H / 2
                    val label = row.sep.label
                    if (label.isEmpty()) {
                        context.fill(entryLeft + 8, ly, entryRight - 8, ly + 1, 0x44CDD6F4)
                    } else {
                        val lw    = font.width(label)
                        val cx    = (entryLeft + entryRight) / 2
                        val gap   = 6
                        context.fill(entryLeft + 8,    ly, cx - lw / 2 - gap, ly + 1, 0x44CDD6F4)
                        context.fill(cx + lw / 2 + gap, ly, entryRight - 8,    ly + 1, 0x44CDD6F4)
                        context.centeredText(font, label, cx, ly - font.lineHeight / 2, C_OVERLAY0)
                    }
                }
                is Row.SearchHeader -> {
                    context.fill(entryLeft, ry, entryRight, ry + ROW_H, 0x22FFFFFF)
                    context.text(
                        font, row.name.uppercase(),
                        entryLeft + 8, ry + (ROW_H - font.lineHeight) / 2,
                        accentColor, false,
                    )
                }
                is Row.ColHeader -> {
                    val indentX = row.indent * 10
                    context.fill(entryLeft, ry, entryRight - 4, ry + ROW_H, if (hov) C_SURFACE1 else C_SURFACE0)
                    for (level in 0 until row.indent) {
                        val gx = entryLeft + level * 10 + 1
                        context.fill(gx, ry, gx + 1, ry + ROW_H, 0x22FFFFFF)
                    }
                    val barColor = if (row.indent == 0) accentColor else C_OVERLAY0
                    context.fill(entryLeft + indentX, ry + 1, entryLeft + indentX + 3, ry + ROW_H - 1, barColor)
                    val tx  = entryLeft + indentX + 8
                    val ty  = ry + (ROW_H - font.lineHeight) / 2
                    val arrow = if (row.group.collapsed) "+" else "-"
                    context.text(font, "$arrow  ${row.group.name}", tx, ty, C_TEXT, true)
                    if (!row.group.collapsed)
                        context.fill(entryLeft + indentX + 3, ry + ROW_H - 1, entryRight - 4, ry + ROW_H, 0x33FFFFFF)
                    if (row.group.description.isNotEmpty() && hov)
                        pendingTooltip = row.group.description to (mouseX to mouseY)
                }
                is Row.EntryRow -> {
                    val e  = row.entry
                    val tx = entryLeft + 8 + row.indent * 10
                    val ty = ry + (ROW_H - font.lineHeight) / 2
                    for (level in 0 until row.indent) {
                        val gx = entryLeft + level * 10 + 1
                        context.fill(gx, ry, gx + 1, ry + ROW_H, 0x22FFFFFF)
                    }
                    if (e is ProcessedEntry.InfoEntry) {
                        context.text(font, e.name, tx, ty, C_SUBTEXT, false)
                        context.text(font, e.getText(), widgetX(), ty, accentColor, false)
                    } else {
                        context.text(font, e.name, tx, ty, C_TEXT, false)
                        if (e.description.isNotEmpty() && hov)
                            pendingTooltip = e.description to (mouseX to mouseY)
                        val effMx = if (anyOverlayOpen) -1 else mouseX
                        val effMy = if (anyOverlayOpen) -1 else mouseY
                        if (e is ProcessedEntry.TextEntry) {
                            val wx = widgetX(); val ww = widgetW()
                            val tf = scTextFields.getOrPut(e) {
                                ScTextField(wx, ry + 3, ww, ROW_H - 6, e.get()).also { it.onChange = { v -> e.set(v) } }
                            }.apply { x = wx; y = ry + 3 }
                            if (anyOverlayOpen) tf.focused = false
                            tf.render(context, effMx, effMy)
                        } else {
                            drawEntryWidget(context, e, widgetX(), ry + 3, widgetW(), ROW_H - 6, effMx, effMy)
                        }
                    }
                }
            }
        }
        context.disableScissor()

        // When any overlay is open, render the search field manually here so it appears behind the
        // dim layer drawn by the overlay. The drawable-child pass is suppressed in extractRenderState below.
        if (anyOverlayOpen && model.searchEnabled) {
            searchField?.extractRenderState(context, mouseX, mouseY, delta)
        }
    }

    override fun extractRenderState(state: GuiGraphicsExtractor, mx: Int, my: Int, delta: Float) {
        // Suppress the drawable-child pass of searchField when overlays are open so it doesn't
        // double-draw on top of the dim (it was already drawn in extractBackground above).
        if (anyOverlayOpen) searchField?.visible = false
        super.extractRenderState(state, mx, my, delta)
        if (anyOverlayOpen) searchField?.visible = true

        // Scrollbar
        if (maxScroll > 0) {
            val thumbH = (visH * visH.toFloat() / (rows.size * ROW_H)).toInt().coerceAtLeast(16)
            val thumbY = entryTop + ((scroll.toFloat() / maxScroll) * (visH - thumbH)).toInt()
            state.fill(entryRight - 3, entryTop,  entryRight, entryBottom, 0x22FFFFFF)
            state.fill(entryRight - 3, thumbY, entryRight, thumbY + thumbH, 0xAABAC2DE.toInt())
        }

        // Done button - disabled while any overlay is open
        val bw = 80; val bh = 20
        val bx = width / 2 - bw / 2
        val by = height - BOT_H + (BOT_H - bh) / 2
        val doneHov = !anyOverlayOpen && mx in bx until bx + bw && my in by until by + bh
        state.fill(bx, by, bx + bw, by + bh, if (doneHov) C_SURFACE1 else C_SURFACE0)
        state.centeredText(font, "Done", bx + bw / 2, by + (bh - font.lineHeight) / 2,
            if (anyOverlayOpen) C_OVERLAY0 else C_TEXT)

        // Tooltip (suppressed when any overlay is open)
        if (!anyOverlayOpen)
            pendingTooltip?.let { (msg, pos) -> state.setTooltipForNextFrame(Component.literal(msg), pos.first, pos.second) }

        // Overlays - rendered last, on top of everything
        overlayStack.render(state, mx, my)
    }

    // -- Widget drawing -------------------------------------------------------

    private fun drawEntryWidget(state: GuiGraphicsExtractor, entry: ProcessedEntry, x: Int, y: Int, w: Int, h: Int, mx: Int, my: Int) {
        val hov = mx in x until x + w && my in y until y + h
        when (entry) {
            is ProcessedEntry.BoolEntry        -> drawToggle(state, entry.get(), x, y, w, h, hov)
            is ProcessedEntry.SliderEntry      -> drawSlider(state, entry, x, y, w, h, hov, mx, my)
            is ProcessedEntry.DropdownEntry    -> drawDropdown(state, entry, x, y, w, h, hov)
            is ProcessedEntry.ButtonEntry      -> drawBox(state, entry.buttonText, x, y, w, h, hov)
            is ProcessedEntry.ColorEntry       -> drawColorSwatch(state, SimpleColor.fromAwtColor(entry.get()), x, y, w, h, hov)
            is ProcessedEntry.KeybindEntry     -> drawKeybind(state, entry, x, y, w, h, hov, mx)
            is ProcessedEntry.MutableListEntry  -> drawBox(state, "Edit (${entry.getList().size} items)", x, y, w, h, hov)
            is ProcessedEntry.ObjectListEntry   -> drawBox(state, "Edit (${entry.getList().size} items)", x, y, w, h, hov)
            is ProcessedEntry.MultiSelectEntry  -> {
                val sel = entry.getSelected()
                val label = when (sel.size) {
                    0    -> "None"
                    1    -> sel[0]
                    else -> "${sel.size} selected"
                }
                drawBox(state, label, x, y, w, h, hov)
            }
            else -> {}
        }
    }

    private fun drawToggle(state: GuiGraphicsExtractor, on: Boolean, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        val trackW = 34; val trackH = 14; val knobSz = 10; val pad = 2
        val tx = x + w - trackW; val ty = y + (h - trackH) / 2
        val trackBg = when {
            on && hov  -> lighten(C_GREEN)
            on         -> C_GREEN
            hov        -> C_SURFACE1
            else       -> C_SURFACE0
        }
        state.fill(tx + 2, ty,               tx + trackW - 2, ty + 2,              trackBg)
        state.fill(tx,     ty + 2,           tx + trackW,     ty + trackH - 2,     trackBg)
        state.fill(tx + 2, ty + trackH - 2,  tx + trackW - 2, ty + trackH,         trackBg)
        val knobX = if (on) tx + trackW - knobSz - pad else tx + pad
        val knobY = ty + (trackH - knobSz) / 2
        state.fill(knobX,              knobY,               knobX + knobSz, knobY + knobSz, C_TEXT)
        state.fill(knobX,              knobY,               knobX + 1,      knobY + 1,      trackBg)
        state.fill(knobX + knobSz - 1, knobY,               knobX + knobSz, knobY + 1,      trackBg)
        state.fill(knobX,              knobY + knobSz - 1,  knobX + 1,      knobY + knobSz, trackBg)
        state.fill(knobX + knobSz - 1, knobY + knobSz - 1,  knobX + knobSz, knobY + knobSz, trackBg)
    }

    private fun drawSlider(state: GuiGraphicsExtractor, e: ProcessedEntry.SliderEntry, x: Int, y: Int, w: Int, h: Int, hov: Boolean, mx: Int = -1, my: Int = -1) {
        val sliderW = w - SLIDER_INP - 4
        val ratio   = ((e.get() - e.min) / (e.max - e.min)).coerceIn(0.0, 1.0)
        val fillW   = (sliderW * ratio).toInt()
        val midY    = y + h / 2
        state.fill(x, midY - 2, x + sliderW, midY + 2, C_SURFACE0)
        state.fill(x, midY - 2, x + fillW,   midY + 2, accentColor)
        val tx = x + fillW
        state.fill(tx - 3, y + 2, tx + 3, y + h - 2, if (hov || draggingSlider === e) C_TEXT else C_SUBTEXT)

        val inpX = x + w - SLIDER_INP
        val dec  = if (e.step >= 1.0) 0 else e.step.toString().substringAfter('.').trimEnd('0').length
        val tf   = scTextFields.getOrPut(e) {
            ScTextField(inpX, y + 1, SLIDER_INP, h - 2, "%.${dec}f".format(e.get())).also { f ->
                f.onChange = { v ->
                    val parsed = v.toDoubleOrNull()
                    if (parsed != null) {
                        val stepped = (round(parsed / e.step) * e.step).coerceIn(e.min, e.max)
                        e.set(stepped)
                    }
                }
            }
        }.apply { this.x = inpX; this.y = y + 1 }
        if (!tf.focused) tf.setText("%.${dec}f".format(e.get()))
        if (mx < 0) tf.focused = false
        tf.render(state, mx, my)
    }

    private fun drawDropdown(state: GuiGraphicsExtractor, e: ProcessedEntry.DropdownEntry, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        val isOpen = overlayStack.topLayer?.overlays
            ?.filterIsInstance<DropdownOverlay>()
            ?.any { it.entry === e } == true
        state.fill(x, y, x + w, y + h, if (isOpen || hov) C_SURFACE1 else C_SURFACE0)
        val opts  = e.options()
        val label = opts.getOrElse(e.get().coerceIn(0, opts.lastIndex)) { "?" }
        val lw    = font.width(label)
        val ty    = y + (h - font.lineHeight) / 2
        state.text(font, label, x + (w - lw) / 2, ty, C_TEXT, false)
        val arrow = if (isOpen) "▲" else "▼"
        state.text(font, arrow, x + w - font.width(arrow) - 4, ty, C_OVERLAY0, false)
    }

    private fun drawBox(state: GuiGraphicsExtractor, label: String, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        state.fill(x, y, x + w, y + h, if (hov) C_SURFACE1 else C_SURFACE0)
        val lw = font.width(label)
        state.text(font, label, x + (w - lw) / 2, y + (h - font.lineHeight) / 2, C_TEXT, false)
    }

    private fun drawColorSwatch(state: GuiGraphicsExtractor, color: SimpleColor, x: Int, y: Int, w: Int, h: Int, hov: Boolean) {
        val argb = color.argb
        state.fill(x, y + 1, x + w, y + h - 1, 0xFF888888.toInt())
        state.fill(x,         y + 1,     x + w / 2, y + h / 2, 0xFFAAAAAA.toInt())
        state.fill(x + w / 2, y + h / 2, x + w,     y + h - 1, 0xFFAAAAAA.toInt())
        state.fill(x, y + 1, x + w, y + h - 1, argb)
        val borderCol = if (hov) C_SUBTEXT else C_SURFACE1
        state.fill(x - 1, y,         x + w + 1, y + 1,     borderCol)
        state.fill(x - 1, y + h,     x + w + 1, y + h + 1, borderCol)
        state.fill(x - 1, y,         x,         y + h + 1, borderCol)
        state.fill(x + w, y,         x + w + 1, y + h + 1, borderCol)
        if (hov) {
            val hint = "click to edit"
            val hw   = font.width(hint)
            if (hw + 4 <= w)
                state.text(font, hint, x + (w - hw) / 2, y + (h - font.lineHeight) / 2, 0xAAFFFFFF.toInt(), false)
        }
    }

    private fun drawKeybind(state: GuiGraphicsExtractor, e: ProcessedEntry.KeybindEntry, x: Int, y: Int, w: Int, h: Int, hov: Boolean, mx: Int = -1) {
        val capturing = capturingKeybind === e
        val canReset  = e.defaultPacked != 0 && e.get() != e.defaultPacked && !capturing
        val resetW    = if (canReset) 16 else 0
        val mainW     = w - resetW

        state.fill(x, y, x + mainW, y + h, when { capturing -> C_RED; hov -> C_SURFACE1; else -> C_SURFACE0 })

        val label = if (capturing) {
            "Press key or click..."
        } else {
            val packed  = e.get()
            val keyCode = KeybindPacked.keyCode(packed)
            val mods    = KeybindPacked.modifiers(packed)
            val keyName = if (KeybindPacked.isMouse(packed))
                InputConstants.Type.MOUSE.getOrCreate(keyCode).displayName.string
            else
                InputConstants.Type.KEYSYM.getOrCreate(keyCode).displayName.string
            buildString {
                if (mods.ctrl)  append("Ctrl+")
                if (mods.shift) append("Shift+")
                if (mods.alt)   append("Alt+")
                append(keyName)
            }
        }
        val lw = font.width(label)
        state.text(font, label, x + (mainW - lw) / 2, y + (h - font.lineHeight) / 2, C_TEXT, false)

        if (canReset) {
            val rx = x + mainW
            val resetHov = hov && mx in rx until rx + resetW
            state.fill(rx, y, rx + resetW, y + h, if (resetHov) C_SURFACE1 else C_SURFACE0)
            val rw = font.width("↺")
            state.text(font, "↺", rx + (resetW - rw) / 2, y + (h - font.lineHeight) / 2, C_OVERLAY0, false)
        }
    }

    // -- Input ----------------------------------------------------------------

    override fun mouseScrolled(mouseX: Double, mouseY: Double, hAmt: Double, vAmt: Double): Boolean {
        if (overlayStack.mouseScrolled(vAmt)) return true
        if (mouseX >= entryLeft) {
            scroll = (scroll - (vAmt * ROW_H).toInt()).coerceIn(0, maxScroll)
            syncWidgets(); return true
        }
        return super.mouseScrolled(mouseX, mouseY, hAmt, vAmt)
    }

    override fun mouseClicked(click: MouseButtonEvent, doubled: Boolean): Boolean {
        val mx = click.x().toInt(); val my = click.y().toInt()

        // Capture mouse button for keybind rebinding
        val capturingMouse = capturingKeybind
        if (capturingMouse != null) {
            val button = click.button()
            val packed = KeybindPacked.packMouse(button, Modifiers(click.hasControlDown(), click.hasShiftDown(), click.hasAltDown()))
            capturingMouse.set(packed)
            capturingMouse.onChanged?.invoke(packed)
            capturingKeybind = null
            return true
        }

        // Route to overlay stack first - always consumes the click when an overlay is open
        if (overlayStack.isOpen) {
            overlayStack.handleMouseClicked(mx, my)
            // Also do global scTextField defocus pass
            scTextFields.values.forEach { it.mouseClicked(mx, my) }
            return true
        }

        // Global defocus pass
        scTextFields.values.forEach { it.mouseClicked(mx, my) }

        // Done button
        val bw = 80; val bh = 20
        val bx = width / 2 - bw / 2; val by = height - BOT_H + (BOT_H - bh) / 2
        if (mx in bx until bx + bw && my in by until by + bh) {
            manager.save()
            //? if >= 26.2 {
            /*minecraft.gui.setScreen(parent)
            *///?} else {
            minecraft.setScreen(parent)
            //?}
            return true
        }

        if (super.mouseClicked(click, doubled)) return true

        // Category panel
        if (mx < CAT_W) {
            for (item in buildSidebarItems()) {
                if (my in item.y until item.y + item.h) {
                    selCat = item.catIdx; selSubCat = item.subCatIdx
                    scroll = 0; searchText = ""
                    searchField?.setValue(""); rebuildRows(); syncWidgets()
                    break
                }
            }
            return true
        }

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
                is Row.EntryRow -> handleEntryClick(row.entry, mx, ry + 3, my)
                else -> false
            }
        }
        return false
    }

    private fun handleEntryClick(entry: ProcessedEntry, mx: Int, wy: Int, my: Int): Boolean {
        val wx = widgetX(); val ww = widgetW()
        if (mx !in wx until wx + ww) return false
        scTextFields.values.forEach { it.focused = false }
        when (entry) {
            is ProcessedEntry.TextEntry -> {
                scTextFields[entry]?.mouseClicked(mx, my)
                return scTextFields[entry]?.focused ?: false
            }
            is ProcessedEntry.BoolEntry     -> entry.set(!entry.get())
            is ProcessedEntry.SliderEntry   -> {
                val inputX = wx + ww - SLIDER_INP
                if (mx < inputX) startSliderDragImpl(entry, mx, widgetX(), widgetW() - SLIDER_INP - 4)
                else scTextFields[entry]?.mouseClicked(mx, my)
            }
            is ProcessedEntry.DropdownEntry -> {
                val existing = overlayStack.topLayer?.overlays
                    ?.filterIsInstance<DropdownOverlay>()?.firstOrNull { it.entry === entry }
                if (existing != null) overlayStack.pop()
                else overlayStack.push(DropdownOverlay(
                    entry         = entry,
                    anchorX       = wx,
                    anchorBottom  = wy + ROW_H - 3,
                    anchorW       = ww,
                    screenHeight  = { height },
                    accentColor   = { accentColor },
                    tr            = font,
                    closeCallback = { overlayStack.pop() },
                ))
            }
            is ProcessedEntry.ButtonEntry      -> entry.action()
            is ProcessedEntry.ColorEntry       -> {
                overlayStack.push(ColorPickerOverlay(entry, ctx) { overlayStack.pop() })
            }
            is ProcessedEntry.KeybindEntry -> {
                val canReset = entry.defaultPacked != 0 && entry.get() != entry.defaultPacked
                if (canReset && mx >= wx + ww - 16) {
                    entry.set(entry.defaultPacked)
                    entry.onChanged?.invoke(entry.defaultPacked)
                } else {
                    capturingKeybind = entry
                }
            }
            is ProcessedEntry.MutableListEntry  -> {
                overlayStack.push(ListOverlay(entry, ctx) { overlayStack.pop() })
            }
            is ProcessedEntry.ObjectListEntry   -> {
                val layer = OverlayLayer()
                val olo = ObjectListOverlay(entry, ctx, layer)
                layer.addPeer(olo)
                overlayStack.push(layer)
            }
            is ProcessedEntry.MultiSelectEntry  -> {
                val existing = overlayStack.topLayer?.overlays
                    ?.filterIsInstance<MultiSelectOverlay>()?.firstOrNull { it.entry === entry }
                if (existing != null) overlayStack.pop()
                else overlayStack.push(MultiSelectOverlay(
                    entry         = entry,
                    anchorX       = wx,
                    anchorBottom  = wy + ROW_H - 3,
                    anchorW       = ww,
                    screenHeight  = { height },
                    accentColor   = { accentColor },
                    tr            = font,
                    closeCallback = { overlayStack.pop() },
                ))
            }
            else -> return false
        }
        return true
    }

    override fun mouseDragged(click: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean {
        if (overlayStack.mouseDragged(click.x().toInt(), click.y().toInt())) return true
        val drag = draggingSlider
        if (drag != null) { applySliderX(drag, click.x().toInt()); return true }
        return super.mouseDragged(click, deltaX, deltaY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        overlayStack.mouseReleased()
        if (draggingSlider != null) {
            draggingSlider = null
            syncWidgets()
        }
        return super.mouseReleased(click)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        val keyCode = input.key(); val mods = input.modifiers()

        // Route to overlay stack first
        if (overlayStack.keyPressed(keyCode, mods)) return true

        // Route to focused main-screen ScTextField
        scTextFields.values.firstOrNull { it.focused }?.let { tf ->
            if (tf.keyPressed(keyCode, mods)) return true
        }

        val capturing = capturingKeybind
        if (capturing != null) {
            if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL  || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL ||
                keyCode == GLFW.GLFW_KEY_LEFT_SHIFT     || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT   ||
                keyCode == GLFW.GLFW_KEY_LEFT_ALT       || keyCode == GLFW.GLFW_KEY_RIGHT_ALT
            ) return true
            val packed = KeybindPacked.pack(keyCode, Modifiers(input.hasControlDown(), input.hasShiftDown(), input.hasAltDown()))
            capturing.set(packed)
            capturing.onChanged?.invoke(packed)
            capturingKeybind = null
            return true
        }

        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            manager.save()
            //? if >= 26.2 {
            /*minecraft.gui.setScreen(parent)
            *///?} else {
            minecraft.setScreen(parent)
            //?}
            return true
        }
        return super.keyPressed(input)
    }

    override fun charTyped(input: CharacterEvent): Boolean {
        if (!input.isAllowedChatCharacter()) return false
        val chr = input.codepoint().toChar()
        if (overlayStack.charTyped(chr)) return true
        if (scTextFields.values.firstOrNull { it.focused }?.charTyped(chr) == true) return true
        return super.charTyped(input)
    }

    override fun shouldCloseOnEsc() = false

    fun refresh() { rebuildRows(); syncWidgets() }

    // -- Slider helpers -------------------------------------------------------

    private fun applySliderX(e: ProcessedEntry.SliderEntry, mx: Int) {
        if (sliderDragTrackW <= 0) return
        val ratio   = ((mx - sliderDragTrackX).toDouble() / sliderDragTrackW).coerceIn(0.0, 1.0)
        val stepped = (round((e.min + ratio * (e.max - e.min)) / e.step) * e.step).coerceIn(e.min, e.max)
        e.set(stepped)
        syncWidgets()
    }

    private fun startSliderDragImpl(e: ProcessedEntry.SliderEntry, mx: Int, trackX: Int, trackW: Int) {
        sliderDragTrackX = trackX
        sliderDragTrackW = trackW
        draggingSlider = e
        applySliderX(e, mx)
    }
}
