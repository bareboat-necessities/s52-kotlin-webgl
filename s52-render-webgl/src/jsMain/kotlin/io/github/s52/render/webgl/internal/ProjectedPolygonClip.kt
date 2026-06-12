package io.github.s52.render.webgl.internal

import io.github.s52.core.geometry.EncGeometry
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal data class ProjectedPolygonClip(
    val outer: List<ClipPoint>,
    val holes: List<List<ClipPoint>>
) {
    val bounds: ClipBounds? = ClipBounds.of(outer, holes)

    fun contains(x: Float, y: Float): Boolean {
        if (outer.size < 3) return false
        var inside = pointInRing(outer, x, y)
        for (hole in holes) {
            if (hole.size >= 3 && pointInRing(hole, x, y)) inside = !inside
        }
        return inside
    }

    fun mayIntersectRect(minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val b = bounds ?: return false
        if (maxX < b.minX || minX > b.maxX || maxY < b.minY || minY > b.maxY) return false

        if (contains(minX, minY) || contains(maxX, minY) || contains(maxX, maxY) || contains(minX, maxY)) {
            return true
        }
        for (point in outer) {
            if (point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY) return true
        }
        for (hole in holes) {
            for (point in hole) {
                if (point.x >= minX && point.x <= maxX && point.y >= minY && point.y <= maxY) return true
            }
        }
        if (ringIntersectsRect(outer, minX, maxX, minY, maxY)) return true
        for (hole in holes) if (ringIntersectsRect(hole, minX, maxX, minY, maxY)) return true
        return false
    }

    companion object {
        fun from(polygon: EncGeometry.Polygon, projector: GeometryProjector): ProjectedPolygonClip? {
            if (polygon.outer.size < 3) return null
            val tolerance = projector.ringSimplifyTolerance()
            val limitX = projector.clipLimitX()
            val limitY = projector.clipLimitY()

            val projectedOuter = polygon.outer
                .map(projector::project)
                .clipRingToRect(limitX, limitY)
                .simplifyRing(tolerance)
            if (projectedOuter.size < 3) return null

            val projectedHoles = polygon.holes
                .map { hole ->
                    hole.map(projector::project)
                        .clipRingToRect(limitX, limitY)
                        .simplifyRing(tolerance)
                }
                .filter { it.size >= 3 }

            return ProjectedPolygonClip(projectedOuter, projectedHoles)
        }
    }
}

internal data class ClipBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
    companion object {
        fun of(outer: List<ClipPoint>, holes: List<List<ClipPoint>> = emptyList()): ClipBounds? {
            if (outer.isEmpty() && holes.all { it.isEmpty() }) return null
            var initialized = false
            var minX = 0.0f
            var maxX = 0.0f
            var minY = 0.0f
            var maxY = 0.0f

            fun include(point: ClipPoint) {
                if (!initialized) {
                    minX = point.x
                    maxX = point.x
                    minY = point.y
                    maxY = point.y
                    initialized = true
                } else {
                    minX = min(minX, point.x)
                    maxX = max(maxX, point.x)
                    minY = min(minY, point.y)
                    maxY = max(maxY, point.y)
                }
            }

            for (point in outer) include(point)
            for (hole in holes) for (point in hole) include(point)
            return if (initialized) ClipBounds(minX, maxX, minY, maxY) else null
        }
    }
}

internal fun GeometryProjector.ringSimplifyTolerance(): Double {
    val px = max(abs(pixelToClipX(0.10).toDouble()), abs(pixelToClipY(0.10).toDouble()))
    return max(px, 1.0e-7)
}

