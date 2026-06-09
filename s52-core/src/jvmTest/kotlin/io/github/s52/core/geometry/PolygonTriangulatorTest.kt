package io.github.s52.core.geometry

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolygonTriangulatorTest {
    @Test
    fun triangulatesConcavePolygonWithoutFillingNotch() {
        val outer = ring(0.0 to 0.0, 3.0 to 0.0, 3.0 to 1.0, 1.0 to 1.0, 1.0 to 3.0, 0.0 to 3.0)
        val triangles = PolygonTriangulator.triangulate(outer)

        assertTrue(triangles.isNotEmpty())
        assertEquals(5.0, triangles.area(), absoluteTolerance = 1.0e-6)
        assertTrue(PolygonTriangulator.contains(outer, point = p(0.5, 2.0)))
        assertFalse(PolygonTriangulator.contains(outer, point = p(2.0, 2.0)))
        assertFalse(triangles.any { it.contains(p(2.0, 2.0)) }, "Concave notch must not be covered by generated triangles")
    }

    @Test
    fun triangulatesPolygonWithHoleWithoutFillingHole() {
        val outer = ring(0.0 to 0.0, 4.0 to 0.0, 4.0 to 4.0, 0.0 to 4.0)
        val hole = ring(1.0 to 1.0, 3.0 to 1.0, 3.0 to 3.0, 1.0 to 3.0)
        val triangles = PolygonTriangulator.triangulate(outer, listOf(hole))

        assertTrue(triangles.isNotEmpty())
        assertEquals(12.0, triangles.area(), absoluteTolerance = 1.0e-6)
        assertTrue(PolygonTriangulator.contains(outer, listOf(hole), p(0.5, 0.5)))
        assertFalse(PolygonTriangulator.contains(outer, listOf(hole), p(2.0, 2.0)))
        assertFalse(triangles.any { it.contains(p(2.0, 2.0)) }, "Hole must not be covered by generated triangles")
    }

    private fun ring(vararg values: Pair<Double, Double>): List<TriangulationPoint> =
        values.map { p(it.first, it.second) }

    private fun p(x: Double, y: Double): TriangulationPoint = TriangulationPoint(x, y)

    private fun List<TriangulationTriangle>.area(): Double = sumOf { triangle ->
        abs(
            (triangle.a.x * (triangle.b.y - triangle.c.y) +
                triangle.b.x * (triangle.c.y - triangle.a.y) +
                triangle.c.x * (triangle.a.y - triangle.b.y)) * 0.5
        )
    }

    private fun TriangulationTriangle.contains(point: TriangulationPoint): Boolean {
        val area = listOf(this).area()
        val a1 = abs(cross(point, b, c)) * 0.5
        val a2 = abs(cross(a, point, c)) * 0.5
        val a3 = abs(cross(a, b, point)) * 0.5
        return abs(area - (a1 + a2 + a3)) <= 1.0e-6
    }

    private fun cross(a: TriangulationPoint, b: TriangulationPoint, c: TriangulationPoint): Double =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
}
