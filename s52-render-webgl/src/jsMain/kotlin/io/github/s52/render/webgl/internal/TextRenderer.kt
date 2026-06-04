package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import org.khronos.webgl.WebGLRenderingContext

internal class TextRenderer(
    private val gl: WebGLRenderingContext,
    private val program: SolidColorProgram
) {
    fun renderText(command: S52DrawCommand.Text, projector: GeometryProjector, colors: ColorResolver): Int {
        val anchor = anchor(command.geometry, projector) ?: return 0
        val text = command.textExpression.ifBlank { command.rawArgs.firstOrNull().orEmpty() }
        val vertices = LineGlyphFont.lineVertices(text, anchor, projector, pixelSize = 10.0)
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.0f)
        return program.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(command.colorToken, fallback = "CHBLK"))
    }

    fun renderSounding(command: S52DrawCommand.Sounding, projector: GeometryProjector, colors: ColorResolver): Int {
        val anchor = anchor(command.geometry, projector) ?: return 0
        val vertices = LineGlyphFont.lineVertices(command.depthLabel, anchor, projector, pixelSize = 12.0)
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.0f)
        return program.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(command.colorToken, fallback = "SNDG1"))
    }

    private fun anchor(geometry: EncGeometry, projector: GeometryProjector): ClipPoint? = when (geometry) {
        is EncGeometry.Point -> projector.project(geometry.coordinate)
        is EncGeometry.MultiPoint -> geometry.coordinates.firstOrNull()?.let(projector::project)
        is EncGeometry.LineString -> geometry.coordinates.firstOrNull()?.let(projector::project)
        is EncGeometry.Polygon -> geometry.outer.firstOrNull()?.let(projector::project)
    }
}
