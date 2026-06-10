package io.github.s52.render.webgl.esri

import io.github.s52.preslib.esri.vector.EsriVectorAreaPattern

/**
 * Phase ESRI-9 renderer for generated ESRI vector area-pattern SVG meshes.
 *
 * The first phase tiles a screen-space bounding box. The surrounding chart
 * renderer should enable polygon stencil/clipping before calling this method;
 * Phase ESRI-10 can wire that directly into the polygon fill pipeline.
 */
class EsriVectorPatternRenderer(
    gl: dynamic,
    program: dynamic
) {
    private val painter = EsriWebGlMeshPainter(gl, program)

    fun drawPatternBox(
        pattern: EsriVectorAreaPattern,
        minX: Double,
        minY: Double,
        maxX: Double,
        maxY: Double,
        pixelsPerMm: Double,
        viewportWidth: Double,
        viewportHeight: Double,
        palette: Map<String, String> = emptyMap()
    ) {
        if (!pattern.isRenderable || pixelsPerMm <= 0.0) return
        val tileW = (pattern.tileWidthMm * pixelsPerMm).coerceAtLeast(1.0)
        val tileH = (pattern.tileHeightMm * pixelsPerMm).coerceAtLeast(1.0)
        var y = minY - positiveModulo(minY, tileH)
        while (y <= maxY + tileH) {
            var x = minX - positiveModulo(minX, tileW)
            while (x <= maxX + tileW) {
                drawTile(pattern, x, y, pixelsPerMm, viewportWidth, viewportHeight, palette)
                x += tileW
            }
            y += tileH
        }
    }

    private fun drawTile(
        pattern: EsriVectorAreaPattern,
        tileX: Double,
        tileY: Double,
        pixelsPerMm: Double,
        viewportWidth: Double,
        viewportHeight: Double,
        palette: Map<String, String>
    ) {
        val originX = pattern.viewBox.minX.toDouble()
        val originY = pattern.viewBox.minY.toDouble()
        for (mesh in pattern.meshes) {
            if (!mesh.isRenderable) continue
            val color = EsriPaintResolver.resolve(mesh.paint, palette) ?: continue
            val transformed = mesh.transformedVertices({ x, y ->
                EsriScreenPoint(
                    tileX + (x - originX) * pixelsPerMm,
                    tileY + (y - originY) * pixelsPerMm
                )
            }, viewportWidth, viewportHeight)
            painter.drawMesh(transformed, mesh.indices, color)
        }
    }

    private fun positiveModulo(value: Double, modulus: Double): Double {
        val r = value % modulus
        return if (r < 0.0) r + modulus else r
    }
}
