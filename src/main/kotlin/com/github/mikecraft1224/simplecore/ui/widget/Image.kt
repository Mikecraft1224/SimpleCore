@file:Suppress("unused")

package com.github.mikecraft1224.simplecore.ui.widget

import com.github.mikecraft1224.simplecore.ui.Widget
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.util.Identifier

/**
 * Renders a GUI texture identified by a resource [Identifier].
 *
 * Uses [DrawContext.drawTexture] with the standard `GUI_TEXTURED` render pipeline.
 * For full-texture rendering, use only [texture] and leave other params at defaults.
 * For sprite-sheet sub-regions, pass [u], [v], [regionW], [regionH], [texW], [texH].
 *
 * ```kotlin
 * val logo = Image(Identifier.of("mymod", "textures/gui/logo.png"))
 * ```
 *
 * @param texture  resource location of the texture file
 * @param u        left edge of the region in pixels (default 0)
 * @param v        top edge of the region in pixels (default 0)
 * @param regionW  width of the region in pixels; -1 = same as widget width
 * @param regionH  height of the region in pixels; -1 = same as widget height
 * @param texW     full texture width in pixels (for UV calculation)
 * @param texH     full texture height in pixels
 */
class Image(
    var texture: Identifier,
    private val u: Int = 0,
    private val v: Int = 0,
    private val regionW: Int = -1,
    private val regionH: Int = -1,
    private val texW: Int = 256,
    private val texH: Int = 256,
) : Widget {

    override var x: Int = 0
    override var y: Int = 0
    override var width: Int = 0
    override var height: Int = 0

    override fun layout(x: Int, y: Int, width: Int, height: Int) {
        this.x = x; this.y = y; this.width = width; this.height = height
    }

    override fun render(ctx: DrawContext, mx: Int, my: Int, delta: Float) {
        val rw = if (regionW >= 0) regionW else texW
        val rh = if (regionH >= 0) regionH else texH
        ctx.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x, y,
            u.toFloat(), v.toFloat(),
            width, height,
            rw, rh,
        )
    }
}
