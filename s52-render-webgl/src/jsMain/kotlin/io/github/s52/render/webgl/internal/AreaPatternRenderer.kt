package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PatternDefinition
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.RasterBitmapDefinition
import org.khronos.webgl.WebGLRenderingContext

internal class AreaPatternRenderer(
    private val gl: WebGLRenderingContext,
    private val solidProgram: SolidColorProgram,
    private val textureProgram: TextureProgram,
    private val rasterAtlases: RasterAtlasCache,
    private val presLib: PresLibPack
) {
    fun render(
        command: S52DrawCommand.AreaPattern,
        projector: GeometryProjector,
        colors: ColorResolver,
        palette: S52Palette
    ): Int {
        val pattern = presLib.patterns.find(command.patternName)
        if (pattern != null) {
            val bitmapCalls = renderBitmapPattern(command, pattern, projector, palette)
            if (bitmapCalls > 0) return bitmapCalls

            val vectorCalls = renderVectorPattern(command, pattern, projector, colors)
            if (vectorCalls > 0) return vectorCalls
        }

        val vertices = hatchLines(command.geometry, projector)
        if (vertices.isEmpty()) return 0
        return solidProgram.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(command.backgroundColorToken, fallback = "CHMGD"))
    }

    private fun renderBitmapPattern(
        command: S52DrawCommand.AreaPattern,
        pattern: PatternDefinition,
        projector: GeometryProjector,
        palette: S52Palette
    ): Int {
        val bitmap = pattern.bitmap ?: return 0
        val polygon = command.geometry as? EncGeometry.Polygon ?: return 0
        val atlas = rasterAtlases.textureFor(palette, bitmap.atlasFileName) ?: return 0
        val vertices = bitmapTileVertices(polygon, projector, bitmap, atlas.width, atlas.height)
        if (vertices.isEmpty()) return 0
        return textureProgram.drawTriangles(atlas.texture, vertices, alpha = 1.0f)
    }

    private fun renderVectorPattern(
        command: S52DrawCommand.AreaPattern,
        pattern: PatternDefinition,
        projector: GeometryProjector,
        colors: ColorResolver
    ): Int {
        val polygon = command.geometry as? EncGeometry.Polygon ?: return 0
        val hpgl = pattern.vectorHpgl ?: return 0
        val segments = HpglLineParser.parseSegments(hpgl)
        val bounds = HpglLineParser.bounds(segments) ?: return 0
        if (segments.isEmpty()) return 0

        val vertices = vectorTileVertices(polygon, projector, pattern, segments, bounds)
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.0f)
        return solidProgram.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(pattern.colorRefs.firstOrNull(), fallback = "CHMGD"))
    }

    private fun bitmapTileVertices(
        polygon: EncGeometry.Polygon,
        projector: GeometryProjector,
        bitmap: RasterBitmapDefinition,
        atlasWidth: Int,
        atlasHeight: Int
    ): FloatArray {
        val projected = ProjectedPolygonClip.from(polygon, projector) ?: return FloatArray(0)
        val bounds = ClipBounds.of(projected.allPoints) ?: return FloatArray(0)
        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        val tileWClip = (bitmap.width * sx).toFloat().coerceAtLeast((8.0 * sx).toFloat())
        val tileHClip = (bitmap.height * sy).toFloat().coerceAtLeast((8.0 * sy).toFloat())
        if (tileWClip <= 0f || tileHClip <= 0f) return FloatArray(0)

        val atlasW = atlasWidth.coerceAtLeast(1).toDouble()
        val atlasH = atlasHeight.coerceAtLeast(1).toDouble()
        val u0 = ((bitmap.x + 0.5) / atlasW).toFloat()
        val u1 = ((bitmap.x + bitmap.width - 0.5).coerceAtLeast(bitmap.x + 0.5) / atlasW).toFloat()
        val v0 = ((bitmap.y + 0.5) / atlasH).toFloat()
        val v1 = ((bitmap.y + bitmap.height - 0.5).coerceAtLeast(bitmap.y + 0.5) / atlasH).toFloat()

        val floats = FloatArrayBuilder()
        var yTop = bounds.maxY
        var row = 0
        while (yTop >= bounds.minY - tileHClip && row < MAX_TILE_ROWS) {
            var xLeft = bounds.minX
            var col = 0
            while (xLeft <= bounds.maxX + tileWClip && col < MAX_TILE_COLS) {
                val cx = xLeft + tileWClip * 0.5f
                val cy = yTop - tileHClip * 0.5f
                if (projected.contains(cx, cy)) {
                    val x0 = xLeft
                    val x1 = xLeft + tileWClip
                    val y0 = yTop
                    val y1 = yTop - tileHClip
                    floats.addTexturedTriangle(x0, y0, u0, v0, x1, y0, u1, v0, x1, y1, u1, v1)
                    floats.addTexturedTriangle(x0, y0, u0, v0, x1, y1, u1, v1, x0, y1, u0, v1)
                }
                xLeft += tileWClip
                col++
            }
            yTop -= tileHClip
            row++
        }
        return floats.toFloatArray()
    }

    private fun vectorTileVertices(
        polygon: EncGeometry.Polygon,
        projector: GeometryProjector,
        pattern: PatternDefinition,
        hpglSegments: List<HpglLineSegment>,
        bounds: HpglBounds
    ): FloatArray {
        val projected = ProjectedPolygonClip.from(polygon, projector) ?: return FloatArray(0)
        val clipBounds = ClipBounds.of(projected.allPoints) ?: return FloatArray(0)
        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        if (sx <= 0.0 || sy <= 0.0) return FloatArray(0)

        val tileWidthPx = tileWidthPx(pattern, bounds)
        val tileHeightPx = tileHeightPx(pattern, bounds)
        val tileWClip = (tileWidthPx * sx).toFloat()
        val tileHClip = (tileHeightPx * sy).toFloat()
        val originX = if (pattern.width > 0.0) pattern.pivotX else bounds.minX
        val originY = if (pattern.height > 0.0) pattern.pivotY else bounds.centerY
        val floats = FloatArrayBuilder()

        var yCenter = clipBounds.maxY - tileHClip * 0.5f
        var row = 0
        while (yCenter >= clipBounds.minY - tileHClip && row < MAX_TILE_ROWS) {
            var xCenter = clipBounds.minX + tileWClip * 0.5f
            var col = 0
            while (xCenter <= clipBounds.maxX + tileWClip && col < MAX_TILE_COLS) {
                if (projected.contains(xCenter, yCenter)) {
                    appendPatternTile(
                        out = floats,
                        hpglSegments = hpglSegments,
                        originX = originX,
                        originY = originY,
                        tileWidthPx = tileWidthPx,
                        tileHeightPx = tileHeightPx,
                        sx = sx,
                        sy = sy,
                        center = ClipPoint(xCenter, yCenter)
                    )
                }
                xCenter += tileWClip
                col++
            }
            yCenter -= tileHClip
            row++
        }
        return floats.toFloatArray()
    }

    private fun appendPatternTile(
        out: FloatArrayBuilder,
        hpglSegments: List<HpglLineSegment>,
        originX: Double,
        originY: Double,
        tileWidthPx: Double,
        tileHeightPx: Double,
        sx: Double,
        sy: Double,
        center: ClipPoint
    ) {
        fun point(x: Double, y: Double): ClipPoint {
            val localX = (x - originX) * HPGL_TO_PIXEL - tileWidthPx * 0.5
            val localY = (y - originY) * HPGL_TO_PIXEL - tileHeightPx * 0.5
            return ClipPoint(
                x = center.x + (localX * sx).toFloat(),
                y = center.y - (localY * sy).toFloat()
            )
        }
        for (segment in hpglSegments) {
            val a = point(segment.x1, segment.y1)
            val b = point(segment.x2, segment.y2)
            out.addLine(a, b)
        }
    }

    private fun tileWidthPx(pattern: PatternDefinition, bounds: HpglBounds): Double {
        val sourceWidth = when {
            pattern.width > 0.0 -> pattern.width
            bounds.width > 0.0 -> bounds.width
            else -> 400.0
        }
        return (sourceWidth * HPGL_TO_PIXEL).coerceIn(12.0, 128.0)
    }

    private fun tileHeightPx(pattern: PatternDefinition, bounds: HpglBounds): Double {
        val sourceHeight = when {
            pattern.height > 0.0 -> pattern.height
            bounds.height > 0.0 -> bounds.height
            else -> 400.0
        }
        return (sourceHeight * HPGL_TO_PIXEL).coerceIn(12.0, 128.0)
    }

    private fun hatchLines(geometry: EncGeometry, projector: GeometryProjector): FloatArray {
        val polygon = geometry as? EncGeometry.Polygon ?: return FloatArray(0)
        val projected = ProjectedPolygonClip.from(polygon, projector) ?: return FloatArray(0)
        val bounds = ClipBounds.of(projected.allPoints) ?: return FloatArray(0)
        val sy = projector.pixelToClipY(HATCH_SPACING_PX).toFloat().coerceAtLeast(0.0001f)
        val inset = projector.pixelToClipX(0.75).toFloat()
        val floats = FloatArrayBuilder(64)

        var y = bounds.minY + sy
        var row = 0
        while (y < bounds.maxY && row < MAX_HATCH_ROWS) {
            val intervals = horizontalIntervals(projected, y)
            for (interval in intervals) {
                val x0 = interval.first + inset
                val x1 = interval.second - inset
                if (x1 > x0) {
                    floats.add(x0, y)
                    floats.add(x1, y)
                }
            }
            y += sy
            row++
        }
        return floats.toFloatArray()
    }

    private fun horizontalIntervals(projected: ProjectedPolygonClip, y: Float): List<Pair<Float, Float>> {
        val xs = ArrayList<Float>()
        appendHorizontalCrossings(projected.outer, y, xs)
        for (hole in projected.holes) appendHorizontalCrossings(hole, y, xs)
        if (xs.size < 2) return emptyList()
        xs.sort()
        val intervals = ArrayList<Pair<Float, Float>>(xs.size / 2)
        var i = 0
        while (i + 1 < xs.size) {
            val left = xs[i]
            val right = xs[i + 1]
            if (right > left) intervals += left to right
            i += 2
        }
        return intervals
    }

    private fun appendHorizontalCrossings(ring: List<ClipPoint>, y: Float, out: MutableList<Float>) {
        if (ring.size < 2) return
        var previous = ring.last()
        for (current in ring) {
            val minY = minOf(previous.y, current.y)
            val maxY = maxOf(previous.y, current.y)
            if (maxY > minY && y >= minY && y < maxY) {
                val t = (y - previous.y) / (current.y - previous.y)
                out += previous.x + t * (current.x - previous.x)
            }
            previous = current
        }
    }

    private data class ClipBounds(val minX: Float, val maxX: Float, val minY: Float, val maxY: Float) {
        companion object {
            fun of(points: List<ClipPoint>): ClipBounds? {
                if (points.isEmpty()) return null
                var minX = points.first().x
                var maxX = points.first().x
                var minY = points.first().y
                var maxY = points.first().y
                for (p in points.drop(1)) {
                    minX = minOf(minX, p.x)
                    maxX = maxOf(maxX, p.x)
                    minY = minOf(minY, p.y)
                    maxY = maxOf(maxY, p.y)
                }
                return ClipBounds(minX, maxX, minY, maxY)
            }
        }
    }

    private companion object {
        private const val HPGL_TO_PIXEL: Double = 0.04
        private const val MAX_TILE_ROWS: Int = 128
        private const val MAX_TILE_COLS: Int = 128
        private const val MAX_HATCH_ROWS: Int = 96
        private const val HATCH_SPACING_PX: Double = 18.0
    }
}
