package io.github.s52.render.webgl.esri

import io.github.s52.preslib.esri.vector.EsriVectorLineStyle
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Renderer for generated ESRI complex-line SVG meshes.
 *
 * It places the generated line SVG as a repeated vector motif along a screen-space
 * polyline. Geometry is passed in screen pixels by the higher chart renderer.
 */
class EsriVectorLineRenderer(
    gl: dynamic,
    program: dynamic
) {
    private val painter = EsriWebGlMeshPainter(gl, program)

    fun drawLineStyle(
        lineStyle: EsriVectorLineStyle,
        polyline: List<Pair<Double, Double>>,
        pixelsPerMm: Double,
        viewportWidth: Double,
        viewportHeight: Double,
        palette: Map<String, String> = emptyMap()
    ) {
        if (!lineStyle.isRenderable || polyline.size < 2 || pixelsPerMm <= 0.0) return
        val points = polyline.map { EsriScreenPoint(it.first, it.second) }
        val repeatPx = (lineStyle.repeatMm * pixelsPerMm).coerceAtLeast(1.0)
        for (segmentIndex in 0 until points.lastIndex) {
            drawSegment(lineStyle, points[segmentIndex], points[segmentIndex + 1], repeatPx, pixelsPerMm, viewportWidth, viewportHeight, palette)
        }
    }

    private fun drawSegment(
        lineStyle: EsriVectorLineStyle,
        a: EsriScreenPoint,
        b: EsriScreenPoint,
        repeatPx: Double,
        pixelsPerMm: Double,
        viewportWidth: Double,
        viewportHeight: Double,
        palette: Map<String, String>
    ) {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len < 1.0e-6) return
        val angle = atan2(dy, dx)
        val c = cos(angle)
        val s = sin(angle)
        var distance = repeatPx / 2.0
        while (distance < len) {
            val centerX = a.x + dx * (distance / len)
            val centerY = a.y + dy * (distance / len)
            drawAt(lineStyle, centerX, centerY, c, s, pixelsPerMm, viewportWidth, viewportHeight, palette)
            distance += repeatPx
        }
    }

    private fun drawAt(
        lineStyle: EsriVectorLineStyle,
        centerX: Double,
        centerY: Double,
        c: Double,
        s: Double,
        pixelsPerMm: Double,
        viewportWidth: Double,
        viewportHeight: Double,
        palette: Map<String, String>
    ) {
        val originX = lineStyle.viewBox.minX + lineStyle.viewBox.width / 2.0
        val originY = lineStyle.viewBox.minY + lineStyle.viewBox.height / 2.0
        for (mesh in lineStyle.meshes) {
            if (!mesh.isRenderable) continue
            val color = EsriPaintResolver.resolve(mesh.paint, palette) ?: continue
            val transformed = mesh.transformedVertices({ x, y ->
                val localX = (x - originX) * pixelsPerMm
                val localY = (y - originY) * pixelsPerMm
                EsriScreenPoint(
                    centerX + localX * c - localY * s,
                    centerY + localX * s + localY * c
                )
            }, viewportWidth, viewportHeight)
            painter.drawMesh(transformed, mesh.indices, color)
        }
    }
}
