package io.github.s52.core.geometry

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Deterministic polygon decomposition for renderer backends.
 *
 * The implementation uses a vertical trapezoid decomposition and emits two
 * triangles per non-empty trapezoid. Unlike the old renderer fan from vertex 0,
 * this handles concave outer rings and holes using even/odd ring containment.
 * It is intentionally dependency-free so the same behavior is available to JVM
 * tests and Kotlin/JS renderers.
 */
data class TriangulationPoint(val x: Double, val y: Double)

data class TriangulationTriangle(
    val a: TriangulationPoint,
    val b: TriangulationPoint,
    val c: TriangulationPoint
)

object PolygonTriangulator {
    private const val EPSILON: Double = 1.0e-9

    fun triangulate(
        outer: List<TriangulationPoint>,
        holes: List<List<TriangulationPoint>> = emptyList()
    ): List<TriangulationTriangle> {
        val cleanOuter = cleanRing(outer)
        if (cleanOuter.size < 3 || abs(signedArea(cleanOuter)) <= EPSILON) return emptyList()

        val rings = buildList {
            add(cleanOuter)
            holes.map(::cleanRing)
                .filter { it.size >= 3 && abs(signedArea(it)) > EPSILON }
                .forEach(::add)
        }
        val xs = distinctSortedX(rings)
        if (xs.size < 2) return emptyList()

        val triangles = mutableListOf<TriangulationTriangle>()
        for (index in 0 until xs.lastIndex) {
            val x0 = xs[index]
            val x1 = xs[index + 1]
            val width = x1 - x0
            if (width <= EPSILON) continue

            val probeMargin = max(width * 1.0e-7, EPSILON)
            val leftIntervals = intervalsAtX(rings, x0 + probeMargin)
            val rightIntervals = intervalsAtX(rings, x1 - probeMargin)
            val intervalCount = min(leftIntervals.size, rightIntervals.size)

            for (intervalIndex in 0 until intervalCount) {
                val left = leftIntervals[intervalIndex]
                val right = rightIntervals[intervalIndex]
                if (left.height <= EPSILON && right.height <= EPSILON) continue

                val a = TriangulationPoint(x0, left.lower)
                val b = TriangulationPoint(x1, right.lower)
                val c = TriangulationPoint(x1, right.upper)
                val d = TriangulationPoint(x0, left.upper)
                appendIfNonDegenerate(triangles, a, b, c)
                appendIfNonDegenerate(triangles, a, c, d)
            }
        }
        return triangles
    }

    fun contains(
        outer: List<TriangulationPoint>,
        holes: List<List<TriangulationPoint>> = emptyList(),
        point: TriangulationPoint
    ): Boolean {
        val cleanOuter = cleanRing(outer)
        if (cleanOuter.size < 3) return false
        var inside = pointInRing(cleanOuter, point)
        for (hole in holes.map(::cleanRing).filter { it.size >= 3 }) {
            if (pointInRing(hole, point)) inside = !inside
        }
        return inside
    }

    private fun appendIfNonDegenerate(
        out: MutableList<TriangulationTriangle>,
        a: TriangulationPoint,
        b: TriangulationPoint,
        c: TriangulationPoint
    ) {
        if (abs(cross(a, b, c)) > EPSILON) out += TriangulationTriangle(a, b, c)
    }

    private fun intervalsAtX(rings: List<List<TriangulationPoint>>, x: Double): List<Interval> {
        val ys = mutableListOf<Double>()
        for (ring in rings) {
            var previous = ring.last()
            for (current in ring) {
                val minX = min(previous.x, current.x)
                val maxX = max(previous.x, current.x)
                if (maxX - minX > EPSILON && x >= minX && x < maxX) {
                    val t = (x - previous.x) / (current.x - previous.x)
                    ys += previous.y + t * (current.y - previous.y)
                }
                previous = current
            }
        }

        if (ys.size < 2) return emptyList()
        ys.sort()
        val intervals = mutableListOf<Interval>()
        var i = 0
        while (i + 1 < ys.size) {
            val lower = ys[i]
            val upper = ys[i + 1]
            if (upper - lower > EPSILON) intervals += Interval(lower, upper)
            i += 2
        }
        return intervals
    }

    private fun pointInRing(ring: List<TriangulationPoint>, point: TriangulationPoint): Boolean {
        var inside = false
        var previous = ring.last()
        for (current in ring) {
            if (pointOnSegment(previous, current, point)) return true
            val crosses = (current.y > point.y) != (previous.y > point.y)
            if (crosses) {
                val xAtY = (previous.x - current.x) * (point.y - current.y) /
                    ((previous.y - current.y).takeIf { abs(it) > EPSILON } ?: EPSILON) + current.x
                if (point.x < xAtY) inside = !inside
            }
            previous = current
        }
        return inside
    }

    private fun pointOnSegment(a: TriangulationPoint, b: TriangulationPoint, p: TriangulationPoint): Boolean {
        if (abs(cross(a, b, p)) > EPSILON) return false
        return p.x >= min(a.x, b.x) - EPSILON && p.x <= max(a.x, b.x) + EPSILON &&
            p.y >= min(a.y, b.y) - EPSILON && p.y <= max(a.y, b.y) + EPSILON
    }

    private fun cleanRing(ring: List<TriangulationPoint>): List<TriangulationPoint> {
        val finite = ring.filter { it.x.isFinite() && it.y.isFinite() }
        if (finite.isEmpty()) return emptyList()

        val cleaned = mutableListOf<TriangulationPoint>()
        for (point in finite) {
            if (cleaned.lastOrNull()?.nearlyEquals(point) != true) cleaned += point
        }
        if (cleaned.size > 1 && cleaned.first().nearlyEquals(cleaned.last())) cleaned.removeAt(cleaned.lastIndex)
        return cleaned
    }

    private fun distinctSortedX(rings: List<List<TriangulationPoint>>): List<Double> {
        val sorted = rings.flatten().map { it.x }.filter { it.isFinite() }.sorted()
        val result = mutableListOf<Double>()
        for (x in sorted) {
            if (result.lastOrNull()?.let { abs(it - x) <= EPSILON } != true) result += x
        }
        return result
    }

    private fun signedArea(ring: List<TriangulationPoint>): Double {
        var area = 0.0
        var previous = ring.last()
        for (current in ring) {
            area += previous.x * current.y - current.x * previous.y
            previous = current
        }
        return area * 0.5
    }

    private fun cross(a: TriangulationPoint, b: TriangulationPoint, c: TriangulationPoint): Double =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    private fun TriangulationPoint.nearlyEquals(other: TriangulationPoint): Boolean =
        abs(x - other.x) <= EPSILON && abs(y - other.y) <= EPSILON

    private data class Interval(val lower: Double, val upper: Double) {
        val height: Double get() = upper - lower
    }
}
