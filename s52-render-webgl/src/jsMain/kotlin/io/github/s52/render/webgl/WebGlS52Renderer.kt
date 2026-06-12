package io.github.s52.render.webgl

import io.github.s52.core.draw.DrawCommandKind
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.performance.DrawCommandBatcher
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.preslib.PresLibPack
import io.github.s52.render.webgl.internal.AreaFillRenderer
import io.github.s52.render.webgl.internal.AreaPatternRenderer
import io.github.s52.render.webgl.internal.ColorResolver
import io.github.s52.render.webgl.internal.GeometryProjector
import io.github.s52.render.webgl.internal.LineRenderer
import io.github.s52.render.webgl.internal.RasterAtlasCache
import io.github.s52.render.webgl.internal.SolidColorProgram
import io.github.s52.render.webgl.internal.SymbolRenderer
import io.github.s52.render.webgl.internal.TextureProgram
import io.github.s52.render.webgl.internal.TextRenderer
import kotlin.js.unsafeCast
import org.khronos.webgl.WebGLRenderingContext
import org.w3c.dom.HTMLCanvasElement

/**
 * Phase 8 WebGL backend for renderer-independent S-52 draw commands.
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
    private val areaFillRenderer = AreaFillRenderer(gl, solidProgram)
    private val areaPatternRenderer = AreaPatternRenderer(gl, solidProgram, textureProgram, rasterAtlasCache, presLib)
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
        gl.blendFunc(
            WebGLRenderingContext.SRC_ALPHA,
            WebGLRenderingContext.ONE_MINUS_SRC_ALPHA
        )

        val colors = ColorResolver(presLib, settings.palette)
        val background = presLib.colors.color(settings.palette, "DEPDW")
        if (background != null) {
            gl.clearColor(
                background.r / 255.0f,
                background.g / 255.0f,
                background.b / 255.0f,
                1.0f
            )
        } else {
            gl.clearColor(0.9f, 0.95f, 1.0f, 1.0f)
        }
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT)

        val projector = GeometryProjector(viewport, canvas.width, canvas.height)
        val batchReport = DrawCommandBatcher.report(commands)
        val builder = RenderStatsBuilder()

        /*
         * Important performance detail:
         *
         * Area fills are often emitted as long adjacent runs. Rendering each one
         * separately causes many small Float32Array uploads and draw calls.
         *
         * Keep rendering order intact by batching only adjacent AreaFill commands.
         * Any non-area command flushes the pending area-fill run before rendering.
         *
         * AreaFillRenderer.renderBatch(...) is expected to preserve command order
         * inside the run and may internally group compatible fills by color.
         */
        val areaFillRun = ArrayList<S52DrawCommand.AreaFill>()
        fun flushAreaFillRun() {
            if (areaFillRun.isEmpty()) return

            val drawCalls = if (areaFillRun.size == 1) {
                areaFillRenderer.render(areaFillRun[0], projector, colors)
            } else {
                areaFillRenderer.renderBatch(areaFillRun, projector, colors)
            }

            builder.addMany(
                kind = DrawCommandKind.AreaFill,
                commandCount = areaFillRun.size,
                drawCalls = drawCalls
            )
            areaFillRun.clear()
        }

        for (command in commands) {
            when (command) {
                is S52DrawCommand.AreaFill -> {
                    areaFillRun += command
                }

                is S52DrawCommand.AreaPattern -> {
                    flushAreaFillRun()
                    val drawCalls = areaPatternRenderer.render(
                        command,
                        projector,
                        colors,
                        settings.palette
                    )
                    builder.add(command.kind, drawCalls)
                }

                is S52DrawCommand.LineSimple -> {
                    flushAreaFillRun()
                    val drawCalls = lineRenderer.renderSimple(command, projector, colors)
                    builder.add(command.kind, drawCalls)
                }

                is S52DrawCommand.LineComplex -> {
                    flushAreaFillRun()
                    val drawCalls = lineRenderer.renderComplex(command, projector, colors)
                    builder.add(command.kind, drawCalls)
                }

                is S52DrawCommand.PointSymbol -> {
                    flushAreaFillRun()
                    val drawCalls = symbolRenderer.render(
                        command,
                        projector,
                        colors,
                        settings.palette
                    )
                    builder.add(command.kind, drawCalls)
                }

                is S52DrawCommand.Text -> {
                    flushAreaFillRun()
                    val drawCalls = textRenderer.renderText(command, projector, colors)
                    builder.add(command.kind, drawCalls)
                }

                is S52DrawCommand.Sounding -> {
                    flushAreaFillRun()
                    val drawCalls = textRenderer.renderSounding(command, projector, colors)
                    builder.add(command.kind, drawCalls)
                }
            }
        }

        flushAreaFillRun()

        return builder.build(
            batchCount = batchReport.batchCount,
            averageCommandsPerBatch = batchReport.averageCommandsPerBatch
        )
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
            val context = canvas.getContext("webgl2")
                ?: error("WebGL2 is not available in this browser")

            /*
             * Kotlin/JS safe-cast is wrong here:
             *
             *     canvas.getContext("webgl2") as? WebGLRenderingContext
             *
             * A browser returns a WebGL2RenderingContext object. It is usable by
             * this renderer because this renderer only calls WebGLRenderingContext
             * APIs, but Kotlin's runtime safe-cast can reject it.
             *
             * The null check above proves WebGL2 exists; after that, use unsafeCast.
             */
            return context.unsafeCast<WebGLRenderingContext>()
        }
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
        addMany(
            kind = kind,
            commandCount = 1,
            drawCalls = calls
        )
    }

    fun addMany(
        kind: DrawCommandKind,
        commandCount: Int,
        drawCalls: Int
    ) {
        when (kind) {
            DrawCommandKind.AreaFill -> areaFillCount += commandCount
            DrawCommandKind.AreaPattern -> areaPatternCount += commandCount
            DrawCommandKind.LineSimple,
            DrawCommandKind.LineComplex -> lineCount += commandCount
            DrawCommandKind.PointSymbol -> symbolCount += commandCount
            DrawCommandKind.Text -> textCount += commandCount
            DrawCommandKind.Sounding -> soundingCount += commandCount
        }

        this.drawCalls += drawCalls
    }

    fun build(
        batchCount: Int,
        averageCommandsPerBatch: Double
    ): RenderStats = RenderStats(
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
