package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.render.webgl.coordinates
import org.khronos.webgl.WebGL2RenderingContext

internal class AreaPatternRenderer(
    private val gl: WebGL2RenderingContext,
    private val program: SolidColorProgram
) {
    fun render(command: S52DrawCommand.AreaPattern, projector: GeometryProjector, colors: ColorResolver): Int {
        val geometryCoordinates = command.geometry.coordinates()
        if (geometryCoordinates.isEmpty()) return 0

        val vertices = hatchLines(geometryCoordinates, projector)
        if (vertices.isEmpty()) return 0
        return program.draw(WebGL2RenderingContext.LINES, vertices, colors.resolve(command.backgroundColorToken, fallback = "CHMGD"))
    }

    private fun hatchLines(coordinates: List<Coordinate>, projector: GeometryProjector): FloatArray {
        var minLon = coordinates.first().lon
        var maxLon = coordinates.first().lon
        var minLat = coordinates.first().lat
        var maxLat = coordinates.first().lat
        for (coordinate in coordinates.drop(1)) {
            minLon = minOf(minLon, coordinate.lon)
            maxLon = maxOf(maxLon, coordinate.lon)
            minLat = minOf(minLat, coordinate.lat)
            maxLat = maxOf(maxLat, coordinate.lat)
        }

        val count = 8
        val floats = ArrayList<Float>(count * 4)
        for (i in 1..count) {
            val t = i.toDouble() / (count + 1)
            val a = projector.project(Coordinate(minLon, minLat + (maxLat - minLat) * t))
            val b = projector.project(Coordinate(maxLon, minLat + (maxLat - minLat) * t))
            floats.add(a.x); floats.add(a.y)
            floats.add(b.x); floats.add(b.y)
        }
        return floats.toFloatArray()
    }
}
