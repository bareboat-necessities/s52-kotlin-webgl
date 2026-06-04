package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import org.khronos.webgl.WebGL2RenderingContext

internal class LineRenderer(
    private val gl: WebGL2RenderingContext,
    private val program: SolidColorProgram
) {
    fun renderSimple(command: S52DrawCommand.LineSimple, projector: GeometryProjector, colors: ColorResolver): Int {
        return drawGeometry(command.geometry, projector, colors.resolve(command.colorToken), lineWidth = command.width)
    }

    fun renderComplex(command: S52DrawCommand.LineComplex, projector: GeometryProjector, colors: ColorResolver): Int {
        return drawGeometry(command.geometry, projector, colors.resolve(null, fallback = "CHBLK"), lineWidth = 1.0)
    }

    private fun drawGeometry(geometry: EncGeometry, projector: GeometryProjector, color: GlColor, lineWidth: Double): Int {
        val vertices = when (geometry) {
            is EncGeometry.LineString -> geometry.coordinates.toVertices(projector)
            is EncGeometry.Polygon -> geometry.outer.toVertices(projector)
            else -> FloatArray(0)
        }
        if (vertices.isEmpty()) return 0
        gl.lineWidth(lineWidth.toFloat().coerceAtLeast(1.0f))
        return program.draw(WebGL2RenderingContext.LINE_STRIP, vertices, color)
    }

    private fun List<io.github.s52.core.geometry.Coordinate>.toVertices(projector: GeometryProjector): FloatArray {
        val floats = ArrayList<Float>(size * 2)
        for (coordinate in this) {
            val point = projector.project(coordinate)
            floats.add(point.x); floats.add(point.y)
        }
        return floats.toFloatArray()
    }
}
