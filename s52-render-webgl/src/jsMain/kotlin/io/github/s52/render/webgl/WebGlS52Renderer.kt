package io.github.s52.render.webgl

import io.github.s52.core.draw.DrawCommandKind
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.preslib.PresLibPack
import io.github.s52.render.webgl.internal.AreaFillRenderer
import io.github.s52.render.webgl.internal.AreaPatternRenderer
import io.github.s52.render.webgl.internal.ColorResolver
import io.github.s52.render.webgl.internal.GeometryProjector
import io.github.s52.render.webgl.internal.LineRenderer
import io.github.s52.render.webgl.internal.RasterAtlasCache
import io.github.s52.render.webgl.internal.SolidColorProgram
import io.github.s52.render.webgl.internal.StencilPolygonClipper
import io.github.s52.render.webgl.internal.SymbolRenderer
import io.github.s52.render.webgl.internal.TextureProgram
import io.github.s52.render.webgl.internal.TextRenderer
import kotlin.js.unsafeCast
import org.khronos.webgl.WebGLRenderingContext
import org.w3c.dom.HTMLCanvasElement

/**
 * WebGL backend for renderer-independent S-52 draw commands.
 *
 * This class deliberately accepts only [S52DrawCommand] values. It does not
 * inspect S-57 object classes, attributes, lookup records, or CSP names. That
 * boundary keeps portrayal decisions inside s52-core/s52-csp and leaves this
 * module responsible only for GPU rendering.
 */
class WebGlS52Renderer(
    private val canvas: HTMLCanvasElement,
    private val presLib: PresLibPack,
    private val onResourcesChanged: (() -> Unit)? = null
) {
    private val gl: WebGLRenderingContext = requireWebGl2(canvas)

    private val solidProgram = SolidColorProgram(gl)
    private val textureProgram = TextureProgram(gl)
    private val rasterAtlasCache = RasterAtlasCache(gl, onAtlasReady = onResourcesChanged)
    private val stencilClipper = StencilPolygonClipper(gl, solidProgram)
    private val areaFillRenderer = AreaFillRenderer(gl, solidProgram, stencilClipper)
    private val areaPatternRenderer = AreaPatternRenderer(gl, solidProgram, textureProgram, rasterAtlasCache, presLib, stencilClipper)
    private val lineRenderer = LineRenderer(gl, solidProgram, presLib)
    private val symbolRenderer = SymbolRenderer(gl, solidProgram, textureProgram, rasterAtlasCache, presLib)
    private val textRenderer = TextRenderer(gl, solidProgram)

    fun render(
        commands: List<S52DrawCommand>,
        settings: MarinerSettings,
        viewport: RenderViewport = RenderViewport.auto(commands)
    ): RenderStats {
        resizeToDisplaySize()
        gl.viewport(0, 0, canvas.width, canvas.height)
        gl.disable(WebGLRenderingContext.DEPTH_TEST)
        gl.enable(WebGLRenderingContext.BLEND)
        gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA)

        val colors = ColorResolver(presLib, settings.palette)
        val background = presLib.colors.color(settings.palette, "DEPDW")
        if (background != null) {
            gl.clearColor(background.r / 255.0f, background.g / 255.0f, background.b / 255.0f, 1.0f)
        } else {
            gl.clearColor(0.9f, 0.95f, 1.0f, 1.0f)
        }
        gl.disable(WebGLRenderingContext.STENCIL_TEST)
        gl.disable(WebGLRenderingContext.SCISSOR_TEST)
        gl.colorMask(true, true, true, true)
        gl.stencilMask(0xFF)
        gl.clearStencil(0)
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT or WebGLRenderingContext.STENCIL_BUFFER_BIT)

        val projector = GeometryProjector(viewport, canvas.width, canvas.height)
        textRenderer.beginFrame()
        val builder = RenderStatsBuilder()
        var renderBatchCount = 0

        var index = 0
        while (index < commands.size) {
            val command = commands[index]
            when (command) {
                is S52DrawCommand.AreaFill -> {
                    val start = index
                    val colorToken = command.colorToken
                    index++
                    while (index < commands.size) {
                        val next = commands[index]
                        if (next !is S52DrawCommand.AreaFill || next.colorToken != colorToken) break
                        index++
                    }
                    val batch = ArrayList<S52DrawCommand.AreaFill>(index - start)
                    for (batchIndex in start until index) batch += commands[batchIndex] as S52DrawCommand.AreaFill
                    val drawCalls = areaFillRenderer.renderBatch(batch, projector, colors)
                    builder.addMany(DrawCommandKind.AreaFill, batch.size, drawCalls)
                    renderBatchCount++
                }

                is S52DrawCommand.LineSimple -> {
                    val start = index
                    val colorToken = command.colorToken
                    val width = command.width
                    val style = command.style
                    index++
                    while (index < commands.size) {
                        val next = commands[index]
                        if (next !is S52DrawCommand.LineSimple) break
                        if (next.colorToken != colorToken || next.width != width || next.style != style) break
                        index++
                    }
                    val batchSize = index - start
                    val drawCalls = if (batchSize == 1) {
                        lineRenderer.renderSimple(command, projector, colors)
                    } else {
                        val batch = ArrayList<S52DrawCommand.LineSimple>(batchSize)
                        for (batchIndex in start until index) batch += commands[batchIndex] as S52DrawCommand.LineSimple
                        lineRenderer.renderSimpleBatch(batch, projector, colors)
                    }
                    builder.addMany(DrawCommandKind.LineSimple, batchSize, drawCalls)
                    renderBatchCount++
                }

                is S52DrawCommand.AreaPattern -> {
                    val drawCalls = areaPatternRenderer.render(command, projector, colors, settings.palette)
                    builder.add(command.kind, drawCalls)
                    index++
                    renderBatchCount++
                }

                is S52DrawCommand.LineComplex -> {
                    val drawCalls = lineRenderer.renderComplex(command, projector, colors)
                    builder.add(command.kind, drawCalls)
                    index++
                    renderBatchCount++
                }

                is S52DrawCommand.PointSymbol -> {
                    val drawCalls = symbolRenderer.render(command, projector, colors, settings.palette)
                    builder.add(command.kind, drawCalls)
                    index++
                    renderBatchCount++
                }

                is S52DrawCommand.Text -> {
                    val drawCalls = textRenderer.renderText(command, projector, colors)
                    builder.add(command.kind, drawCalls)
                    index++
                    renderBatchCount++
                }

                is S52DrawCommand.Sounding -> {
                    val drawCalls = textRenderer.renderSounding(command, projector, colors)
                    builder.add(command.kind, drawCalls)
                    index++
                    renderBatchCount++
                }
            }
        }

        val averageCommandsPerBatch = if (renderBatchCount == 0) {
            0.0
        } else {
            commands.size.toDouble() / renderBatchCount.toDouble()
        }
        return builder.build(renderBatchCount, averageCommandsPerBatch)
    }

    private fun resizeToDisplaySize() {
        val displayWidth = canvas.clientWidth.coerceAtLeast(1)
        val displayHeight = canvas.clientHeight.coerceAtLeast(1)
        if (canvas.width != displayWidth || canvas.height != displayHeight) {
            canvas.width = displayWidth
            canvas.height = displayHeight
        }
    }

    private companion object {
        private fun requireWebGl2(canvas: HTMLCanvasElement): WebGLRenderingContext {
            val context = getWebGl2ContextWithStencil(canvas)
                ?: canvas.getContext("webgl2")
                ?: error("WebGL2 is not available in this browser")

            /*
             * Kotlin/JS safe-cast is wrong here:
             *
             *     canvas.getContext("webgl2") as? WebGLRenderingContext
             *
             * A browser returns a WebGL2RenderingContext object. It is usable by
             * this renderer because the renderer only calls WebGLRenderingContext
             * APIs, but Kotlin's runtime safe-cast can reject it. The null check
             * above proves WebGL2 exists; after that, use unsafeCast.
             */
            return context.unsafeCast<WebGLRenderingContext>()
        }

        private fun getWebGl2ContextWithStencil(canvas: HTMLCanvasElement): Any? =
            js("canvas.getContext('webgl2', { alpha: true, antialias: true, stencil: true })")
    }
}

