package com.github.mikecraft1224.simplecore.overlay.api

import com.github.mikecraft1224.simplecore.overlay.OverlayRegistry
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Stores the screen position and scale of a HUD overlay.
 *
 * [x] and [y] are absolute pixel coordinates from the screen's top-left corner.
 * [scale] is a uniform multiplier applied at the overlay's local origin.
 *
 * Persists automatically via GSON - store as a field inside any config class:
 * ```kotlin
 * @Config("My Mod")
 * object MyConfig {
 *     var myHudPosition: OverlayPosition = OverlayPosition(10f, 10f)
 * }
 * ```
 */
class OverlayPosition(
    var x: Float = 10f,
    var y: Float = 10f,
    var scale: Float = 1f,
)

/**
 * The pixel dimensions of a rendered overlay, returned from the [renderAt] block.
 * Used by [OverlayRegistry] to size drag handles in [com.github.mikecraft1224.simplecore.overlay.OverlayEditScreen].
 */
data class OverlaySize(val width: Int, val height: Int)

/**
 * Applies this position's matrix transform to [state], calls [block] to render overlay
 * content at local (0, 0), then restores the matrix.
 *
 * The [OverlaySize] returned by [block] is reported to [OverlayRegistry] so the overlay
 * shows up as a draggable handle when the overlay editor is open.
 *
 * [label] must be a stable, unique string - it is used as the registry key and displayed
 * in the editor.
 *
 * ```kotlin
 * @Subscribe
 * fun onHud(event: RenderHudEvent) {
 *     position.renderAt(event.state, event.state.guiWidth(), event.state.guiHeight(), "FPS Counter") { state ->
 *         val text = "FPS: ${Minecraft.getInstance().fps}"
 *         val font = Minecraft.getInstance().font
 *         state.text(font, text, 0, 0, 0xFFFFFFFF.toInt(), true)
 *         OverlaySize(font.width(text), font.lineHeight)
 *     }
 * }
 * ```
 */
inline fun OverlayPosition.renderAt(
    state: GuiGraphicsExtractor,
    screenW: Int,
    screenH: Int,
    label: String,
    block: (GuiGraphicsExtractor) -> OverlaySize,
) {
    val absX = x.toInt().coerceIn(0, screenW)
    val absY = y.toInt().coerceIn(0, screenH)
    state.pose().pushMatrix()
    state.pose().translate(absX.toFloat(), absY.toFloat())
    if (scale != 1f) state.pose().scale(scale)
    val size = block(state)
    state.pose().popMatrix()
    // size is in local (pre-scale) pixels - scale it up so the reported footprint matches what's
    // actually visible on screen (the editor's drag handle is sized from this).
    val scaledSize = OverlaySize((size.width * scale).toInt(), (size.height * scale).toInt())
    OverlayRegistry.report(label, this, scaledSize, absX, absY)
}
