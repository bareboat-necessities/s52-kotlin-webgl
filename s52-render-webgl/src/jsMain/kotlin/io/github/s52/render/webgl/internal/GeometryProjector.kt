package io.github.s52.render.webgl.internal

import io.github.s52.core.geometry.Coordinate
import io.github.s52.render.webgl.RenderViewport
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class ClipPoint(val x: Float, val y: Float)

internal data class ScissorRect(val x: Int, val y: Int, val width: Int, val height: Int)

internal class GeometryProjector(
    private val viewport: RenderViewport,
    private val canvasWidth: Int,
    private val canvasHeight: Int
) {
    private val width = max(viewport.east - viewport.west, 1e-9)
    private val height = max(viewport.north - viewport.south, 1e-9)

    fun project(coordinate: Coordinate): ClipPoint {
        val x01 = (coordinate.lon - viewport.west) / width
        val y01 = (coordinate.lat - viewport.south) / height
        return ClipPoint(
            x = (x01 * 2.0 - 1.0).toFloat(),
            y = (y01 * 2.0 - 1.0).toFloat()
        )
    }

    fun pixelToClipX(px: Double): Float = (2.0 * px / canvasWidth.coerceAtLeast(1)).toFloat()
    fun pixelToClipY(px: Double): Float = (2.0 * px / canvasHeight.coerceAtLeast(1)).toFloat()

    fun scissorFor(bounds: ClipBounds, paddingPx: Int = 2): ScissorRect? {
        val w = canvasWidth.coerceAtLeast(1)
        val h = canvasHeight.coerceAtLeast(1)
        val minX = bounds.minX.coerceIn(-1.0f, 1.0f)
        val maxX = bounds.maxX.coerceIn(-1.0f, 1.0f)
        val minY = bounds.minY.coerceIn(-1.0f, 1.0f)
        val maxY = bounds.maxY.coerceIn(-1.0f, 1.0f)
        if (maxX < minX || maxY < minY) return null

        val x0 = floor(((minX + 1.0f) * 0.5f * w).toDouble()).toInt() - paddingPx
        val x1 = ceil(((maxX + 1.0f) * 0.5f * w).toDouble()).toInt() + paddingPx
        val y0 = floor(((minY + 1.0f) * 0.5f * h).toDouble()).toInt() - paddingPx
        val y1 = ceil(((maxY + 1.0f) * 0.5f * h).toDouble()).toInt() + paddingPx

        val sx = x0.coerceIn(0, w)
        val sy = y0.coerceIn(0, h)
        val ex = x1.coerceIn(0, w)
        val ey = y1.coerceIn(0, h)
        val sw = ex - sx
        val sh = ey - sy
        return if (sw > 0 && sh > 0) ScissorRect(sx, sy, sw, sh) else null
    }

    fun clipLimitX(): Float = 1.0f + pixelToClipX(CLIP_PADDING_PX.toDouble())
    fun clipLimitY(): Float = 1.0f + pixelToClipY(CLIP_PADDING_PX.toDouble())

    private companion object {
        private const val CLIP_PADDING_PX: Int = 4
    }
}

internal fun clipSegmentToViewport(a: ClipPoint, b: ClipPoint, limitX: Float, limitY: Float): Pair<ClipPoint, ClipPoint>? {
    var x0 = a.x
    var y0 = a.y
    var x1 = b.x
    var y1 = b.y
    var code0 = outCode(x0, y0, limitX, limitY)
    var code1 = outCode(x1, y1, limitX, limitY)

    while (true) {
        if ((code0 or code1) == 0) return ClipPoint(x0, y0) to ClipPoint(x1, y1)
        if ((code0 and code1) != 0) return null

        val out = if (code0 != 0) code0 else code1
        var x = 0.0f
        var y = 0.0f
        when {
            (out and OUT_TOP) != 0 -> {
                val denom = y1 - y0
                if (denom == 0.0f) return null
                x = x0 + (x1 - x0) * (limitY - y0) / denom
                y = limitY
            }
            (out and OUT_BOTTOM) != 0 -> {
                val denom = y1 - y0
                if (denom == 0.0f) return null
                x = x0 + (x1 - x0) * (-limitY - y0) / denom
                y = -limitY
            }
            (out and OUT_RIGHT) != 0 -> {
                val denom = x1 - x0
                if (denom == 0.0f) return null
                y = y0 + (y1 - y0) * (limitX - x0) / denom
                x = limitX
            }
            (out and OUT_LEFT) != 0 -> {
                val denom = x1 - x0
                if (denom == 0.0f) return null
                y = y0 + (y1 - y0) * (-limitX - x0) / denom
                x = -limitX
            }
        }

        if (out == code0) {
            x0 = x
            y0 = y
            code0 = outCode(x0, y0, limitX, limitY)
        } else {
            x1 = x
            y1 = y
            code1 = outCode(x1, y1, limitX, limitY)
        }
    }
}

private fun outCode(x: Float, y: Float, limitX: Float, limitY: Float): Int {
    var code = 0
    if (x < -limitX) code = code or OUT_LEFT else if (x > limitX) code = code or OUT_RIGHT
    if (y < -limitY) code = code or OUT_BOTTOM else if (y > limitY) code = code or OUT_TOP
    return code
}

private const val OUT_LEFT: Int = 1
private const val OUT_RIGHT: Int = 2
private const val OUT_BOTTOM: Int = 4
private const val OUT_TOP: Int = 8
