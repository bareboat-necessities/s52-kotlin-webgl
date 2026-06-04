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
import io.github.s52.render.webgl.internal.SolidColorProgram
import io.github.s52.render.webgl.internal.SymbolRenderer
import io.github.s52.render.webgl.internal.TextRenderer
import org.khronos.webgl.WebGL2RenderingContext
import org.w3c.dom.HTMLCanvasElement

/**
 * Phase 8 WebGL2 backend for renderer-independent S-52 draw commands.
 *
 * This class deliberately accepts only [S52DrawCommand] values. It does not
 * inspect S-57 object classes, attributes, lookup records, or CSP names. That
 * boundary keeps portrayal decisions inside s52-core/s52-csp and leaves this
 * module responsible only for GPU rendering.
 */
class WebGlS52Renderer(
    private val canvas: HTMLCanvasElement,
    private val presLib: PresLibPack
) {
    private val gl: WebGL2RenderingContext =
        canvas.getContext("webgl2") as? WebGL2RenderingContext
            ?: error("WebGL2 is not available in this browser")

    private val solidProgram = SolidColorProgram(gl)
    private val areaFillRenderer = AreaFillRenderer(gl, solidProgram)
    private val areaPatternRenderer = AreaPatternRenderer(gl, solidProgram)
    private val lineRenderer = LineRenderer(gl, solidProgram)
    private val symbolRenderer = SymbolRenderer(gl, solidProgram, presLib)
    private val textRenderer = TextRenderer(gl, solidProgram)

    fun render(
        commands: List<S52DrawCommand>,
        settings: MarinerSettings,
        viewport: RenderViewport = RenderViewport.auto(commands)
    ): RenderStats {
        resizeToDisplaySize()
        gl.viewport(0, 0, canvas.width, canvas.height)
        gl.disable(WebGL2RenderingContext.DEPTH_TEST)
        gl.enable(WebGL2RenderingContext.BLEND)
        gl.blendFunc(WebGL2RenderingContext.SRC_ALPHA, WebGL2RenderingContext.ONE_MINUS_SRC_ALPHA)

        val colors = ColorResolver(presLib, settings.palette)
        val background = presLib.colors.color(settings.palette, "DEPDW")
        if (background != null) {
            gl.clearColor(background.r / 255.0f, background.g / 255.0f, background.b / 255.0f, 1.0f)
        } else {
            gl.clearColor(0.9f, 0.95f, 1.0f, 1.0f)
        }
        gl.clear(WebGL2RenderingContext.COLOR_BUFFER_BIT)

        val projector = GeometryProjector(viewport, canvas.width, canvas.height)
        val batchReport = DrawCommandBatcher.report(commands)
        val builder = RenderStatsBuilder()

        for (command in commands) {
            val drawCalls = when (command) {
                is S52DrawCommand.AreaFill -> areaFillRenderer.render(command, projector, colors)
                is S52DrawCommand.AreaPattern -> areaPatternRenderer.render(command, projector, colors)
                is S52DrawCommand.LineSimple -> lineRenderer.renderSimple(command, projector, colors)
                is S52DrawCommand.LineComplex -> lineRenderer.renderComplex(command, projector, colors)
                is S52DrawCommand.PointSymbol -> symbolRenderer.render(command, projector, colors)
                is S52DrawCommand.Text -> textRenderer.renderText(command, projector, colors)
                is S52DrawCommand.Sounding -> textRenderer.renderSounding(command, projector, colors)
            }
            builder.add(command.kind, drawCalls)
        }
        return builder.build(batchReport.batchCount, batchReport.averageCommandsPerBatch)
    }

    private fun resizeToDisplaySize() {
        val displayWidth = canvas.clientWidth.coerceAtLeast(1)
        val displayHeight = canvas.clientHeight.coerceAtLeast(1)
        if (canvas.width != displayWidth || canvas.height != displayHeight) {
            canvas.width = displayWidth
            canvas.height = displayHeight
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
        when (kind) {
            DrawCommandKind.AreaFill -> areaFillCount++
            DrawCommandKind.AreaPattern -> areaPatternCount++
            DrawCommandKind.LineSimple, DrawCommandKind.LineComplex -> lineCount++
            DrawCommandKind.PointSymbol -> symbolCount++
            DrawCommandKind.Text -> textCount++
            DrawCommandKind.Sounding -> soundingCount++
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