data class RenderStats(
    val areaFillCount: Int,
    val areaPatternCount: Int,
    val lineCount: Int,
    val symbolCount: Int,
    val textCount: Int,
    val soundingCount: Int = 0,
    val drawCalls: Int = 0,
    val batchCount: Int = 0,
    val averageCommandsPerBatch: Double = 0.0
)

private class RenderStatsBuilder {
    private var areaFillCount = 0
    private var areaPatternCount = 0
    private var lineCount = 0
    private var symbolCount = 0
    private var textCount = 0
    private var soundingCount = 0
    private var drawCalls = 0

    fun add(kind: DrawCommandKind, calls: Int) {
        addMany(kind, 1, calls)
    }

    fun addMany(kind: DrawCommandKind, count: Int, calls: Int) {
        when (kind) {
            DrawCommandKind.AreaFill -> areaFillCount += count
            DrawCommandKind.AreaPattern -> areaPatternCount += count
            DrawCommandKind.LineSimple,
            DrawCommandKind.LineComplex -> lineCount += count
            DrawCommandKind.PointSymbol -> symbolCount += count
            DrawCommandKind.Text -> textCount += count
            DrawCommandKind.Sounding -> soundingCount += count
        }
        drawCalls += calls
    }

    fun build(batchCount: Int, averageCommandsPerBatch: Double): RenderStats = RenderStats(
        areaFillCount = areaFillCount,
        areaPatternCount = areaPatternCount,
        lineCount = lineCount,
        symbolCount = symbolCount,
        textCount = textCount,
        soundingCount = soundingCount,
        drawCalls = drawCalls,
        batchCount = batchCount,
        averageCommandsPerBatch = averageCommandsPerBatch
    )
}
