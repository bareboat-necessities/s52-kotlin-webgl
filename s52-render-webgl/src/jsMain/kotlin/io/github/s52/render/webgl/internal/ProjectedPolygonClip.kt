package io.github.s52.render.webgl.internal

import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.geometry.PolygonTriangulator
import io.github.s52.core.geometry.TriangulationPoint

internal data class ProjectedPolygonClip(
    val outer: List<ClipPoint>,
    val holes: List<List<ClipPoint>>
) {
    val allPoints: List<ClipPoint> = outer + holes.flatten()

    fun contains(x: Float, y: Float): Boolean = PolygonTriangulator.contains(
        outer = outer.toTriangulationRing(),
        holes = holes.map { it.toTriangulationRing() },
        point = TriangulationPoint(x.toDouble(), y.toDouble())
    )

    companion object {
        fun from(polygon: EncGeometry.Polygon, projector: GeometryProjector): ProjectedPolygonClip? {
            if (polygon.outer.size < 3) return null
            val projectedOuter = polygon.outer.map(projector::project)
            val projectedHoles = polygon.holes
                .map { hole -> hole.map(projector::project) }
                .filter { it.size >= 3 }
            return ProjectedPolygonClip(projectedOuter, projectedHoles)
        }
    }
}

private fun List<ClipPoint>.toTriangulationRing(): List<TriangulationPoint> =
    map { TriangulationPoint(it.x.toDouble(), it.y.toDouble()) }
