package io.github.s52.render.webgl.esri

import io.github.s52.preslib.esri.vector.EsriMesh
import io.github.s52.preslib.esri.vector.EsriPaint
import io.github.s52.preslib.esri.vector.EsriVectorSymbol
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * WebGL renderer for generated ESRI vector symbol meshes.
 *
 * This renderer deliberately consumes generated Kotlin mesh data, not runtime SVG
 * files.  It is intentionally small and dependency-light so it can be integrated
 * into the existing S-52 WebGL renderer once the ESRI profile starts emitting
 * PointSymbol commands.
 */
class EsriVectorSymbolRenderer(
    private val gl: dynamic,
    private val program: dynamic
) {
    private val cache = EsriMeshBufferCache(gl)

    /**
     * Draws one generated symbol centered at screen pixel (screenX, screenY).
     *
     * @param scale converts SVG viewBox units to screen pixels.
     * @param rotationDegrees clockwise symbol rotation in screen coordinates.
     * @param viewportWidth canvas width in pixels.
     * @param viewportHeight canvas height in pixels.
     */
    fun drawSymbol(
        symbol: EsriVectorSymbol,
        screenX: Double,
        screenY: Double,
        scale: Double,
        rotationDegrees: Double = 0.0,
        viewportWidth: Double,
        viewportHeight: Double,
        palette: Map<String, String> = emptyMap()
    ) {
        if (!symbol.isRenderable) return
        val transform = EsriScreenTransform(
            viewBoxMinX = symbol.viewBox.minX.toDouble(),
            viewBoxMinY = symbol.viewBox.minY.toDouble(),
            viewBoxWidth = symbol.viewBox.width.toDouble(),
            viewBoxHeight = symbol.viewBox.height.toDouble(),
            screenX = screenX,
            screenY = screenY,
            scale = scale,
            rotationRadians = rotationDegrees * PI / 180.0,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        )
        for (mesh in symbol.meshes) {
            if (!mesh.isRenderable) continue
            val color = EsriPaintResolver.resolve(mesh.paint, palette) ?: continue
            val transformed = transform.vertices(mesh.vertices)
            drawMesh(transformed, mesh.indices, color)
        }
    }

    private fun drawMesh(vertices: FloatArray, indices: ShortArray, color: FloatArray) {
        val buffers = cache.upload(vertices, indices)
        gl.useProgram(program)
        val positionLocation = gl.getAttribLocation(program, "a_position")
        if (positionLocation >= 0) {
            gl.bindBuffer(gl.ARRAY_BUFFER, buffers.vertexBuffer)
            gl.enableVertexAttribArray(positionLocation)
            gl.vertexAttribPointer(positionLocation, 2, gl.FLOAT, false, 0, 0)
        }
        val colorLocation = gl.getUniformLocation(program, "u_color")
        if (colorLocation != null) gl.uniform4f(colorLocation, color[0], color[1], color[2], color[3])
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, buffers.indexBuffer)
        gl.drawElements(gl.TRIANGLES, indices.size, gl.UNSIGNED_SHORT, 0)
    }
}

private data class EsriScreenTransform(
    val viewBoxMinX: Double,
    val viewBoxMinY: Double,
    val viewBoxWidth: Double,
    val viewBoxHeight: Double,
    val screenX: Double,
    val screenY: Double,
    val scale: Double,
    val rotationRadians: Double,
    val viewportWidth: Double,
    val viewportHeight: Double
) {
    private val centerX = viewBoxMinX + viewBoxWidth / 2.0
    private val centerY = viewBoxMinY + viewBoxHeight / 2.0
    private val c = cos(rotationRadians)
    private val s = sin(rotationRadians)

    fun vertices(source: FloatArray): FloatArray {
        val out = FloatArray(source.size)
        var i = 0
        while (i + 1 < source.size) {
            val sx = (source[i].toDouble() - centerX) * scale
            val sy = (source[i + 1].toDouble() - centerY) * scale
            val rx = sx * c - sy * s
            val ry = sx * s + sy * c
            val px = screenX + rx
            val py = screenY + ry
            out[i] = ((px / viewportWidth) * 2.0 - 1.0).toFloat()
            out[i + 1] = (1.0 - (py / viewportHeight) * 2.0).toFloat()
            i += 2
        }
        return out
    }
}

private class EsriMeshBufferCache(private val gl: dynamic) {
    fun upload(vertices: FloatArray, indices: ShortArray): EsriGlBuffers {
        val vertexBuffer = gl.createBuffer()
        gl.bindBuffer(gl.ARRAY_BUFFER, vertexBuffer)
        gl.bufferData(gl.ARRAY_BUFFER, vertices, gl.STREAM_DRAW)
        val indexBuffer = gl.createBuffer()
        gl.bindBuffer(gl.ELEMENT_ARRAY_BUFFER, indexBuffer)
        gl.bufferData(gl.ELEMENT_ARRAY_BUFFER, indices, gl.STREAM_DRAW)
        return EsriGlBuffers(vertexBuffer, indexBuffer)
    }
}

private data class EsriGlBuffers(val vertexBuffer: dynamic, val indexBuffer: dynamic)

object EsriPaintResolver {
    fun resolve(paint: EsriPaint, palette: Map<String, String> = emptyMap()): FloatArray? = when (paint) {
        is EsriPaint.Token -> parseColor(palette[paint.token] ?: defaultTokenColor(paint.token))
        is EsriPaint.LiteralHex -> parseColor(paint.hex)
        EsriPaint.None -> null
    }

    private fun defaultTokenColor(token: String): String = when (token.uppercase()) {
        "CHWHT" -> "#ffffff"
        "CHRED" -> "#d40000"
        "CHGRN" -> "#008000"
        "CHYLW" -> "#e0c000"
        "CHMGD", "CHMGF" -> "#c000c0"
        else -> "#231f20"
    }

    private fun parseColor(raw: String): FloatArray {
        val hex = raw.trim().removePrefix("#")
        val expanded = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        val r = expanded.substringOrNull(0, 2)?.toIntOrNull(16) ?: 0
        val g = expanded.substringOrNull(2, 4)?.toIntOrNull(16) ?: 0
        val b = expanded.substringOrNull(4, 6)?.toIntOrNull(16) ?: 0
        return floatArrayOf(r / 255f, g / 255f, b / 255f, 1f)
    }

    private fun String.substringOrNull(start: Int, end: Int): String? =
        if (length >= end) substring(start, end) else null
}
