package com.github.mikecraft1224.simplecore.render.api

import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import net.minecraft.client.gl.RenderPipelines as McRenderPipelines
import net.minecraft.util.Identifier

object ScRenderPipelines {
    val FILLED_XRAY: RenderPipeline = McRenderPipelines.register(
        RenderPipeline.builder(McRenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("simplecore", "pipeline/filled_xray"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val LINE_XRAY: RenderPipeline = McRenderPipelines.register(
        RenderPipeline.builder(McRenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of("simplecore", "pipeline/line_xray"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build()
    )
}
