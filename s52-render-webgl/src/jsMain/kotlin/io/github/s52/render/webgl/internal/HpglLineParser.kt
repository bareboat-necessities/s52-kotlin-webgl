package io.github.s52.render.webgl.internal

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class HpglLineSegment(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double
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

/**
 * Small HPGL geometry extractor used by the WebGL OpenCPN asset renderers.
 *
 * It intentionally extracts geometry only. Styling is resolved at the asset
 * level from OpenCPN color references until the full styled HPGL renderer lands.
 */
internal object HpglLineParser {
    fun parseSegments(hpgl: String): List<HpglLineSegment> {
        if (hpgl.isBlank()) return emptyList()
        val segments = ArrayList<HpglLineSegment>()
        var x = 0.0
        var y = 0.0

        for (raw in hpgl.split(';')) {
            val token = raw.trim()
            if (token.length < 2) continue
            val op = token.take(2).uppercase()
            val args = token.drop(2).trim()
            when (op) {
                "PU" -> {
                    val points = parsePairs(args)
                    if (points.isNotEmpty()) {
                        x = points.last().first
                        y = points.last().second
                    }
                }
                "PD" -> {
                    val points = parsePairs(args)
                    if (points.isNotEmpty()) {
                        for ((nx, ny) in points) {
                            segments += HpglLineSegment(x, y, nx, ny)
                            x = nx
                            y = ny
                        }
                    }
                }
                "CI" -> {
                    val radius = args.numberPrefixOrNull()
                    if (radius != null && radius > 0.0) {
                        appendCircle(segments, x, y, radius)
                    }
                }
                "AA" -> {
                    val nums = parseNumbers(args)
                    if (nums.size >= 3) {
                        val cx = nums[0]
                        val cy = nums[1]
                        val sweep = nums[2]
                        val start = kotlin.math.atan2(y - cy, x - cx)
                        val radius = kotlin.math.hypot(x - cx, y - cy)
                        if (radius > 0.0) {
                            val steps = maxOf(8, kotlin.math.abs(sweep / 12.0).roundToInt())
                            var px = x
                            var py = y
                            for (i in 1..steps) {
                                val a = start + (sweep * PI / 180.0) * (i.toDouble() / steps)
                                val nx = cx + cos(a) * radius
                                val ny = cy + sin(a) * radius
                                segments += HpglLineSegment(px, py, nx, ny)
                                px = nx
                                py = ny
                            }
                            x = px
                            y = py
                        }
                    }
                }
            }
        }
        return segments
    }

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

    private fun appendCircle(out: MutableList<HpglLineSegment>, cx: Double, cy: Double, radius: Double) {
        val steps = 32
        var px = cx + radius
        var py = cy
        for (i in 1..steps) {
            val a = 2.0 * PI * i / steps
            val nx = cx + cos(a) * radius
            val ny = cy + sin(a) * radius
            out += HpglLineSegment(px, py, nx, ny)
            px = nx
            py = ny
        }
    }

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
}
