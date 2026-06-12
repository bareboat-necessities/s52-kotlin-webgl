package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.geometry.PolygonTriangulator
import io.github.s52.core.geometry.TriangulationPoint
import org.khronos.webgl.WebGLRenderingContext
import kotlin.math.abs
import kotlin.math.max

internal class AreaFillRenderer(
    private val gl: WebGLRenderingContext,
    private val program: SolidColorProgram
) {
    fun render(command: S52DrawCommand.AreaFill, projector: GeometryProjector, colors: ColorResolver): Int =
        renderBatch(listOf(command), projector, colors)

    fun renderBatch(commands: List<S52DrawCommand.AreaFill>, projector: GeometryProjector, colors: ColorResolver): Int {
        if (commands.isEmpty()) return 0
        val colorToken = commands.first().colorToken
        val floats = ArrayList<Float>(commands.size * DEFAULT_TRIANGLE_FLOAT_CAPACITY)
        for (command in commands) {
            appendTriangles(command.geometry, projector, floats)
        }
        if (floats.isEmpty()) return 0
        return program.draw(
            WebGLRenderingContext.TRIANGLES,
            floats.toFloatArray(),
            colors.resolve(colorToken, fallback = "DEPDW")
        )
    }

    private fun appendTriangles(geometry: EncGeometry, projector: GeometryProjector, out: MutableList<Float>) {
        val polygon = geometry as? EncGeometry.Polygon ?: return
        if (polygon.outer.size < 3) return

        val tolerance = projectionTolerance(projector)
        val outer = polygon.outer.map(projector::project).simplifyRing(tolerance).toTriangulationRing()
        if (outer.size < 3) return
        val holes = polygon.holes
            .map { hole -> hole.map(projector::project).simplifyRing(tolerance).toTriangulationRing() }
            .filter { it.size >= 3 }

        val triangles = PolygonTriangulator.triangulate(outer, holes)
        for (triangle in triangles) {
            out.add(triangle.a.x.toFloat()); out.add(triangle.a.y.toFloat())
            out.add(triangle.b.x.toFloat()); out.add(triangle.b.y.toFloat())
            out.add(triangle.c.x.toFloat()); out.add(triangle.c.y.toFloat())
        }
    }

    private fun projectionTolerance(projector: GeometryProjector): Double {
        val px = max(abs(projector.pixelToClipX(0.10).toDouble()), abs(projector.pixelToClipY(0.10).toDouble()))
        return max(px, 1.0e-7)
    }

    private fun List<ClipPoint>.simplifyRing(tolerance: Double): List<ClipPoint> {
        if (size < 3) return this

        val noDuplicates = ArrayList<ClipPoint>(size)
        for (point in this) {
            val last = noDuplicates.lastOrNull()
            if (last == null || !last.nearlyEquals(point, tolerance)) noDuplicates += point
        }
        if (noDuplicates.size > 1 && noDuplicates.first().nearlyEquals(noDuplicates.last(), tolerance)) {
            noDuplicates.removeAt(noDuplicates.lastIndex)
        }
        if (noDuplicates.size < 3) return noDuplicates

        var changed = true
        val simplified = noDuplicates.toMutableList()
        while (changed && simplified.size >= 3) {
            changed = false
            var i = 0
            while (i < simplified.size && simplified.size >= 3) {
                val previous = simplified[(i - 1 + simplified.size) % simplified.size]
                val current = simplified[i]
                val next = simplified[(i + 1) % simplified.size]
                if (current.nearlyEquals(previous, tolerance) || collinear(previous, current, next, tolerance)) {
                    simplified.removeAt(i)
                    changed = true
                } else {
                    i++
                }
            }
        }
        return simplified
    }

    private fun ClipPoint.nearlyEquals(other: ClipPoint, tolerance: Double): Boolean =
        abs(x.toDouble() - other.x.toDouble()) <= tolerance && abs(y.toDouble() - other.y.toDouble()) <= tolerance

    private fun collinear(a: ClipPoint, b: ClipPoint, c: ClipPoint, tolerance: Double): Boolean {
        val area2 = (b.x - a.x).toDouble() * (c.y - a.y).toDouble() -
            (b.y - a.y).toDouble() * (c.x - a.x).toDouble()
        return abs(area2) <= tolerance * tolerance
    }

    private fun List<ClipPoint>.toTriangulationRing(): List<TriangulationPoint> =
        map { TriangulationPoint(it.x.toDouble(), it.y.toDouble()) }

    private companion object {
        private const val DEFAULT_TRIANGLE_FLOAT_CAPACITY: Int = 96
    }
}
