package io.github.s52.preslib.esri.svg

import kotlin.math.pow

/** Converts the supported SVG path subset into closed/open polylines in source coordinates. */
object EsriSvgPathFlattener {
    fun flatten(pathData: EsriSvgPathData, curveSteps: Int = 10): List<List<EsriPoint>> {
        val subpaths = mutableListOf<MutableList<EsriPoint>>()
        var current = EsriPoint(0.0, 0.0)
        var start = EsriPoint(0.0, 0.0)
        var lastCubicControl: EsriPoint? = null
        var lastQuadraticControl: EsriPoint? = null

        fun active(): MutableList<EsriPoint> {
            if (subpaths.isEmpty()) subpaths.add(mutableListOf())
            return subpaths.last()
        }

        fun moveTo(point: EsriPoint) {
            subpaths.add(mutableListOf(point))
            current = point
            start = point
            lastCubicControl = null
            lastQuadraticControl = null
        }

        fun lineTo(point: EsriPoint) {
            active().add(point)
            current = point
            lastCubicControl = null
            lastQuadraticControl = null
        }

        fun absolute(relative: Boolean, x: Double, y: Double): EsriPoint = if (relative) {
            EsriPoint(current.x + x, current.y + y)
        } else {
            EsriPoint(x, y)
        }

        for (command in pathData.commands) {
            val v = command.values
            when (command.command) {
                'M' -> {
                    var index = 0
                    if (v.size >= 2) {
                        moveTo(absolute(command.relative, v[0], v[1]))
                        index = 2
                    }
                    while (index + 1 < v.size) {
                        lineTo(absolute(command.relative, v[index], v[index + 1]))
                        index += 2
                    }
                }
                'L' -> {
                    var index = 0
                    while (index + 1 < v.size) {
                        lineTo(absolute(command.relative, v[index], v[index + 1]))
                        index += 2
                    }
                }
                'H' -> {
                    for (x in v) lineTo(if (command.relative) EsriPoint(current.x + x, current.y) else EsriPoint(x, current.y))
                }
                'V' -> {
                    for (y in v) lineTo(if (command.relative) EsriPoint(current.x, current.y + y) else EsriPoint(current.x, y))
                }
                'C' -> {
                    var index = 0
                    while (index + 5 < v.size) {
                        val c1 = absolute(command.relative, v[index], v[index + 1])
                        val c2 = absolute(command.relative, v[index + 2], v[index + 3])
                        val end = absolute(command.relative, v[index + 4], v[index + 5])
                        appendCubic(active(), current, c1, c2, end, curveSteps)
                        current = end
                        lastCubicControl = c2
                        lastQuadraticControl = null
                        index += 6
                    }
                }
                'S' -> {
                    var index = 0
                    while (index + 3 < v.size) {
                        val c1 = lastCubicControl?.let { reflect(it, current) } ?: current
                        val c2 = absolute(command.relative, v[index], v[index + 1])
                        val end = absolute(command.relative, v[index + 2], v[index + 3])
                        appendCubic(active(), current, c1, c2, end, curveSteps)
                        current = end
                        lastCubicControl = c2
                        lastQuadraticControl = null
                        index += 4
                    }
                }
                'Q' -> {
                    var index = 0
                    while (index + 3 < v.size) {
                        val c = absolute(command.relative, v[index], v[index + 1])
                        val end = absolute(command.relative, v[index + 2], v[index + 3])
                        appendQuadratic(active(), current, c, end, curveSteps)
                        current = end
                        lastQuadraticControl = c
                        lastCubicControl = null
                        index += 4
                    }
                }
                'T' -> {
                    var index = 0
                    while (index + 1 < v.size) {
                        val c = lastQuadraticControl?.let { reflect(it, current) } ?: current
                        val end = absolute(command.relative, v[index], v[index + 1])
                        appendQuadratic(active(), current, c, end, curveSteps)
                        current = end
                        lastQuadraticControl = c
                        lastCubicControl = null
                        index += 2
                    }
                }
                'Z' -> {
                    if (active().lastOrNull() != start) active().add(start)
                    current = start
                    lastCubicControl = null
                    lastQuadraticControl = null
                }
            }
        }
        return subpaths.filter { it.size >= 2 }
    }

    private fun reflect(point: EsriPoint, around: EsriPoint): EsriPoint =
        EsriPoint(2.0 * around.x - point.x, 2.0 * around.y - point.y)

    private fun appendCubic(out: MutableList<EsriPoint>, p0: EsriPoint, p1: EsriPoint, p2: EsriPoint, p3: EsriPoint, steps: Int) {
        for (i in 1..steps.coerceAtLeast(1)) {
            val t = i.toDouble() / steps.coerceAtLeast(1)
            val u = 1.0 - t
            out.add(EsriPoint(
                u.pow(3) * p0.x + 3.0 * u.pow(2) * t * p1.x + 3.0 * u * t.pow(2) * p2.x + t.pow(3) * p3.x,
                u.pow(3) * p0.y + 3.0 * u.pow(2) * t * p1.y + 3.0 * u * t.pow(2) * p2.y + t.pow(3) * p3.y
            ))
        }
    }

    private fun appendQuadratic(out: MutableList<EsriPoint>, p0: EsriPoint, p1: EsriPoint, p2: EsriPoint, steps: Int) {
        for (i in 1..steps.coerceAtLeast(1)) {
            val t = i.toDouble() / steps.coerceAtLeast(1)
            val u = 1.0 - t
            out.add(EsriPoint(
                u * u * p0.x + 2.0 * u * t * p1.x + t * t * p2.x,
                u * u * p0.y + 2.0 * u * t * p1.y + t * t * p2.y
            ))
        }
    }
}
