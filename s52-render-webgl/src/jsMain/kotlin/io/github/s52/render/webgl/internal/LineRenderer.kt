package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.preslib.LineStyleDefinition
import io.github.s52.preslib.PresLibPack
import org.khronos.webgl.WebGLRenderingContext
import kotlin.math.hypot

internal class LineRenderer(
    private val gl: WebGLRenderingContext,
    private val program: SolidColorProgram,
    private val presLib: PresLibPack
) {
    fun renderSimple(command: S52DrawCommand.LineSimple, projector: GeometryProjector, colors: ColorResolver): Int {
        return drawGeometry(command.geometry, projector, colors.resolve(command.colorToken), lineWidth = command.width)
    }

    fun renderComplex(command: S52DrawCommand.LineComplex, projector: GeometryProjector, colors: ColorResolver): Int {
        val style = presLib.lineStyles.find(command.lineStyleName)
        val hpgl = style?.vectorHpgl
        if (style == null || hpgl.isNullOrBlank()) {
            return drawGeometry(command.geometry, projector, colors.resolve(null, fallback = "CHBLK"), lineWidth = 1.0)
        }

        val segments = HpglLineParser.parseSegments(hpgl)
        val bounds = HpglLineParser.bounds(segments)
        if (segments.isEmpty() || bounds == null) {
            return drawGeometry(command.geometry, projector, colors.resolve(style.colorRefs.firstOrNull(), fallback = "CHBLK"), lineWidth = 1.0)
        }

        val vertices = complexLineVertices(command.geometry, projector, style, segments, bounds)
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.5f)
        return program.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(style.colorRefs.firstOrNull(), fallback = "CHBLK"))
    }

    private fun drawGeometry(geometry: EncGeometry, projector: GeometryProjector, color: GlColor, lineWidth: Double): Int {
        val vertices = when (geometry) {
            is EncGeometry.LineString -> geometry.coordinates.toVertices(projector)
            is EncGeometry.Polygon -> geometry.outer.toVertices(projector)
            else -> FloatArray(0)
        }
        if (vertices.isEmpty()) return 0
        gl.lineWidth(lineWidth.toFloat().coerceAtLeast(1.0f))
        return program.draw(WebGLRenderingContext.LINE_STRIP, vertices, color)
    }

    private fun complexLineVertices(
        geometry: EncGeometry,
        projector: GeometryProjector,
        style: LineStyleDefinition,
        hpglSegments: List<HpglLineSegment>,
        bounds: HpglBounds
    ): FloatArray {
        val coordinates = when (geometry) {
            is EncGeometry.LineString -> geometry.coordinates
            is EncGeometry.Polygon -> geometry.outer
            else -> return FloatArray(0)
        }
        if (coordinates.size < 2) return FloatArray(0)

        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        if (sx <= 0.0 || sy <= 0.0) return FloatArray(0)

        val tileWidthPx = tileWidthPx(style, bounds)
        val tileHeightPx = tileHeightPx(style, bounds)
        val originX = bounds.minX
        val originY = if (style.height > 0.0) style.pivotY else bounds.centerY
        val floats = ArrayList<Float>()

        for (i in 0 until coordinates.lastIndex) {
            val a = projector.project(coordinates[i])
            val b = projector.project(coordinates[i + 1])
            val dxPx = (b.x.toDouble() - a.x.toDouble()) / sx
            val dyPx = -(b.y.toDouble() - a.y.toDouble()) / sy
            val lengthPx = hypot(dxPx, dyPx)
            if (lengthPx < 1.0) continue
            val tx = dxPx / lengthPx
            val ty = dyPx / lengthPx
            val nx = -ty
            val ny = tx
            val stepPx = tileWidthPx.coerceAtLeast(12.0)
            var along = stepPx * 0.5
            var placed = 0
            while (along <= lengthPx && placed < MAX_TILES_PER_SEGMENT) {
                val anchorScreenX = a.x.toDouble() / sx + tx * along
                val anchorScreenY = -a.y.toDouble() / sy + ty * along
                appendLineStyleTile(
                    out = floats,
                    hpglSegments = hpglSegments,
                    originX = originX,
                    originY = originY,
                    tileWidthPx = tileWidthPx,
                    tileHeightPx = tileHeightPx,
                    sx = sx,
                    sy = sy,
                    anchorScreenX = anchorScreenX,
                    anchorScreenY = anchorScreenY,
                    tx = tx,
                    ty = ty,
                    nx = nx,
                    ny = ny
                )
                along += stepPx
                placed++
            }
        }
        return floats.toFloatArray()
    }

    private fun appendLineStyleTile(
        out: MutableList<Float>,
        hpglSegments: List<HpglLineSegment>,
        originX: Double,
        originY: Double,
        tileWidthPx: Double,
        tileHeightPx: Double,
        sx: Double,
        sy: Double,
        anchorScreenX: Double,
        anchorScreenY: Double,
        tx: Double,
        ty: Double,
        nx: Double,
        ny: Double
    ) {
        fun point(x: Double, y: Double): ClipPoint {
            val localX = (x - originX) * HPGL_TO_PIXEL - tileWidthPx * 0.5
            val localY = (y - originY) * HPGL_TO_PIXEL
            val screenX = anchorScreenX + tx * localX + nx * localY
            val screenY = anchorScreenY + ty * localX + ny * localY
            return ClipPoint((screenX * sx).toFloat(), (-screenY * sy).toFloat())
        }
        for (segment in hpglSegments) {
            val a = point(segment.x1, segment.y1)
            val b = point(segment.x2, segment.y2)
            out.add(a.x); out.add(a.y)
            out.add(b.x); out.add(b.y)
        }
    }

    private fun tileWidthPx(style: LineStyleDefinition, bounds: HpglBounds): Double {
        val sourceWidth = when {
            style.width > 0.0 -> style.width
            bounds.width > 0.0 -> bounds.width
            else -> 400.0
        }
        return (sourceWidth * HPGL_TO_PIXEL).coerceIn(12.0, 220.0)
    }

    private fun tileHeightPx(style: LineStyleDefinition, bounds: HpglBounds): Double {
        val sourceHeight = when {
            style.height > 0.0 -> style.height
            bounds.height > 0.0 -> bounds.height
            else -> 200.0
        }
        return (sourceHeight * HPGL_TO_PIXEL).coerceIn(2.0, 80.0)
    }

    private fun List<Coordinate>.toVertices(projector: GeometryProjector): FloatArray {
        val floats = ArrayList<Float>(size * 2)
        for (coordinate in this) {
            val point = projector.project(coordinate)
            floats.add(point.x); floats.add(point.y)
        }
        return floats.toFloatArray()
    }

    private companion object {
        private const val HPGL_TO_PIXEL: Double = 0.04
        private const val MAX_TILES_PER_SEGMENT: Int = 512
    }
}
