package com.github.mikecraft1224.simplecore.ui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.Drawable
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput

/**
 * Registers a Drawable + Element + Selectable child widget with the hosting Screen.
 *
 * Passed into [Widget.onAdded] so widgets can register their Minecraft counterparts
 * (e.g. [net.minecraft.client.gui.widget.TextFieldWidget]) without needing a direct
 * reference to the protected [net.minecraft.client.gui.screen.Screen.addDrawableChild].
 */
interface ChildRegistrar {
    /** Registers [child] as a drawable+selectable child of the host screen. */
    fun <T> register(child: T) where T : Drawable, T : net.minecraft.client.gui.Element, T : net.minecraft.client.gui.Selectable
}

/**
 * Base contract for every UI element in the SimpleCore UI framework.
 *
 * A widget has a bounding box ([x], [y], [width], [height]) set by [layout].
 * The parent panel calls [layout] before the first frame and on every screen resize.
 * Input events are forwarded from the host [UiScreen] and should return `true` when
 * the widget consumed the event.
 *
 * Widgets wrapping Minecraft child elements (e.g. [net.minecraft.client.gui.widget.TextFieldWidget])
 * should register them via [onAdded] using the provided [ChildRegistrar].
 *
 * Input method signatures match the 1.21.10 Yarn-mapped [net.minecraft.client.gui.Element] interface.
 */
interface Widget {
    var x: Int
    var y: Int
    var width: Int
    var height: Int

    /**
     * Sets the bounding box and performs layout-dependent initialization.
     * Always called at least once before [render].
     */
    fun layout(x: Int, y: Int, width: Int, height: Int)

    /**
     * Renders the widget.
     *
     * @param ctx   draw context for this frame
     * @param mx    mouse x in screen pixels
     * @param my    mouse y in screen pixels
     * @param delta partial tick delta
     */
    fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float)

    /** Called when a mouse button is pressed. Returns true if consumed. */
    fun mouseClicked(click: Click, doubled: Boolean): Boolean = false

    /** Called when a mouse button is released. Returns true if consumed. */
    fun mouseReleased(click: Click): Boolean = false

    /** Called while a mouse button is held and the mouse moves. Returns true if consumed. */
    fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean = false

    /** Called when a key is pressed. Returns true if consumed. */
    fun keyPressed(input: KeyInput): Boolean = false

    /** Called when a printable character is typed. Returns true if consumed. */
    fun charTyped(input: CharInput): Boolean = false

    /** Called every game tick. */
    fun tick() {}

    /**
     * Called when this widget is added to a screen (during [UiScreen.init] or re-layout).
     * Use [registrar] to register any Minecraft child widgets with the host screen.
     */
    fun onAdded(registrar: ChildRegistrar) {}

    /** Called when this widget is removed from a screen. Use to release resources. */
    fun onRemoved() {}

    /** Returns whether the given screen coordinate falls inside this widget's bounds. */
    fun contains(px: Int, py: Int): Boolean =
        px in x until x + width && py in y until y + height
}

/** Alignment along a single axis. Used by [com.github.mikecraft1224.simplecore.ui.layout.Align] and layouts. */
enum class Alignment { START, CENTER, END }

/** Main-axis justification for [com.github.mikecraft1224.simplecore.ui.layout.LinearLayout]. */
enum class Justify {
    /** Items packed at the start (default). */
    START,
    /** Items centered along the main axis. */
    CENTER,
    /** Items packed at the end. */
    END,
    /** Equal space between items; no space at edges. */
    SPACE_BETWEEN,
    /** Equal space around each item (half-unit at edges). */
    SPACE_AROUND,
    /** Equal space between items and at edges. */
    SPACE_EVENLY,
}
