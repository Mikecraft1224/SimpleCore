package com.github.mikecraft1224.simplecore.ui

import net.minecraft.client.gui.Click
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput

/**
 * Abstract container that holds child [Widget] instances.
 *
 * Concrete subclasses implement [doLayout] to distribute bounds across children.
 * All input and render calls are forwarded to children in order; the first child
 * to return `true` from an input method stops propagation.
 *
 * A panel is itself a [Widget], so panels can be nested arbitrarily.
 */
abstract class Panel : Widget {

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    protected val children: MutableList<Widget> = mutableListOf()

    /** Adds [widget] to this panel's child list. Returns `this` for chaining. */
    open fun add(widget: Widget): Panel {
        children.add(widget)
        return this
    }

    /**
     * Called by [layout] after the panel's own bounds are updated.
     * Implementations must call [Widget.layout] on every child.
     */
    protected abstract fun doLayout()

    final override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
        doLayout()
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        for (child in children) child.render(ctx, mx, my, delta)
    }

    override fun mouseClicked(click: Click, doubled: Boolean): Boolean =
        children.any { it.mouseClicked(click, doubled) }

    override fun mouseReleased(click: Click): Boolean =
        children.any { it.mouseReleased(click) }

    override fun mouseDragged(click: Click, deltaX: Double, deltaY: Double): Boolean =
        children.any { it.mouseDragged(click, deltaX, deltaY) }

    override fun keyPressed(input: KeyInput): Boolean =
        children.any { it.keyPressed(input) }

    override fun charTyped(input: CharInput): Boolean =
        children.any { it.charTyped(input) }

    override fun tick() {
        children.forEach { it.tick() }
    }

    override fun onAdded(registrar: ChildRegistrar) {
        children.forEach { it.onAdded(registrar) }
    }

    override fun onRemoved() {
        children.forEach { it.onRemoved() }
    }
}
