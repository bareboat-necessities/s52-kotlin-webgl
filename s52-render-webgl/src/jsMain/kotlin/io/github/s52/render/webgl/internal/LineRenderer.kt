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
    private val hpglCache = mutableMapOf<String, HpglDisplayList>()
    fun renderSimple(command: S52DrawCommand.LineSimple, projector: GeometryProjector, colors: ColorResolver): Int {
        return drawGeometry(command.geometry, projector, colors.resolve(command.colorToken), lineWidth = command.width)
    }

    fun renderSimpleBatch(commands: List<S52DrawCommand.LineSimple>, projector: GeometryProjector, colors: ColorResolver): Int {
        if (commands.isEmpty()) return 0
        val first = commands.first()
        val vertices = lineSegments(commands, projector)
        if (vertices.isEmpty()) return 0
        gl.lineWidth(first.width.toFloat().coerceAtLeast(1.0f))
        return program.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(first.colorToken))
    }

    fun renderComplex(command: S52DrawCommand.LineComplex, projector: GeometryProjector, colors: ColorResolver): Int {
        val style = presLib.lineStyles.find(command.lineStyleName)
        val hpgl = style?.vectorHpgl
        if (style == null || hpgl.isNullOrBlank()) {
            return drawGeometry(command.geometry, projector, colors.resolve(null, fallback = "CHBLK"), lineWidth = 1.0)
        }

        val displayList = cachedHpgl(style.name, hpgl)
        val bounds = displayList.bounds
        val strokeSegments = displayList.strokeSegments()
        if (strokeSegments.isEmpty() || bounds == null) {
            return drawGeometry(command.geometry, projector, colors.resolve(style.colorRefs.firstOrNull(), fallback = "CHBLK"), lineWidth = 1.0)
        }

        val vertices = complexLineVertices(command.geometry, projector, style, strokeSegments, bounds)
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.5f)
        return program.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(style.colorRefs.firstOrNull(), fallback = "CHBLK"))
    }


    private fun cachedHpgl(name: String, hpgl: String): HpglDisplayList =
        hpglCache.getOrPut(name) { HpglDisplayListCompiler.compile(hpgl) }
    private fun drawGeometry(geometry: EncGeometry, projector: GeometryProjector, color: GlColor, lineWidth: Double): Int {
        val vertices = FloatArrayBuilder()
        when (geometry) {
            is EncGeometry.LineString -> appendLineStripSegments(geometry.coordinates, projector, vertices)
            is EncGeometry.Polygon -> appendLineStripSegments(geometry.outer, projector, vertices)
            else -> Unit
        }
        if (vertices.isEmpty()) return 0
        gl.lineWidth(lineWidth.toFloat().coerceAtLeast(1.0f))
        return program.draw(WebGLRenderingContext.LINES, vertices, color)
    }

    private fun complexLineVertices(
        geometry: EncGeometry,
        projector: GeometryProjector,
        style: LineStyleDefinition,
        hpglSegments: List<HpglLineSegment>,
        bounds: HpglBounds
    ): FloatArrayBuilder {
        val coordinates = when (geometry) {
            is EncGeometry.LineString -> geometry.coordinates
            is EncGeometry.Polygon -> geometry.outer
            else -> return FloatArrayBuilder(0)
        }
        if (coordinates.size < 2) return FloatArrayBuilder(0)

        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        if (sx <= 0.0 || sy <= 0.0) return FloatArrayBuilder(0)

        val tileWidthPx = tileWidthPx(style, bounds)
        val tileHeightPx = tileHeightPx(style, bounds)
        val originX = bounds.minX
        val originY = if (style.height > 0.0) style.pivotY else bounds.centerY
        val floats = FloatArrayBuilder()
        val limitX = projector.clipLimitX()
        val limitY = projector.clipLimitY()

        for (i in 0 until coordinates.lastIndex) {
            val rawA = projector.project(coordinates[i])
            val rawB = projector.project(coordinates[i + 1])
            val clipped = clipSegmentToViewport(rawA, rawB, limitX, limitY) ?: continue
            val a = clipped.first
            val b = clipped.second
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
        return floats
    }

    private fun appendLineStyleTile(
        out: FloatArrayBuilder,
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
            out.addLine(a, b)
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

    private fun lineSegments(commands: List<S52DrawCommand.LineSimple>, projector: GeometryProjector): FloatArrayBuilder {
        val floats = FloatArrayBuilder(commands.size * 8)
        for (command in commands) {
            when (val geometry = command.geometry) {
                is EncGeometry.LineString -> appendLineStripSegments(geometry.coordinates, projector, floats)
                is EncGeometry.Polygon -> appendLineStripSegments(geometry.outer, projector, floats)
                else -> Unit
            }
        }
        return floats
    }

    private fun appendLineStripSegments(
        coordinates: List<Coordinate>,
        projector: GeometryProjector,
        out: FloatArrayBuilder
    ) {
        if (coordinates.size < 2) return
        val limitX = projector.clipLimitX()
        val limitY = projector.clipLimitY()
        var previous = projector.project(coordinates[0])
        for (i in 1 until coordinates.size) {
            val next = projector.project(coordinates[i])
            val clipped = clipSegmentToViewport(previous, next, limitX, limitY)
            if (clipped != null) out.addLine(clipped.first, clipped.second)
            previous = next
        }
    }

    private companion object {
        private const val HPGL_TO_PIXEL: Double = 0.04
        private const val MAX_TILES_PER_SEGMENT: Int = 512
    }
}
