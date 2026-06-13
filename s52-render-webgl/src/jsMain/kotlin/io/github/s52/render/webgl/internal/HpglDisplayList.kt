package io.github.s52.render.webgl.internal

import io.github.s52.core.geometry.PolygonTriangulator
import io.github.s52.core.geometry.TriangulationPoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class HpglPoint(val x: Double, val y: Double)

internal data class HpglLineSegment(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double
)

internal data class HpglTriangle(
    val a: HpglPoint,
    val b: HpglPoint,
    val c: HpglPoint
)

internal data class HpglGeometry(
    val pen: String?,
    val strokes: List<HpglLineSegment> = emptyList(),
    val fills: List<HpglTriangle> = emptyList()
)

internal data class HpglBounds(
    val minX: Double,
    val minY: Double,
    val maxX: Double,
    val maxY: Double
) {
    val width: Double get() = maxX - minX
    val height: Double get() = maxY - minY
    val centerY: Double get() = (minY + maxY) * 0.5
}

internal data class HpglDisplayList(
    val geometries: List<HpglGeometry>,
    val bounds: HpglBounds?
) {
    val isEmpty: Boolean get() = geometries.all { it.strokes.isEmpty() && it.fills.isEmpty() }

    fun strokeSegments(): List<HpglLineSegment> = geometries.flatMap { it.strokes }

    fun colorTokenForPen(pen: String?, colorRefs: List<String>): String? {
        if (colorRefs.isEmpty()) return null
        val normalized = pen?.trim().orEmpty()
        if (normalized.isEmpty()) return colorRefs.first()

        normalized.toIntOrNull()?.let { oneBased ->
            return colorRefs.getOrNull(oneBased - 1) ?: colorRefs.first()
        }

        if (normalized == "^") return colorRefs.getOrNull(1) ?: colorRefs.first()
        if (normalized.length == 1 && normalized[0] in 'A'..'Z') {
            return colorRefs.getOrNull(normalized[0] - 'A') ?: colorRefs.first()
        }

        return colorRefs.first()
    }
}

/**
 * Compiles the OpenCPN HPGL subset used by chart-symbol assets into a reusable
 * display list.  Rendering code transforms this geometry each frame, but never
 * tokenizes HPGL in the hot path.
 */
internal object HpglDisplayListCompiler {
    fun compile(hpgl: String): HpglDisplayList {
        if (hpgl.isBlank()) return HpglDisplayList(emptyList(), null)

        class GeometryBuilder(
            val pen: String?,
            val strokes: MutableList<HpglLineSegment> = mutableListOf(),
            val fills: MutableList<HpglTriangle> = mutableListOf()
        ) {
            val isEmpty: Boolean get() = strokes.isEmpty() && fills.isEmpty()
            fun toGeometry(): HpglGeometry = HpglGeometry(pen = pen, strokes = strokes.toList(), fills = fills.toList())
        }

        val batches = mutableListOf<GeometryBuilder>()
        var currentPen: String? = null
        var currentBatch = GeometryBuilder(currentPen).also { batches += it }
        var x = 0.0
        var y = 0.0
        var polygonContours: MutableList<MutableList<HpglPoint>>? = null
        var currentContour: MutableList<HpglPoint>? = null
        var collectingPolygon = false

        fun selectPen(rawPen: String) {
            val normalized = rawPen.trim().uppercase().takeIf { it.isNotEmpty() }
            if (normalized == currentPen) return
            currentPen = normalized
            val next = GeometryBuilder(normalized)
            if (currentBatch.isEmpty && batches.isNotEmpty()) {
                batches[batches.lastIndex] = next
            } else {
                batches += next
            }
            currentBatch = next
        }

        fun drawingEnabled(): Boolean = currentPen != NO_PEN
        fun strokes(): MutableList<HpglLineSegment> = currentBatch.strokes
        fun fills(): MutableList<HpglTriangle> = currentBatch.fills
        fun currentPoint(): HpglPoint = HpglPoint(x, y)

        fun beginPolygon() {
            polygonContours = mutableListOf()
            currentContour = mutableListOf(currentPoint()).also { polygonContours?.add(it) }
            collectingPolygon = true
        }

        fun nextPolygonContour() {
            val contours = polygonContours ?: return beginPolygon()
            currentContour = mutableListOf(currentPoint()).also { contours.add(it) }
            collectingPolygon = true
        }

        fun appendPolygonPoint(point: HpglPoint) {
            if (!collectingPolygon) return
            val contour = currentContour ?: return
            if (contour.lastOrNull() != point) contour += point
        }

        fun lineTo(nx: Double, ny: Double) {
            if (!collectingPolygon && drawingEnabled()) {
                strokes() += HpglLineSegment(x, y, nx, ny)
            }
            x = nx
            y = ny
            appendPolygonPoint(currentPoint())
        }

        fun moveTo(nx: Double, ny: Double) {
            x = nx
            y = ny
            if (collectingPolygon && currentContour?.size == 1) {
                currentContour?.set(0, currentPoint())
            }
        }

        fun appendArc(cx: Double, cy: Double, sweep: Double) {
            val radius = hypot(x - cx, y - cy)
            if (radius <= 0.0) return
            val start = kotlin.math.atan2(y - cy, x - cx)
            val steps = maxOf(8, abs(sweep / ARC_DEGREES_PER_STEP).roundToInt())
            var px = x
            var py = y
            for (i in 1..steps) {
                val a = start + (sweep * PI / 180.0) * (i.toDouble() / steps)
                val nx = cx + cos(a) * radius
                val ny = cy + sin(a) * radius
                if (!collectingPolygon && drawingEnabled()) {
                    strokes() += HpglLineSegment(px, py, nx, ny)
                }
                px = nx
                py = ny
                x = nx
                y = ny
                appendPolygonPoint(currentPoint())
            }
        }

        fun appendCircle(radius: Double) {
            if (radius <= 0.0) return
            val cx = x
            val cy = y
            val points = circlePoints(cx, cy, radius)
            if (collectingPolygon) {
                currentContour = points.toMutableList().also { polygonContours?.add(it) }
                return
            }
            if (!drawingEnabled()) return
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                strokes() += HpglLineSegment(a.x, a.y, b.x, b.y)
            }
        }

