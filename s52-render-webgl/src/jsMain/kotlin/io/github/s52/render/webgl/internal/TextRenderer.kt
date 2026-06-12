package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import org.khronos.webgl.WebGLRenderingContext

internal class TextRenderer(
    private val gl: WebGLRenderingContext,
    private val program: SolidColorProgram
) {
    private val occupiedLabels = ArrayList<LabelBounds>()

    fun beginFrame() {
        occupiedLabels.clear()
    }

    fun renderText(command: S52DrawCommand.Text, projector: GeometryProjector, colors: ColorResolver): Int {
        val anchor = anchor(command.geometry, projector) ?: return 0
        val text = command.textExpression.ifBlank { command.rawArgs.firstOrNull().orEmpty() }
        val layout = LineGlyphFont.lineLayout(text, anchor, projector, pixelSize = 10.5, maxChars = 48)
        if (layout.isEmpty()) return 0
        if (!claimLabel(layout.bounds, projector, paddingPx = 3.0)) return 0

        val foreground = colors.resolve(command.colorToken, fallback = "CHBLK")
        val halo = colors.resolve(haloTokenFor(foreground), fallback = "CHWHT")
        var calls = 0

        // Draw a one-pixel light/dark shadow plus a wider pass.  Browser WebGL
        // lineWidth support varies, but this still improves readability where
        // supported and remains harmless where line width is clamped to 1 px.
        gl.lineWidth(3.0f)
        calls += program.draw(
            WebGLRenderingContext.LINES,
            LineGlyphFont.offsetVertices(
                layout.vertices,
                projector.pixelToClipX(1.0),
                -projector.pixelToClipY(1.0)
            ),
            halo
        )
        gl.lineWidth(1.0f)
        calls += program.draw(WebGLRenderingContext.LINES, layout.vertices, foreground)
        return calls
    }

    fun renderSounding(command: S52DrawCommand.Sounding, projector: GeometryProjector, colors: ColorResolver): Int {
        val anchor = anchor(command.geometry, projector) ?: return 0
        val layout = LineGlyphFont.soundingLayout(command.depthLabel, anchor, projector)
        if (layout.isEmpty()) return 0
        if (!claimLabel(layout.bounds, projector, paddingPx = 2.0)) return 0

        val foreground = colors.resolve(command.colorToken, fallback = "SNDG1")
        val halo = colors.resolve(haloTokenFor(foreground), fallback = "CHWHT")
        var calls = 0

        gl.lineWidth(3.0f)
        calls += program.draw(
            WebGLRenderingContext.LINES,
            LineGlyphFont.offsetVertices(
                layout.vertices,
                projector.pixelToClipX(0.75),
                -projector.pixelToClipY(0.75)
            ),
            halo
        )
        gl.lineWidth(1.0f)
        calls += program.draw(WebGLRenderingContext.LINES, layout.vertices, foreground)
        return calls
    }

    private fun claimLabel(bounds: LabelBounds, projector: GeometryProjector, paddingPx: Double): Boolean {
        if (bounds.isOutside()) return false
        val padded = bounds.expanded(
            dx = projector.pixelToClipX(paddingPx),
            dy = projector.pixelToClipY(paddingPx)
        )
        for (occupied in occupiedLabels) {
            if (padded.intersects(occupied)) return false
        }
        occupiedLabels += padded
        return true
    }

    private fun haloTokenFor(foreground: GlColor): String {
        val luminance = 0.2126f * foreground.r + 0.7152f * foreground.g + 0.0722f * foreground.b
        return if (luminance < 0.45f) "CHWHT" else "CHBLK"
    }

    private fun anchor(geometry: EncGeometry, projector: GeometryProjector): ClipPoint? = when (geometry) {
        is EncGeometry.Point -> projector.project(geometry.coordinate)
        is EncGeometry.MultiPoint -> geometry.coordinates.firstOrNull()?.let(projector::project)
        is EncGeometry.LineString -> lineAnchor(geometry.coordinates, projector)
        is EncGeometry.Polygon -> polygonAnchor(geometry.outer, projector)
    }

    private fun lineAnchor(coordinates: List<Coordinate>, projector: GeometryProjector): ClipPoint? {
        if (coordinates.isEmpty()) return null
        if (coordinates.size == 1) return projector.project(coordinates[0])

        val projected = ArrayList<ClipPoint>(coordinates.size)
        for (coordinate in coordinates) projected += projector.project(coordinate)

        var total = 0.0
        for (i in 0 until projected.lastIndex) {
            total += distance(projected[i], projected[i + 1])
        }
        if (total <= 1e-9) return projected[projected.size / 2]

        val half = total * 0.5
        var acc = 0.0
        for (i in 0 until projected.lastIndex) {
            val a = projected[i]
            val b = projected[i + 1]
            val segment = distance(a, b)
            if (acc + segment >= half && segment > 1e-9) {
                val t = ((half - acc) / segment).coerceIn(0.0, 1.0)
                return ClipPoint(
                    x = (a.x + (b.x - a.x) * t).toFloat(),
                    y = (a.y + (b.y - a.y) * t).toFloat()
                )
            }
            acc += segment
        }
        return projected.last()
    }

    private fun polygonAnchor(outer: List<Coordinate>, projector: GeometryProjector): ClipPoint? {
        if (outer.isEmpty()) return null
        if (outer.size == 1) return projector.project(outer[0])

        val projected = ArrayList<ClipPoint>(outer.size)
        for (coordinate in outer) projected += projector.project(coordinate)

        var twiceArea = 0.0
        var cx = 0.0
        var cy = 0.0
        for (i in projected.indices) {
            val a = projected[i]
            val b = projected[(i + 1) % projected.size]
            val cross = a.x.toDouble() * b.y.toDouble() - b.x.toDouble() * a.y.toDouble()
            twiceArea += cross
            cx += (a.x + b.x).toDouble() * cross
            cy += (a.y + b.y).toDouble() * cross
        }
        if (abs(twiceArea) > 1e-9) {
            val factor = 1.0 / (3.0 * twiceArea)
            return ClipPoint((cx * factor).toFloat(), (cy * factor).toFloat())
        }

        var minX = projected.first().x
        var maxX = minX
        var minY = projected.first().y
        var maxY = minY
        for (point in projected) {
            minX = min(minX, point.x)
            maxX = max(maxX, point.x)
            minY = min(minY, point.y)
            maxY = max(maxY, point.y)
        }
        return ClipPoint((minX + maxX) * 0.5f, (minY + maxY) * 0.5f)
    }

    private fun distance(a: ClipPoint, b: ClipPoint): Double =
        hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble())
}
