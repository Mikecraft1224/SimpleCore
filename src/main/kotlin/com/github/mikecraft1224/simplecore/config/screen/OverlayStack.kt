@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.config.screen

import com.github.mikecraft1224.simplecore.config.ProcessedEntry
import com.github.mikecraft1224.simplecore.ui.ScTextField
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import org.lwjgl.glfw.GLFW
import java.util.IdentityHashMap

// -- ConfigOverlay --------------------------------------------------------------

interface ConfigOverlay {
    fun hitTests(mx: Int, my: Int): Boolean
    fun render(ctx: DrawContext, mx: Int, my: Int)
    fun mouseClicked(mx: Int, my: Int): Boolean
    fun keyPressed(keyCode: Int, mods: Int): Boolean = false
    fun charTyped(chr: Char): Boolean = false
    fun mouseScrolled(vAmt: Double): Boolean = false
    fun mouseDragged(mx: Int, my: Int): Boolean = false
    fun mouseReleased(): Boolean = false
    fun onClose() {}
}

// -- OverlayLayer ---------------------------------------------------------------

/**
 * A collection of peer [ConfigOverlay] instances at the same stack depth.
 * Clicking outside all peers in a layer causes [OverlayStack] to pop the whole layer.
 */
class OverlayLayer(val overlays: MutableList<ConfigOverlay> = mutableListOf()) {
    constructor(vararg overlays: ConfigOverlay) : this(overlays.toMutableList())

    fun hitTests(mx: Int, my: Int) = overlays.any { it.hitTests(mx, my) }

    fun handleMouseClicked(mx: Int, my: Int): Boolean {
        for (o in overlays.reversed()) {
            if (o.hitTests(mx, my)) return o.mouseClicked(mx, my)
        }
        return false
    }

    fun addPeer(o: ConfigOverlay)    { overlays.add(o) }
    fun removePeer(o: ConfigOverlay) { overlays.remove(o) }

    /** Replaces [old] with [new]. If [old] is null or not found, [new] is appended. */
    fun replacePeer(old: ConfigOverlay?, new: ConfigOverlay) {
        val idx = if (old != null) overlays.indexOf(old) else -1
        if (idx >= 0) overlays[idx] = new else overlays.add(new)
    }

    fun render(ctx: DrawContext, mx: Int, my: Int) = overlays.forEach { it.render(ctx, mx, my) }
    fun keyPressed(keyCode: Int, mods: Int)  = overlays.reversed().any { it.keyPressed(keyCode, mods) }
    fun charTyped(chr: Char)                 = overlays.reversed().any { it.charTyped(chr) }
    fun mouseScrolled(vAmt: Double)          = overlays.reversed().any { it.mouseScrolled(vAmt) }
    fun mouseDragged(mx: Int, my: Int)       = overlays.reversed().any { it.mouseDragged(mx, my) }
    fun mouseReleased()                      = overlays.any { it.mouseReleased() }
    fun onClose()                            = overlays.forEach { it.onClose() }
}

// -- OverlayStack ---------------------------------------------------------------

/**
 * A stack of [OverlayLayer] instances. Events flow to the top layer only.
 * Clicking outside all overlays in the top layer automatically pops it.
 */
class OverlayStack {
    private val layers = ArrayDeque<OverlayLayer>()

    val isEmpty  get() = layers.isEmpty()
    val isOpen   get() = layers.isNotEmpty()
    val topLayer get() = layers.lastOrNull()
    val depth    get() = layers.size

    fun push(layer: OverlayLayer)             { layers.addLast(layer) }
    fun push(vararg overlays: ConfigOverlay)  = push(OverlayLayer(*overlays))

    fun pop(): OverlayLayer? {
        val l = layers.lastOrNull() ?: return null
        l.onClose()
        layers.removeLast()
        return l
    }

    fun clear() { while (layers.isNotEmpty()) pop() }

    fun handleMouseClicked(mx: Int, my: Int): Boolean {
        val top = topLayer ?: return false
        return if (top.hitTests(mx, my)) {
            top.handleMouseClicked(mx, my)
            true
        } else {
            pop()
            true
        }
    }

    fun render(ctx: DrawContext, mx: Int, my: Int) = layers.forEach { it.render(ctx, mx, my) }

    fun keyPressed(keyCode: Int, mods: Int): Boolean {
        if (isEmpty) return false
        if (topLayer?.keyPressed(keyCode, mods) == true) return true
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { pop(); return true }
        return false
    }

    fun charTyped(chr: Char)           = topLayer?.charTyped(chr)        ?: false
    fun mouseScrolled(vAmt: Double)    = topLayer?.mouseScrolled(vAmt)   ?: false
    fun mouseDragged(mx: Int, my: Int) = topLayer?.mouseDragged(mx, my)  ?: false
    fun mouseReleased()                = topLayer?.mouseReleased()        ?: false
}

// -- ConfigScreenCtx -----------------------------------------------------------

/**
 * Snapshot of parent-screen services passed to overlay classes.
 *
 * @property tr               The current text renderer.
 * @property getW             Returns the current screen width.
 * @property getH             Returns the current screen height.
 * @property scFields         Map of [ScTextField] instances keyed by entry identity.
 * @property accentColor      Returns the current accent color.
 * @property startSliderDrag  Begins a slider drag with explicit track coordinates.
 * @property captureKeybind   Activates keybind-capture mode for the given entry.
 * @property drawWidget       Draws the standard widget for a [ProcessedEntry] at a given position.
 */
class ConfigScreenCtx(
    val tr: TextRenderer,
    val getW: () -> Int,
    val getH: () -> Int,
    val scFields: IdentityHashMap<ProcessedEntry, ScTextField>,
    val accentColor: () -> Int,
    val startSliderDrag: (entry: ProcessedEntry.SliderEntry, mx: Int, trackX: Int, trackW: Int) -> Unit,
    val captureKeybind: (ProcessedEntry.KeybindEntry) -> Unit,
    val drawWidget: (ctx: DrawContext, entry: ProcessedEntry, x: Int, y: Int, w: Int, h: Int, mx: Int, my: Int) -> Unit,
)
