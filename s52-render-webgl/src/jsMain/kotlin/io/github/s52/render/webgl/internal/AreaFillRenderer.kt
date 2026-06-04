package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import org.khronos.webgl.WebGL2RenderingContext

internal class AreaFillRenderer(
    private val gl: WebGL2RenderingContext,
    private val program: SolidColorProgram
) {
    fun render(command: S52DrawCommand.AreaFill, projector: GeometryProjector, colors: ColorResolver): Int {
        val triangles = triangulate(command.geometry, projector)
        if (triangles.isEmpty()) return 0
        return program.draw(WebGL2RenderingContext.TRIANGLES, triangles, colors.resolve(command.colorToken, fallback = "DEPDW"))
    }

    private fun triangulate(geometry: EncGeometry, projector: GeometryProjector): FloatArray {
        if (geometry !is EncGeometry.Polygon || geometry.outer.size < 3) return FloatArray(0)
        val ring = geometry.outer
        val floats = ArrayList<Float>((ring.size - 2) * 6)
        val first = projector.project(ring[0])
        for (i in 1 until ring.lastIndex) {
            val b = projector.project(ring[i])
            val c = projector.project(ring[i + 1])
            floats.add(first.x); floats.add(first.y)
            floats.add(b.x); floats.add(b.y)
            floats.add(c.x); floats.add(c.y)
        }
        return floats.toFloatArray()
    }
}
