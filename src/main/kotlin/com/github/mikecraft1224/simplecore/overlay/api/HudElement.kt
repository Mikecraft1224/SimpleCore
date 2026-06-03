package com.github.mikecraft1224.simplecore.overlay.api

import com.github.mikecraft1224.simplecore.bus.events.RenderHudEvent
import net.minecraft.client.MinecraftClient

/**
 * Base class for moveable HUD overlays.
 *
 * Implement [buildContent] to describe the overlay content as a list of [HudRenderable] elements.
 * The framework handles layout, background, hover detection, click routing, and tooltip rendering.
 * Register the instance with [com.github.mikecraft1224.simplecore.overlay.HudManager.register] to activate it.
 *
 * ```kotlin
 * object MyHud : HudElement("My HUD", OverlayPosition(10f, 10f)) {
 *     private var mode = "Coins"
 *
 *     override fun buildContent() = listOf(
 *         HudRenderable.text("§e§lMy Tracker"),
 *         HudRenderable.hoverable("§7Profit: §a1,234,567", tooltip = listOf("§7Earned this session")),
 *         HudRenderable.selector("Mode", { mode }, listOf("Coins", "Items")) { mode = it },
 *         HudRenderable.clickable("§c[Reset]", tooltip = listOf("§7Resets all data")) { reset() },
 *     )
 * }
 *
 * // In your mod's init or loader:
 * HudManager.register(MyHud)
 * ```
 *
 * The [position] object should be stored in your config class if you want the position to
 * survive restarts. If you declare it inline (as above), it resets to its defaults on restart.
 *
 * Override [isEnabled] to skip rendering conditionally (e.g. player is null). The overlay
 * will not appear in the drag editor while disabled.
 */
abstract class HudElement(
    val displayName: String,
    val position: OverlayPosition,
) {
    /** Return `false` to skip rendering this frame. Defaults to `true`. */
    open fun isEnabled(): Boolean = true

    /** Whether to draw a dark background panel behind all lines. */
    protected open val showBackground: Boolean = true

    /** Pixels of padding added around the content on each side. */
    protected open val linePadding: Int = 5

    /** Vertical pixel gap between adjacent top-level elements. */
    protected open val lineSpacing: Int = 2

    /**
     * Returns the list of display elements to render this frame.
     *
     * Use the factory functions on [HudRenderable.Companion] to build your list:
     * [HudRenderable.text], [HudRenderable.hoverable], [HudRenderable.clickable],
     * [HudRenderable.selector], [HudRenderable.horizontal], [HudRenderable.spacer],
     * [HudRenderable.custom].
     *
     * The returned list is automatically stacked vertically by the framework.
     * To place elements side by side, wrap them in [HudRenderable.horizontal].
     *
     * The result is cached per frame — [buildContent] is called once per render pass
     * and reused if a click event arrives in the same frame.
     */
    abstract fun buildContent(): List<HudRenderable>

    /**
     * Called when a mouse button is pressed anywhere on the screen (no screen open).
     *
     * Override to handle overlay-level clicks before the element-level routing takes over.
     * The default implementation returns `false` (not consumed).
     *
     * @return `true` to consume the click and stop further routing.
     */
    open fun mouseClicked(mx: Int, my: Int, button: Int): Boolean = false

    private var frameCache: List<HudRenderable>? = null

    /** Clears the per-frame content cache. Called by [com.github.mikecraft1224.simplecore.overlay.HudManager] before rendering. */
    internal fun beginFrame() { frameCache = null }

    /** Called once per frame by [com.github.mikecraft1224.simplecore.overlay.HudManager]. */
    internal fun renderFrame(event: RenderHudEvent) {
        if (!isEnabled()) return
        val client = MinecraftClient.getInstance()
        val winScale = client.window.scaleFactor
        val screenMx = (client.mouse.x / winScale).toInt()
        val screenMy = (client.mouse.y / winScale).toInt()

        val lines = buildContent().also { frameCache = it }
        val container = HudRenderable.vertical(lines, lineSpacing)
        val pad = linePadding
        val pw = container.width + pad * 2
        val ph = container.height + pad * 2

        val absX = position.x.toInt().coerceIn(0, event.screenWidth)
        val absY = position.y.toInt().coerceIn(0, event.screenHeight)
        val localMx = ((screenMx - absX) / position.scale).toInt()
        val localMy = ((screenMy - absY) / position.scale).toInt()

        position.renderAt(event.ctx, event.screenWidth, event.screenHeight, displayName) { ctx ->
            if (showBackground) ctx.fill(0, 0, pw, ph, 0xCC000000.toInt())
            container.render(ctx, localMx, localMy, pad, pad)
            OverlaySize(pw, ph)
        }
    }

    /** Routes a mouse click to this element's content. Called by [com.github.mikecraft1224.simplecore.overlay.HudManager]. */
    internal fun routeMouseClicked(screenMx: Int, screenMy: Int, button: Int): Boolean {
        if (!isEnabled()) return false
        if (mouseClicked(screenMx, screenMy, button)) return true
        val client = MinecraftClient.getInstance()
        val absX = position.x.toInt().coerceIn(0, client.window.scaledWidth)
        val absY = position.y.toInt().coerceIn(0, client.window.scaledHeight)
        val localMx = ((screenMx - absX) / position.scale).toInt()
        val localMy = ((screenMy - absY) / position.scale).toInt()
        val container = HudRenderable.vertical(frameCache ?: buildContent(), lineSpacing)
        return container.mouseClicked(localMx, localMy, button, linePadding, linePadding)
    }
}
