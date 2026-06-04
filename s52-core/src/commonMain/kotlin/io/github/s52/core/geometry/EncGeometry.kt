package io.github.s52.core.geometry

sealed interface EncGeometry {
    data class Point(val coordinate: Coordinate) : EncGeometry
    data class MultiPoint(val coordinates: List<Coordinate>) : EncGeometry
    data class LineString(val coordinates: List<Coordinate>) : EncGeometry
    data class Polygon(
        val outer: List<Coordinate>,
        val holes: List<List<Coordinate>> = emptyList()
    ) : EncGeometry
}