internal fun List<ClipPoint>.simplifyRing(tolerance: Double): List<ClipPoint> {
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


private fun List<ClipPoint>.clipRingToRect(limitX: Float, limitY: Float): List<ClipPoint> {
    if (size < 3) return this
    var out = this
    out = out.clipAgainstBoundary(
        inside = { p -> p.x >= -limitX },
        intersect = { a, b ->
            val dx = b.x - a.x
            if (abs(dx) <= EPSILON_FLOAT) {
                ClipPoint(-limitX, a.y)
            } else {
                val t = (-limitX - a.x) / dx
                ClipPoint(-limitX, a.y + (b.y - a.y) * t)
            }
        }
    )
    if (out.size < 3) return emptyList()
    out = out.clipAgainstBoundary(
        inside = { p -> p.x <= limitX },
        intersect = { a, b ->
            val dx = b.x - a.x
            if (abs(dx) <= EPSILON_FLOAT) {
                ClipPoint(limitX, a.y)
            } else {
                val t = (limitX - a.x) / dx
                ClipPoint(limitX, a.y + (b.y - a.y) * t)
            }
        }
    )
    if (out.size < 3) return emptyList()
    out = out.clipAgainstBoundary(
        inside = { p -> p.y >= -limitY },
        intersect = { a, b ->
            val dy = b.y - a.y
            if (abs(dy) <= EPSILON_FLOAT) {
                ClipPoint(a.x, -limitY)
            } else {
                val t = (-limitY - a.y) / dy
                ClipPoint(a.x + (b.x - a.x) * t, -limitY)
            }
        }
    )
    if (out.size < 3) return emptyList()
    out = out.clipAgainstBoundary(
        inside = { p -> p.y <= limitY },
        intersect = { a, b ->
            val dy = b.y - a.y
            if (abs(dy) <= EPSILON_FLOAT) {
                ClipPoint(a.x, limitY)
            } else {
                val t = (limitY - a.y) / dy
                ClipPoint(a.x + (b.x - a.x) * t, limitY)
            }
        }
    )
    return out
}

private inline fun List<ClipPoint>.clipAgainstBoundary(
    inside: (ClipPoint) -> Boolean,
    intersect: (ClipPoint, ClipPoint) -> ClipPoint
): List<ClipPoint> {
    if (isEmpty()) return emptyList()
    val result = ArrayList<ClipPoint>(size + 4)
    var previous = last()
    var previousInside = inside(previous)
    for (current in this) {
        val currentInside = inside(current)
        when {
            currentInside && previousInside -> result += current
            currentInside && !previousInside -> {
                result += intersect(previous, current)
                result += current
            }
            !currentInside && previousInside -> result += intersect(previous, current)
        }
        previous = current
        previousInside = currentInside
    }
    return result
}

private fun pointInRing(ring: List<ClipPoint>, x: Float, y: Float): Boolean {
    var inside = false
    var previous = ring.last()
    for (current in ring) {
        if (pointOnSegment(previous, current, x, y)) return true
        val crosses = (current.y > y) != (previous.y > y)
        if (crosses) {
            val denom = previous.y - current.y
            if (abs(denom) > EPSILON_FLOAT) {
                val xAtY = (previous.x - current.x) * (y - current.y) / denom + current.x
                if (x < xAtY) inside = !inside
            }
        }
        previous = current
    }
    return inside
}

private fun pointOnSegment(a: ClipPoint, b: ClipPoint, x: Float, y: Float): Boolean {
    val cross = (b.x - a.x) * (y - a.y) - (b.y - a.y) * (x - a.x)
    if (abs(cross) > EPSILON_FLOAT) return false
    return x >= min(a.x, b.x) - EPSILON_FLOAT && x <= max(a.x, b.x) + EPSILON_FLOAT &&
        y >= min(a.y, b.y) - EPSILON_FLOAT && y <= max(a.y, b.y) + EPSILON_FLOAT
}

private fun ringIntersectsRect(ring: List<ClipPoint>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
    if (ring.size < 2) return false
    var previous = ring.last()
    for (current in ring) {
        if (segmentIntersectsRect(previous, current, minX, maxX, minY, maxY)) return true
        previous = current
    }
    return false
}

private fun segmentIntersectsRect(a: ClipPoint, b: ClipPoint, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
    if (a.x >= minX && a.x <= maxX && a.y >= minY && a.y <= maxY) return true
    if (b.x >= minX && b.x <= maxX && b.y >= minY && b.y <= maxY) return true
    val segMinX = min(a.x, b.x)
    val segMaxX = max(a.x, b.x)
    val segMinY = min(a.y, b.y)
    val segMaxY = max(a.y, b.y)
    if (segMaxX < minX || segMinX > maxX || segMaxY < minY || segMinY > maxY) return false
    return segmentsIntersect(a, b, ClipPoint(minX, minY), ClipPoint(maxX, minY)) ||
        segmentsIntersect(a, b, ClipPoint(maxX, minY), ClipPoint(maxX, maxY)) ||
        segmentsIntersect(a, b, ClipPoint(maxX, maxY), ClipPoint(minX, maxY)) ||
        segmentsIntersect(a, b, ClipPoint(minX, maxY), ClipPoint(minX, minY))
}

private fun segmentsIntersect(a: ClipPoint, b: ClipPoint, c: ClipPoint, d: ClipPoint): Boolean {
    val o1 = orientation(a, b, c)
    val o2 = orientation(a, b, d)
    val o3 = orientation(c, d, a)
    val o4 = orientation(c, d, b)
    if (o1 == 0 && pointOnSegment(a, b, c.x, c.y)) return true
    if (o2 == 0 && pointOnSegment(a, b, d.x, d.y)) return true
    if (o3 == 0 && pointOnSegment(c, d, a.x, a.y)) return true
    if (o4 == 0 && pointOnSegment(c, d, b.x, b.y)) return true
    return o1 != o2 && o3 != o4
}

private fun orientation(a: ClipPoint, b: ClipPoint, c: ClipPoint): Int {
    val value = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
    return when {
        abs(value) <= EPSILON_FLOAT -> 0
        value > 0f -> 1
        else -> -1
    }
}

private fun ClipPoint.nearlyEquals(other: ClipPoint, tolerance: Double): Boolean =
    abs(x.toDouble() - other.x.toDouble()) <= tolerance && abs(y.toDouble() - other.y.toDouble()) <= tolerance

private fun collinear(a: ClipPoint, b: ClipPoint, c: ClipPoint, tolerance: Double): Boolean {
    val area2 = (b.x - a.x).toDouble() * (c.y - a.y).toDouble() -
        (b.y - a.y).toDouble() * (c.x - a.x).toDouble()
    return abs(area2) <= tolerance * tolerance
}

private const val EPSILON_FLOAT: Float = 1.0e-6f
