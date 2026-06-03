package com.github.mikecraft1224.simplecore.render.internal

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.MappableRingBuffer
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BuiltBuffer
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.util.BufferAllocator
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.system.MemoryUtil
import java.util.OptionalDouble
import java.util.OptionalInt

internal object PipelineRenderer {

    private val allocator = BufferAllocator(RenderLayer.CUTOUT_BUFFER_SIZE)
    private var vertexBuffer: MappableRingBuffer? = null

    // Uniform values: white color modulator, no model offset, identity texture matrix
    private val colorModulator = Vector4f(1f, 1f, 1f, 1f)
    private val modelOffset = Vector3f()
    private val textureMatrix = Matrix4f()

    fun drawQuads(pipeline: RenderPipeline, addVertices: BufferBuilder.() -> Unit) {
        val client = MinecraftClient.getInstance()

        val builder = BufferBuilder(allocator, pipeline.vertexFormatMode, pipeline.vertexFormat)
        builder.addVertices()
        val builtBuffer = builder.end()

        val drawState = builtBuffer.drawParameters
        if (drawState.vertexCount() == 0) {
            builtBuffer.close()
            return
        }

        val format = drawState.format()
        val vertices = upload(builtBuffer, format, drawState)

        // QUADS mode requires sorted index buffer from the built buffer
        builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().vertexSorter)
        val indices: GpuBuffer = pipeline.vertexFormat.uploadImmediateIndexBuffer(builtBuffer.sortedBuffer)
        val indexType: VertexFormat.IndexType = drawState.indexType()

        val dynamicTransforms: GpuBufferSlice = RenderSystem.getDynamicUniforms()
            .write(RenderSystem.getModelViewMatrix(), colorModulator, modelOffset, textureMatrix, 2f)

        client.framebuffer.colorAttachmentView?.let { colorTexture ->
            client.framebuffer.depthAttachmentView?.let { depthTexture ->
                RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    { "simplecore pipeline render" },
                    colorTexture,
                    OptionalInt.empty(),
                    depthTexture,
                    OptionalDouble.empty()
                ).use { renderPass ->
                    renderPass.setPipeline(pipeline)
                    RenderSystem.bindDefaultUniforms(renderPass)
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms)
                    renderPass.setVertexBuffer(0, vertices)
                    renderPass.setIndexBuffer(indices, indexType)
                    renderPass.drawIndexed(0, 0, drawState.indexCount(), 1)
                }
            }
        }

        builtBuffer.close()
        vertexBuffer?.rotate()
    }

    fun drawLines(pipeline: RenderPipeline, addVertices: BufferBuilder.() -> Unit) {
        val client = MinecraftClient.getInstance()

        val builder = BufferBuilder(allocator, pipeline.vertexFormatMode, pipeline.vertexFormat)
        builder.addVertices()
        val builtBuffer = builder.end()

        val drawState = builtBuffer.drawParameters
        if (drawState.vertexCount() == 0) {
            builtBuffer.close()
            return
        }

        val vertices = upload(builtBuffer, drawState.format(), drawState)

        val dynamicTransforms: GpuBufferSlice = RenderSystem.getDynamicUniforms()
            .write(RenderSystem.getModelViewMatrix(), colorModulator, modelOffset, textureMatrix, 2f)

        client.framebuffer.colorAttachmentView?.let { colorTexture ->
            client.framebuffer.depthAttachmentView?.let { depthTexture ->
                RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                    { "simplecore line render" },
                    colorTexture,
                    OptionalInt.empty(),
                    depthTexture,
                    OptionalDouble.empty()
                ).use { renderPass ->
                    renderPass.setPipeline(pipeline)
                    RenderSystem.bindDefaultUniforms(renderPass)
                    renderPass.setUniform("DynamicTransforms", dynamicTransforms)
                    renderPass.setVertexBuffer(0, vertices)
                    renderPass.draw(0, drawState.vertexCount())
                }
            }
        }

        builtBuffer.close()
        vertexBuffer?.rotate()
    }

    private fun upload(builtBuffer: BuiltBuffer, format: VertexFormat, drawState: BuiltBuffer.DrawParameters): GpuBuffer {
        val vertexBufferSize = drawState.vertexCount() * format.vertexSize

        if (vertexBuffer == null || vertexBuffer!!.size() < vertexBufferSize) {
            vertexBuffer?.close()
            vertexBuffer = MappableRingBuffer(
                { "simplecore render buffer" },
                GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_MAP_WRITE,
                vertexBufferSize
            )
        }

        val commandEncoder = RenderSystem.getDevice().createCommandEncoder()
        commandEncoder.mapBuffer(vertexBuffer!!.blocking.slice(0, builtBuffer.buffer.remaining()), false, true).use { mapped ->
            MemoryUtil.memCopy(builtBuffer.buffer, mapped.data())
        }

        return vertexBuffer!!.blocking
    }

    fun close() {
        allocator.close()
        vertexBuffer?.close()
        vertexBuffer = null
    }
}