        fun appendFilledRect(x0: Double, y0: Double, x1: Double, y1: Double) {
            if (!drawingEnabled()) return
            val a = HpglPoint(x0, y0)
            val b = HpglPoint(x1, y0)
            val c = HpglPoint(x1, y1)
            val d = HpglPoint(x0, y1)
            fills() += HpglTriangle(a, b, c)
            fills() += HpglTriangle(a, c, d)
        }

        fun appendRectEdges(x0: Double, y0: Double, x1: Double, y1: Double) {
            if (!drawingEnabled()) return
            strokes() += HpglLineSegment(x0, y0, x1, y0)
            strokes() += HpglLineSegment(x1, y0, x1, y1)
            strokes() += HpglLineSegment(x1, y1, x0, y1)
            strokes() += HpglLineSegment(x0, y1, x0, y0)
        }

        fun appendContourEdges() {
            if (!drawingEnabled()) return
            val contours = polygonContours.orEmpty()
            for (contour in contours) {
                val clean = contour.withoutDuplicateClose().filterFinite()
                if (clean.size < 2) continue
                for (i in clean.indices) {
                    val a = clean[i]
                    val b = clean[(i + 1) % clean.size]
                    strokes() += HpglLineSegment(a.x, a.y, b.x, b.y)
                }
            }
        }

        fun flushPolygonFill() {
            val contours = polygonContours?.map { contour ->
                contour.withoutDuplicateClose().filterFinite()
            }?.filter { it.size >= 3 }.orEmpty()
            if (contours.isNotEmpty() && drawingEnabled()) {
                val outer = contours.first().map { TriangulationPoint(it.x, it.y) }
                val holes = contours.drop(1).map { contour -> contour.map { TriangulationPoint(it.x, it.y) } }
                for (triangle in PolygonTriangulator.triangulate(outer, holes)) {
                    fills() += HpglTriangle(
                        HpglPoint(triangle.a.x, triangle.a.y),
                        HpglPoint(triangle.b.x, triangle.b.y),
                        HpglPoint(triangle.c.x, triangle.c.y)
                    )
                }
            }
            collectingPolygon = false
        }

