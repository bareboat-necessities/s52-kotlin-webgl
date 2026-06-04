package io.github.s52.render.webgl.internal

import io.github.s52.core.geometry.Coordinate
import io.github.s52.render.webgl.RenderViewport
import kotlin.math.max

internal data class ClipPoint(val x: Float, val y: Float)

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
}
