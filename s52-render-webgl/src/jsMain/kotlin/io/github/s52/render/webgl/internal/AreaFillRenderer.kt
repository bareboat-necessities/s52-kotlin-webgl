package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.geometry.PolygonTriangulator
import io.github.s52.core.geometry.TriangulationPoint
import org.khronos.webgl.WebGLRenderingContext

internal class AreaFillRenderer(
    private val gl: WebGLRenderingContext,
    private val program: SolidColorProgram
) {
    fun render(command: S52DrawCommand.AreaFill, projector: GeometryProjector, colors: ColorResolver): Int {
        val triangles = triangulate(command.geometry, projector)
        if (triangles.isEmpty()) return 0
        return program.draw(WebGLRenderingContext.TRIANGLES, triangles, colors.resolve(command.colorToken, fallback = "DEPDW"))
    }

    private fun triangulate(geometry: EncGeometry, projector: GeometryProjector): FloatArray {
        val polygon = geometry as? EncGeometry.Polygon ?: return FloatArray(0)
        if (polygon.outer.size < 3) return FloatArray(0)

        val outer = polygon.outer.map(projector::project).toTriangulationRing()
        val holes = polygon.holes.map { hole -> hole.map(projector::project).toTriangulationRing() }
        val triangles = PolygonTriangulator.triangulate(outer, holes)
        val floats = ArrayList<Float>(triangles.size * 6)
        for (triangle in triangles) {
            floats.add(triangle.a.x.toFloat()); floats.add(triangle.a.y.toFloat())
            floats.add(triangle.b.x.toFloat()); floats.add(triangle.b.y.toFloat())
            floats.add(triangle.c.x.toFloat()); floats.add(triangle.c.y.toFloat())
        }
        return floats.toFloatArray()
    }

    private fun List<ClipPoint>.toTriangulationRing(): List<TriangulationPoint> =
        map { TriangulationPoint(it.x.toDouble(), it.y.toDouble()) }
}
