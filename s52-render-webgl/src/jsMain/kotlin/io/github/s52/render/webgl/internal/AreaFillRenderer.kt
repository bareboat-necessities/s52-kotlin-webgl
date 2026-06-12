package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.geometry.PolygonTriangulator
import io.github.s52.core.geometry.TriangulationPoint
import org.khronos.webgl.WebGLRenderingContext

internal class AreaFillRenderer(
    private val gl: WebGLRenderingContext,
    private val program: SolidColorProgram,
    private val stencilClipper: StencilPolygonClipper
) {
    fun render(command: S52DrawCommand.AreaFill, projector: GeometryProjector, colors: ColorResolver): Int =
        renderBatch(listOf(command), projector, colors)

    fun renderBatch(commands: List<S52DrawCommand.AreaFill>, projector: GeometryProjector, colors: ColorResolver): Int {
        if (commands.isEmpty()) return 0
        return if (stencilClipper.isAvailable()) {
            renderStencilBatch(commands, projector, colors)
        } else {
            renderTriangulatedBatch(commands, projector, colors)
        }
    }

    private fun renderStencilBatch(
        commands: List<S52DrawCommand.AreaFill>,
        projector: GeometryProjector,
        colors: ColorResolver
    ): Int {
        var calls = 0
        for (command in commands) {
            val polygon = command.geometry as? EncGeometry.Polygon ?: continue
            val projected = ProjectedPolygonClip.from(polygon, projector) ?: continue
            calls += stencilClipper.fill(projected, projector, colors.resolve(command.colorToken, fallback = "DEPDW"))
        }
        return calls
    }

    private fun renderTriangulatedBatch(
        commands: List<S52DrawCommand.AreaFill>,
        projector: GeometryProjector,
        colors: ColorResolver
    ): Int {
        var calls = 0
        var currentColorToken: String? = null
        var floats = FloatArrayBuilder(commands.size * DEFAULT_TRIANGLE_FLOAT_CAPACITY)

        fun flush() {
            if (floats.isEmpty()) return
            calls += program.draw(
                WebGLRenderingContext.TRIANGLES,
                floats,
                colors.resolve(currentColorToken, fallback = "DEPDW")
            )
            floats = FloatArrayBuilder(DEFAULT_TRIANGLE_FLOAT_CAPACITY)
        }

        for (command in commands) {
            if (currentColorToken != null && command.colorToken != currentColorToken) {
                flush()
            }
            currentColorToken = command.colorToken
            appendTriangles(command.geometry, projector, floats)
        }
        flush()
        return calls
    }

    private fun appendTriangles(geometry: EncGeometry, projector: GeometryProjector, out: FloatArrayBuilder) {
        val polygon = geometry as? EncGeometry.Polygon ?: return
        val projected = ProjectedPolygonClip.from(polygon, projector) ?: return
        val outer = projected.outer.toTriangulationRing()
        if (outer.size < 3) return
        val holes = projected.holes.map { it.toTriangulationRing() }.filter { it.size >= 3 }

        val triangles = PolygonTriangulator.triangulate(outer, holes)
        for (triangle in triangles) {
            out.add(triangle.a.x.toFloat(), triangle.a.y.toFloat())
            out.add(triangle.b.x.toFloat(), triangle.b.y.toFloat())
            out.add(triangle.c.x.toFloat(), triangle.c.y.toFloat())
        }
    }

    private fun List<ClipPoint>.toTriangulationRing(): List<TriangulationPoint> =
        map { TriangulationPoint(it.x.toDouble(), it.y.toDouble()) }

    private companion object {
        private const val DEFAULT_TRIANGLE_FLOAT_CAPACITY: Int = 96
    }
}