        for (raw in hpgl.split(';')) {
            val token = raw.trim()
            if (token.length < 2) continue
            val op = token.take(2).uppercase()
            val args = token.drop(2).trim()
            when (op) {
                "SP" -> selectPen(args)
                "PU" -> parsePairs(args).lastOrNull()?.let { moveTo(it.first, it.second) }
                "PD" -> parsePairs(args).forEach { lineTo(it.first, it.second) }
                "CI" -> args.numberPrefixOrNull()?.let { appendCircle(it) }
                "AA" -> parseNumbers(args).takeIf { it.size >= 3 }?.let { appendArc(it[0], it[1], it[2]) }
                "PM" -> when (args.numberPrefixOrNull()?.roundToInt() ?: 0) {
                    0 -> beginPolygon()
                    1 -> nextPolygonContour()
                    2 -> collectingPolygon = false
                    else -> Unit
                }
                "FP" -> flushPolygonFill()
                "EP" -> appendContourEdges()
                "RA" -> parsePairs(args).firstOrNull()?.let { appendFilledRect(x, y, it.first, it.second) }
                "RR" -> parsePairs(args).firstOrNull()?.let { appendFilledRect(x, y, x + it.first, y + it.second) }
                "EA" -> parsePairs(args).firstOrNull()?.let { appendRectEdges(x, y, it.first, it.second) }
                "ER" -> parsePairs(args).firstOrNull()?.let { appendRectEdges(x, y, x + it.first, y + it.second) }
            }
        }

        val geometries = batches.mapNotNull { batch -> if (batch.isEmpty) null else batch.toGeometry() }
        return HpglDisplayList(geometries, bounds(geometries))
    }

    private fun bounds(geometries: List<HpglGeometry>): HpglBounds? {
        val points = sequence {
            for (geometry in geometries) {
                for (segment in geometry.strokes) {
                    yield(HpglPoint(segment.x1, segment.y1))
                    yield(HpglPoint(segment.x2, segment.y2))
                }
                for (triangle in geometry.fills) {
                    yield(triangle.a)
                    yield(triangle.b)
                    yield(triangle.c)
                }
            }
        }.toList()
        if (points.isEmpty()) return null
        var minX = points.first().x
        var maxX = points.first().x
        var minY = points.first().y
        var maxY = points.first().y
        for (point in points) {
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }
        return HpglBounds(minX, minY, maxX, maxY)
    }

    private fun circlePoints(cx: Double, cy: Double, radius: Double): List<HpglPoint> =
        (0 until CIRCLE_STEPS).map { i ->
            val angle = 2.0 * PI * i / CIRCLE_STEPS
            HpglPoint(cx + cos(angle) * radius, cy + sin(angle) * radius)
        }

    private fun List<HpglPoint>.withoutDuplicateClose(): List<HpglPoint> =
        if (size > 1 && first() == last()) dropLast(1) else this

    private fun List<HpglPoint>.filterFinite(): List<HpglPoint> =
        filter { it.x.isFinite() && it.y.isFinite() }

    private fun parsePairs(text: String): List<Pair<Double, Double>> {
        val nums = parseNumbers(text)
        if (nums.size < 2) return emptyList()
        return nums.chunked(2).mapNotNull { pair ->
            if (pair.size == 2) pair[0] to pair[1] else null
        }
    }

    private fun parseNumbers(text: String): List<Double> {
        if (text.isBlank()) return emptyList()
        val out = ArrayList<Double>()
        var start = -1
        fun flush(end: Int) {
            if (start >= 0 && start < end) {
                text.substring(start, end).toDoubleOrNull()?.let(out::add)
            }
            start = -1
        }
        for (i in text.indices) {
            val ch = text[i]
            val numeric = ch.isDigit() || ch == '-' || ch == '+' || ch == '.'
            if (numeric) {
                if (start < 0) start = i
            } else {
                flush(i)
            }
        }
        flush(text.length)
        return out
    }

    private fun String.numberPrefixOrNull(): Double? = parseNumbers(this).firstOrNull()

    private const val NO_PEN: String = "0"
    private const val CIRCLE_STEPS: Int = 32
    private const val ARC_DEGREES_PER_STEP: Double = 12.0
}

/** Backwards-compatible facade for callers that only need line geometry. */
internal object HpglLineParser {
    fun parseSegments(hpgl: String): List<HpglLineSegment> =
        HpglDisplayListCompiler.compile(hpgl).strokeSegments()

    fun bounds(segments: List<HpglLineSegment>): HpglBounds? {
        if (segments.isEmpty()) return null
        var minX = minOf(segments.first().x1, segments.first().x2)
        var maxX = maxOf(segments.first().x1, segments.first().x2)
        var minY = minOf(segments.first().y1, segments.first().y2)
        var maxY = maxOf(segments.first().y1, segments.first().y2)
        for (segment in segments) {
            minX = minOf(minX, segment.x1, segment.x2)
            maxX = maxOf(maxX, segment.x1, segment.x2)
            minY = minOf(minY, segment.y1, segment.y2)
            maxY = maxOf(maxY, segment.y1, segment.y2)
        }
        return HpglBounds(minX, minY, maxX, maxY)
    }
}
