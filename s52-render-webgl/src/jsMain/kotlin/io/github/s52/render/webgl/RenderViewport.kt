package io.github.s52.render.webgl

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import kotlin.math.abs
import kotlin.math.max

/**
 * Renderer viewport in chart coordinates.
 *
 * Intentionally uses an equirectangular lon/lat projection because the
 * renderer consumes already-assembled chart geometry. A future chart engine can
 * pass a viewport computed by its own map projection layer.
 */
data class RenderViewport(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
) {
    init {
        require(east > west) { "east must be greater than west" }
        require(north > south) { "north must be greater than south" }
    }

    companion object {
        fun auto(commands: List<S52DrawCommand>, paddingFraction: Double = 0.08): RenderViewport {
            val coordinates = commands.flatMap { it.geometry.coordinates() }
            if (coordinates.isEmpty()) {
                return RenderViewport(-1.0, -1.0, 1.0, 1.0)
            }

            var west = coordinates.first().lon
            var east = coordinates.first().lon
            var south = coordinates.first().lat
            var north = coordinates.first().lat
            for (coordinate in coordinates.drop(1)) {
                west = minOf(west, coordinate.lon)
                east = maxOf(east, coordinate.lon)
                south = minOf(south, coordinate.lat)
                north = maxOf(north, coordinate.lat)
            }

            val lonSpan = max(abs(east - west), 1e-6)
            val latSpan = max(abs(north - south), 1e-6)
            val padLon = lonSpan * paddingFraction
            val padLat = latSpan * paddingFraction
            return RenderViewport(west - padLon, south - padLat, east + padLon, north + padLat)
        }
    }
}

internal fun EncGeometry.coordinates(): List<Coordinate> = when (this) {
    is EncGeometry.Point -> listOf(coordinate)
    is EncGeometry.MultiPoint -> coordinates
    is EncGeometry.LineString -> coordinates
    is EncGeometry.Polygon -> outer + holes.flatten()
}
