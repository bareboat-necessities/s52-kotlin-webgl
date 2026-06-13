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
    private val presLib: PresLibPack,
    private val stencilClipper: StencilPolygonClipper
) {
    private val vectorHpglCache = mutableMapOf<String, CachedPatternHpgl>()
    fun render(
        command: S52DrawCommand.AreaPattern,
        projector: GeometryProjector,
        colors: ColorResolver,
        palette: S52Palette
    ): Int {
        val polygon = command.geometry as? EncGeometry.Polygon ?: return 0
        val projected = ProjectedPolygonClip.from(polygon, projector) ?: return 0
        val pattern = presLib.patterns.find(command.patternName)
        if (pattern != null) {
            // OpenCPN chart-symbol patterns often include both HPGL and a raster
            // atlas cell. Prefer HPGL: the raster cell can be a preview/glyph box
            // with rounded placeholder edges, which looks like stacked rounded
            // rectangles when tiled across an ENC area.
            val vectorCalls = renderVectorPattern(pattern, projected, projector, colors)
            if (vectorCalls > 0) return vectorCalls

            val bitmapCalls = renderBitmapPattern(command, pattern, projected, projector, palette)
            if (bitmapCalls > 0) return bitmapCalls
        }

        val vertices = hatchLines(projected, projector)
        if (vertices.isEmpty()) return 0
        return clipped(projected, projector) {
            solidProgram.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(command.backgroundColorToken, fallback = "CHMGD"))
        }
    }

    private fun renderBitmapPattern(
        command: S52DrawCommand.AreaPattern,
        pattern: PatternDefinition,
        projected: ProjectedPolygonClip,
        projector: GeometryProjector,
        palette: S52Palette
    ): Int {
        val bitmap = pattern.bitmap ?: return 0
        // OpenCPN pattern placeholders can carry an atlas coordinate with a
        // zero-sized bitmap. Treat those as non-raster patterns; sampling a
        // single atlas texel and stretching it across each tile creates solid
        // color blocks that appear to bleed between patterned areas.
        if (bitmap.width <= 0.0 || bitmap.height <= 0.0) return 0
        val atlas = rasterAtlases.textureFor(palette, bitmap.atlasFileName) ?: return 0
        val vertices = bitmapTileVertices(projected, projector, bitmap, atlas.width, atlas.height)
        if (vertices.isEmpty()) return 0
        return clipped(projected, projector) {
            textureProgram.drawTriangles(atlas.texture, vertices, alpha = 1.0f)
        }
    }

    private fun renderVectorPattern(
        pattern: PatternDefinition,
        projected: ProjectedPolygonClip,
        projector: GeometryProjector,
        colors: ColorResolver
    ): Int {
        val hpgl = pattern.vectorHpgl ?: return 0
        val cachedHpgl = cachedPatternHpgl(pattern.name, hpgl)
        val bounds = cachedHpgl.bounds ?: return 0
        if (cachedHpgl.segments.isEmpty()) return 0

        val vertices = vectorTileVertices(projected, projector, pattern, cachedHpgl.segments, bounds)
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.0f)
        return clipped(projected, projector) {
            solidProgram.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(pattern.colorRefs.firstOrNull(), fallback = "CHMGD"))
        }
    }


    private fun cachedPatternHpgl(name: String, hpgl: String): CachedPatternHpgl =
        vectorHpglCache.getOrPut(name) {
            val segments = HpglLineParser.parseSegments(hpgl)
            CachedPatternHpgl(segments, HpglLineParser.bounds(segments))
        }
    private fun clipped(projected: ProjectedPolygonClip, projector: GeometryProjector, drawInside: () -> Int): Int =
        if (stencilClipper.isAvailable()) stencilClipper.clip(projected, projector, drawInside) else drawInside()

    private fun bitmapTileVertices(
        projected: ProjectedPolygonClip,
        projector: GeometryProjector,
        bitmap: RasterBitmapDefinition,
        atlasWidth: Int,
        atlasHeight: Int
    ): FloatArrayBuilder {
        val bounds = projected.bounds ?: return FloatArrayBuilder(0)
        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        val tileWClip = (bitmap.width * sx).toFloat().coerceAtLeast((8.0 * sx).toFloat())
        val tileHClip = (bitmap.height * sy).toFloat().coerceAtLeast((8.0 * sy).toFloat())
        if (tileWClip <= 0f || tileHClip <= 0f) return FloatArrayBuilder(0)

        val atlasW = atlasWidth.coerceAtLeast(1).toDouble()
        val atlasH = atlasHeight.coerceAtLeast(1).toDouble()
        val u0 = ((bitmap.x + 0.5) / atlasW).toFloat()
        val u1 = ((bitmap.x + bitmap.width - 0.5).coerceAtLeast(bitmap.x + 0.5) / atlasW).toFloat()
        val v0 = ((bitmap.y + 0.5) / atlasH).toFloat()
        val v1 = ((bitmap.y + bitmap.height - 0.5).coerceAtLeast(bitmap.y + 0.5) / atlasH).toFloat()

        val floats = FloatArrayBuilder()
        var yTop = bounds.maxY + tileHClip
        var row = 0
        while (yTop >= bounds.minY - tileHClip && row < MAX_TILE_ROWS) {
            var xLeft = bounds.minX - tileWClip
            var col = 0
            while (xLeft <= bounds.maxX + tileWClip && col < MAX_TILE_COLS) {
                val x0 = xLeft
                val x1 = xLeft + tileWClip
                val y0 = yTop
                val y1 = yTop - tileHClip
                if (projected.mayIntersectRect(x0, x1, minOf(y0, y1), maxOf(y0, y1))) {
                    floats.addTexturedTriangle(x0, y0, u0, v0, x1, y0, u1, v0, x1, y1, u1, v1)
                    floats.addTexturedTriangle(x0, y0, u0, v0, x1, y1, u1, v1, x0, y1, u0, v1)
                }
                xLeft += tileWClip
                col++
            }
            yTop -= tileHClip
            row++
        }
        return floats
    }

    private fun vectorTileVertices(
        projected: ProjectedPolygonClip,
        projector: GeometryProjector,
        pattern: PatternDefinition,
        hpglSegments: List<HpglLineSegment>,
        bounds: HpglBounds
    ): FloatArrayBuilder {
        val clipBounds = projected.bounds ?: return FloatArrayBuilder(0)
        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        if (sx <= 0.0 || sy <= 0.0) return FloatArrayBuilder(0)

        val tileWidthPx = tileWidthPx(pattern, bounds)
        val tileHeightPx = tileHeightPx(pattern, bounds)
        val tileWClip = (tileWidthPx * sx).toFloat()
        val tileHClip = (tileHeightPx * sy).toFloat()
        val originX = if (pattern.width > 0.0) pattern.pivotX else bounds.minX
        val originY = if (pattern.height > 0.0) pattern.pivotY else bounds.centerY
        val floats = FloatArrayBuilder()

        var yCenter = clipBounds.maxY + tileHClip * 0.5f
        var row = 0
        while (yCenter >= clipBounds.minY - tileHClip && row < MAX_TILE_ROWS) {
            var xCenter = clipBounds.minX - tileWClip * 0.5f
            var col = 0
            while (xCenter <= clipBounds.maxX + tileWClip && col < MAX_TILE_COLS) {
                val x0 = xCenter - tileWClip * 0.5f
                val x1 = xCenter + tileWClip * 0.5f
                val y0 = yCenter - tileHClip * 0.5f
                val y1 = yCenter + tileHClip * 0.5f
                if (projected.mayIntersectRect(x0, x1, y0, y1)) {
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
        return floats
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

    private fun hatchLines(projected: ProjectedPolygonClip, projector: GeometryProjector): FloatArrayBuilder {
        val bounds = projected.bounds ?: return FloatArrayBuilder(0)
        val sy = projector.pixelToClipY(HATCH_SPACING_PX).coerceAtLeast(0.0001f)
        val inset = projector.pixelToClipX(0.75)
        val floats = FloatArrayBuilder(64)
        val xs = ArrayList<Float>(16)

        var y = bounds.minY + sy
        var row = 0
        while (y < bounds.maxY && row < MAX_HATCH_ROWS) {
            xs.clear()
            appendHorizontalCrossings(projected.outer, y, xs)
            for (hole in projected.holes) appendHorizontalCrossings(hole, y, xs)
            if (xs.size >= 2) {
                xs.sort()
                var i = 0
                while (i + 1 < xs.size) {
                    val x0 = xs[i] + inset
                    val x1 = xs[i + 1] - inset
                    if (x1 > x0) {
                        floats.add(x0, y)
                        floats.add(x1, y)
                    }
                    i += 2
                }
            }
            y += sy
            row++
        }
        return floats
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

    private companion object {
        private const val HPGL_TO_PIXEL: Double = 0.04
        private const val MAX_TILE_ROWS: Int = 128
        private const val MAX_TILE_COLS: Int = 128
        private const val MAX_HATCH_ROWS: Int = 96
        private const val HATCH_SPACING_PX: Double = 18.0
    }
}

private data class CachedPatternHpgl(val segments: List<HpglLineSegment>, val bounds: HpglBounds?)
