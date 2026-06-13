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
    private val symbolHpglCache = mutableMapOf<String, HpglDisplayList>()
    fun render(
        command: S52DrawCommand.PointSymbol,
        projector: GeometryProjector,
        colors: ColorResolver,
        palette: S52Palette
    ): Int = renderBatch(listOf(command), projector, colors, palette)

    fun renderBatch(
        commands: List<S52DrawCommand.PointSymbol>,
        projector: GeometryProjector,
        colors: ColorResolver,
        palette: S52Palette
    ): Int {
        if (commands.isEmpty()) return 0
        var calls = 0
        var index = 0
        while (index < commands.size) {
            val first = prepare(commands[index], projector)
            if (first == null) {
                index++
                continue
            }

            val firstAtlas = atlasFor(first.definition, palette)
            if (firstAtlas != null) {
                val atlasUrl = firstAtlas.url
                val vertices = FloatArrayBuilder(commands.size * 24)
                while (index < commands.size) {
                    val prepared = prepare(commands[index], projector)
                    if (prepared == null) {
                        index++
                        continue
                    }
                    val bitmap = prepared.definition.bitmap
                    val atlas = atlasFor(prepared.definition, palette)
                    if (bitmap == null || atlas == null || atlas.url != atlasUrl) break
                    appendBitmapQuadVertices(
                        out = vertices,
                        bitmap = bitmap,
                        anchor = prepared.anchor,
                        projector = projector,
                        rotationDegrees = prepared.rotationDegrees,
                        atlasWidth = atlas.width,
                        atlasHeight = atlas.height
                    )
                    index++
                }
                calls += textureProgram.drawTriangles(firstAtlas.texture, vertices, alpha = 1.0f)
            } else {
                val vectorChunk = ArrayList<PreparedSymbol>()
                while (index < commands.size) {
                    val prepared = prepare(commands[index], projector)
                    if (prepared == null) {
                        index++
                        continue
                    }
                    if (atlasFor(prepared.definition, palette) != null) break
                    vectorChunk += prepared
                    index++
                }
                calls += renderPreparedVectorBatch(vectorChunk, projector, colors)
            }
        }
        return calls
    }

    private fun resolveSymbol(name: String): SymbolDefinition? {
        presLib.symbols.find(name)?.let { return it }
        val key = name.trim().substringBefore('#').uppercase()
        if (key.isEmpty()) return null
        SYMBOL_ALIASES[key]?.let { alias -> presLib.symbols.find(alias)?.let { return it } }
        return null
    }

    private fun prepare(command: S52DrawCommand.PointSymbol, projector: GeometryProjector): PreparedSymbol? {
        val geometry = command.geometry as? EncGeometry.Point ?: return null
        val definition = resolveSymbol(command.symbolName) ?: return null
        return PreparedSymbol(
            definition = definition,
            anchor = projector.project(geometry.coordinate),
            rotationDegrees = command.rotationDegrees ?: 0.0
        )
    }

    private fun atlasFor(definition: SymbolDefinition, palette: S52Palette): RasterAtlasTexture? {
        val bitmap = definition.bitmap ?: return null
        if (bitmap.width <= 0.0 || bitmap.height <= 0.0) return null
        return rasterAtlases.textureFor(palette, bitmap.atlasFileName)
    }

    private fun bitmapQuadVertices(
        bitmap: RasterBitmapDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double,
        atlasWidth: Int,
        atlasHeight: Int
    ): FloatArray {
        val out = FloatArrayBuilder(24)
        appendBitmapQuadVertices(out, bitmap, anchor, projector, rotationDegrees, atlasWidth, atlasHeight)
        return out.toFloatArray()
    }

    private fun appendBitmapQuadVertices(
        out: FloatArrayBuilder,
        bitmap: RasterBitmapDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double,
        atlasWidth: Int,
        atlasHeight: Int
    ) {
        val points = arrayOf(
            transformBitmapPoint(0.0, 0.0, bitmap, anchor, projector, rotationDegrees),
            transformBitmapPoint(bitmap.width, 0.0, bitmap, anchor, projector, rotationDegrees),
            transformBitmapPoint(bitmap.width, bitmap.height, bitmap, anchor, projector, rotationDegrees),
            transformBitmapPoint(0.0, bitmap.height, bitmap, anchor, projector, rotationDegrees)
        )

        val atlasW = atlasWidth.coerceAtLeast(1).toDouble()
        val atlasH = atlasHeight.coerceAtLeast(1).toDouble()
        val u0 = ((bitmap.x + 0.5) / atlasW).toFloat()
        val u1 = ((bitmap.x + bitmap.width - 0.5).coerceAtLeast(bitmap.x + 0.5) / atlasW).toFloat()
        val v0 = ((bitmap.y + 0.5) / atlasH).toFloat()
        val v1 = ((bitmap.y + bitmap.height - 0.5).coerceAtLeast(bitmap.y + 0.5) / atlasH).toFloat()

        out.addTexturedTriangle(points[0].x, points[0].y, u0, v0, points[1].x, points[1].y, u1, v0, points[2].x, points[2].y, u1, v1)
        out.addTexturedTriangle(points[0].x, points[0].y, u0, v0, points[2].x, points[2].y, u1, v1, points[3].x, points[3].y, u0, v1)
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

    private fun renderPreparedVectorBatch(
        symbols: List<PreparedSymbol>,
        projector: GeometryProjector,
        colors: ColorResolver
    ): Int {
        if (symbols.isEmpty()) return 0
        val fillsByColor = linkedMapOf<String?, FloatArrayBuilder>()
        val strokesByState = linkedMapOf<VectorStrokeKey, FloatArrayBuilder>()

        fun fillVertices(colorToken: String?): FloatArrayBuilder =
            fillsByColor.getOrPut(colorToken) { FloatArrayBuilder(96) }

        fun strokeVertices(colorToken: String?, lineWidth: Float = SYMBOL_LINE_WIDTH): FloatArrayBuilder =
            strokesByState.getOrPut(VectorStrokeKey(colorToken, lineWidth)) { FloatArrayBuilder(96) }

        for (symbol in symbols) {
            val definition = symbol.definition
            val hpgl = definition.vectorHpgl
            if (!hpgl.isNullOrBlank()) {
                val displayList = symbolHpglCache.getOrPut(definition.name) { HpglDisplayListCompiler.compile(hpgl) }
                for (geometry in displayList.geometries) {
                    val colorToken = displayList.colorTokenForPen(geometry.pen, definition.colorRefs)
                    val fillOut = fillVertices(colorToken)
                    for (triangle in geometry.fills) {
                        fillOut.addTriangle(
                            transformHpglPoint(triangle.a, definition, symbol.anchor, projector, symbol.rotationDegrees),
                            transformHpglPoint(triangle.b, definition, symbol.anchor, projector, symbol.rotationDegrees),
                            transformHpglPoint(triangle.c, definition, symbol.anchor, projector, symbol.rotationDegrees)
                        )
                    }
                    val strokeOut = strokeVertices(colorToken)
                    for (segment in geometry.strokes) {
                        strokeOut.addLine(
                            transformHpglPoint(HpglPoint(segment.x1, segment.y1), definition, symbol.anchor, projector, symbol.rotationDegrees),
                            transformHpglPoint(HpglPoint(segment.x2, segment.y2), definition, symbol.anchor, projector, symbol.rotationDegrees)
                        )
                    }
                }
            } else {
                val strokeOut = strokeVertices(definition.colorRefs.firstOrNull())
                appendVectorCommands(strokeOut, definition, symbol.anchor, projector, symbol.rotationDegrees)
            }
        }

        var calls = 0
        for ((colorToken, vertices) in fillsByColor) {
            if (vertices.isNotEmpty()) {
                calls += solidProgram.draw(WebGLRenderingContext.TRIANGLES, vertices, colors.resolve(colorToken, fallback = "CHBLK"))
            }
        }
        for ((state, vertices) in strokesByState) {
            if (vertices.isNotEmpty()) {
                gl.lineWidth(state.lineWidth)
                calls += solidProgram.draw(WebGLRenderingContext.LINES, vertices, colors.resolve(state.colorToken, fallback = "CHBLK"))
            }
        }
        return calls
    }

    private fun renderHpglDisplayList(
        definition: SymbolDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        colors: ColorResolver,
        rotationDegrees: Double
    ): Int {
        val hpgl = definition.vectorHpgl ?: return 0
        val displayList = symbolHpglCache.getOrPut(definition.name) { HpglDisplayListCompiler.compile(hpgl) }
        if (displayList.isEmpty) return 0

        var calls = 0
        for (geometry in displayList.geometries) {
            if (geometry.fills.isNotEmpty()) {
                val fillVertices = FloatArrayBuilder(geometry.fills.size * 6)
                for (triangle in geometry.fills) {
                    fillVertices.addTriangle(
                        transformHpglPoint(triangle.a, definition, anchor, projector, rotationDegrees),
                        transformHpglPoint(triangle.b, definition, anchor, projector, rotationDegrees),
                        transformHpglPoint(triangle.c, definition, anchor, projector, rotationDegrees)
                    )
                }
                calls += solidProgram.draw(
                    WebGLRenderingContext.TRIANGLES,
                    fillVertices,
                    colors.resolve(displayList.colorTokenForPen(geometry.pen, definition.colorRefs), fallback = "CHBLK")
                )
            }

            if (geometry.strokes.isNotEmpty()) {
                val strokeVertices = FloatArrayBuilder(geometry.strokes.size * 4)
                for (segment in geometry.strokes) {
                    strokeVertices.addLine(
                        transformHpglPoint(HpglPoint(segment.x1, segment.y1), definition, anchor, projector, rotationDegrees),
                        transformHpglPoint(HpglPoint(segment.x2, segment.y2), definition, anchor, projector, rotationDegrees)
                    )
                }
                gl.lineWidth(SYMBOL_LINE_WIDTH)
                calls += solidProgram.draw(
                    WebGLRenderingContext.LINES,
                    strokeVertices,
                    colors.resolve(displayList.colorTokenForPen(geometry.pen, definition.colorRefs), fallback = "CHBLK")
                )
            }
        }
        return calls
    }

    private fun symbolLineSegments(
        definition: SymbolDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ): FloatArray {
        val floats = FloatArrayBuilder()
        appendVectorCommands(floats, definition, anchor, projector, rotationDegrees)
        if (floats.isEmpty() && definition.vectorHpgl.isNullOrBlank()) {
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
        val segments = symbolHpglCache.getOrPut(definition.name) { HpglDisplayListCompiler.compile(hpgl) }.strokeSegments()
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

    private fun transformHpglPoint(
        point: HpglPoint,
        definition: SymbolDefinition,
        anchor: ClipPoint,
        projector: GeometryProjector,
        rotationDegrees: Double
    ): ClipPoint = transformLocal(
        (point.x - definition.pivotX) * HPGL_TO_PIXEL,
        (point.y - definition.pivotY) * HPGL_TO_PIXEL,
        anchor,
        projector,
        rotationDegrees
    )

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

    private data class PreparedSymbol(
        val definition: SymbolDefinition,
        val anchor: ClipPoint,
        val rotationDegrees: Double
    )

    private data class VectorStrokeKey(
        val colorToken: String?,
        val lineWidth: Float
    )

    private companion object {
        /** OpenCPN vector HPGL units are much finer than atlas pixels. */
        private const val HPGL_TO_PIXEL: Double = 0.04
        private const val SYMBOL_LINE_WIDTH: Float = 1.5f

        /**
         * Compatibility aliases for renderer-independent CSP outputs whose
         * stable project names differ from the OpenCPN chart-symbol names.
         */
        private val SYMBOL_ALIASES = mapOf(
            "TOPMAR_CONE_UP01" to "TOPMAR88",
            "TOPMAR_CONE_DOWN01" to "TOPMAR87",
            "TOPMAR_SPHERE01" to "TOPMAR65",
            "TOPMAR_TWO_SPHERES01" to "TOPMAR86",
            "TOPMAR_CYLINDER01" to "TOPMAR85",
            "TOPMAR_X01" to "QUESMRK1",
            "TOPMAR_CROSS01" to "QUESMRK1",
            "TOPMAR_UNKNOWN01" to "QUESMRK1",
            "WRECKS_DANGER01" to "ISODGR01",
            "WRECKS01" to "WRECKS05",
            "OBSTRN_DANGER01" to "ISODGR01",
            "OBSTRN01" to "OBSTRN11"
        )
    }
}
