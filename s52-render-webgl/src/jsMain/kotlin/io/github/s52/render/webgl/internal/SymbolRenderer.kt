package io.github.s52.render.webgl.internal

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.RasterBitmapDefinition
import io.github.s52.preslib.SymbolDefinition
import io.github.s52.preslib.VectorCommand
import org.khronos.webgl.WebGLRenderingContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class SymbolRenderer(
    private val gl: WebGLRenderingContext,
    private val solidProgram: SolidColorProgram,
    private val textureProgram: TextureProgram,
    private val rasterAtlases: RasterAtlasCache,
    private val presLib: PresLibPack
) {
    fun render(
        command: S52DrawCommand.PointSymbol,
        projector: GeometryProjector,
        colors: ColorResolver,
        palette: S52Palette
    ): Int {
        val geometry = command.geometry as? EncGeometry.Point ?: return 0
        val definition = presLib.symbols.find(command.symbolName) ?: return 0
        val anchor = projector.project(geometry.coordinate)
        val rotationDegrees = command.rotationDegrees ?: 0.0

        val bitmap = definition.bitmap
        if (bitmap != null) {
            val atlas = rasterAtlases.textureFor(palette, bitmap.atlasFileName)
            if (atlas != null) {
                val vertices = bitmapQuadVertices(
                    bitmap = bitmap,
                    anchor = anchor,
                    projector = projector,
                    rotationDegrees = rotationDegrees,
                    atlasWidth = atlas.width,
                    atlasHeight = atlas.height
                )
                return textureProgram.drawTriangles(atlas.texture, vertices)
            }
        }

        val vertices = symbolLineSegments(
            definition = definition,
            anchor = anchor,
            projector = projector,
            rotationDegrees = rotationDegrees
        )
        if (vertices.isEmpty()) return 0
        gl.lineWidth(1.5f)
        return solidProgram.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(definition.colorRefs.firstOrNull(), fallback = "CHBLK"))
    }

    private fun bitmapQuadVertices(
        bitmap: RasterBitmapDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double,
        atlasWidth: Int,
        atlasHeight: Int
    ): FloatArray {
        val points = arrayOf(
            transformBitmapPoint(0.0, 0.0, bitmap, anchor, projector, rotationDegrees),
            transformBitmapPoint(bitmap.width, 0.0, bitmap, anchor, projector, rotationDegrees),
            transformBitmapPoint(bitmap.width, bitmap.height, bitmap, anchor, projector, rotationDegrees),
            transformBitmapPoint(0.0, bitmap.height, bitmap, anchor, projector, rotationDegrees)
        )

        val u0 = (bitmap.x / atlasWidth.coerceAtLeast(1)).toFloat()
        val u1 = ((bitmap.x + bitmap.width) / atlasWidth.coerceAtLeast(1)).toFloat()
        val v0 = (bitmap.y / atlasHeight.coerceAtLeast(1)).toFloat()
        val v1 = ((bitmap.y + bitmap.height) / atlasHeight.coerceAtLeast(1)).toFloat()

        return floatArrayOf(
            points[0].x, points[0].y, u0, v0,
            points[1].x, points[1].y, u1, v0,
            points[2].x, points[2].y, u1, v1,
            points[0].x, points[0].y, u0, v0,
            points[2].x, points[2].y, u1, v1,
            points[3].x, points[3].y, u0, v1
        )
    }

    private fun transformBitmapPoint(
        x: Double,
        y: Double,
        bitmap: RasterBitmapDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ): ClipPoint {
        val px = bitmap.pivotX
        val py = bitmap.pivotY
        return transformLocal(x - px, y - py, anchor, projector, rotationDegrees)
    }

    private fun symbolLineSegments(
        definition: SymbolDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ): FloatArray {
        val floats = FloatArrayBuilder()
        appendVectorCommands(floats, definition, anchor, projector, rotationDegrees)
        if (floats.isEmpty()) {
            appendHpglFallback(floats, definition, anchor, projector, rotationDegrees)
        }
        return floats.toFloatArray()
    }

    private fun appendVectorCommands(
        floats: FloatArrayBuilder,
        definition: SymbolDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ) {
        var start: ClipPoint? = null
        var current: ClipPoint? = null
        for (command in definition.commands) {
            when (command) {
                is VectorCommand.MoveTo -> {
                    current = transformLocal(command.x - definition.pivotX, command.y - definition.pivotY, anchor, projector, rotationDegrees)
                    if (start == null) start = current
                }
                is VectorCommand.LineTo -> {
                    val next = transformLocal(command.x - definition.pivotX, command.y - definition.pivotY, anchor, projector, rotationDegrees)
                    val previous = current
                    if (previous != null) {
                        floats.addLine(previous, next)
                    }
                    current = next
                }
                VectorCommand.ClosePath -> {
                    val previous = current
                    val first = start
                    if (previous != null && first != null) {
                        floats.addLine(previous, first)
                    }
                    start = null
                    current = null
                }
            }
        }
    }

    private fun appendHpglFallback(
        floats: FloatArrayBuilder,
        definition: SymbolDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ) {
        val hpgl = definition.vectorHpgl ?: return
        val segments = HpglLineParser.parseSegments(hpgl)
        for (segment in segments) {
            val a = transformLocal(
                (segment.x1 - definition.pivotX) * HPGL_TO_PIXEL,
                (segment.y1 - definition.pivotY) * HPGL_TO_PIXEL,
                anchor,
                projector,
                rotationDegrees
            )
            val b = transformLocal(
                (segment.x2 - definition.pivotX) * HPGL_TO_PIXEL,
                (segment.y2 - definition.pivotY) * HPGL_TO_PIXEL,
                anchor,
                projector,
                rotationDegrees
            )
            floats.addLine(a, b)
        }
    }

    private fun transformLocal(
        localX: Double,
        localY: Double,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ): ClipPoint {
        val sx = projector.pixelToClipX(1.0)
        val sy = projector.pixelToClipY(1.0)
        val angle = rotationDegrees * PI / 180.0
        val ca = cos(angle)
        val sa = sin(angle)
        val rx = localX * ca - localY * sa
        val ry = localX * sa + localY * ca
        return ClipPoint(
            x = anchor.x + (rx * sx).toFloat(),
            y = anchor.y - (ry * sy).toFloat()
        )
    }

    private companion object {
        /** OpenCPN vector HPGL units are much finer than atlas pixels. */
        private const val HPGL_TO_PIXEL: Double = 0.04
    }
}
