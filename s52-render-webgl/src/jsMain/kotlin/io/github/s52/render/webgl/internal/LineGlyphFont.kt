package io.github.s52.render.webgl.internal

import kotlin.math.max
import kotlin.math.min

internal data class LabelBounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    fun isOutside(limitX: Float = 1.04f, limitY: Float = 1.04f): Boolean =
        maxX < -limitX || minX > limitX || maxY < -limitY || minY > limitY

    fun expanded(dx: Float, dy: Float): LabelBounds = LabelBounds(
        minX = minX - dx,
        minY = minY - dy,
        maxX = maxX + dx,
        maxY = maxY + dy
    )

    fun intersects(other: LabelBounds): Boolean =
        minX <= other.maxX && maxX >= other.minX && minY <= other.maxY && maxY >= other.minY
}

internal data class GlyphLayout(
    val vertices: FloatArray,
    val bounds: LabelBounds
) {
    fun isEmpty(): Boolean = vertices.isEmpty()
}

internal object LineGlyphFont {
    private const val CHAR_ADVANCE: Double = 0.72
    private const val GLYPH_HEIGHT: Double = 1.0

    private val segmentMap: Map<Char, List<Segment>> = mapOf(
        '0' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F),
        '1' to listOf(Segment.B, Segment.C),
        '2' to listOf(Segment.A, Segment.B, Segment.G, Segment.E, Segment.D),
        '3' to listOf(Segment.A, Segment.B, Segment.G, Segment.C, Segment.D),
        '4' to listOf(Segment.F, Segment.G, Segment.B, Segment.C),
        '5' to listOf(Segment.A, Segment.F, Segment.G, Segment.C, Segment.D),
        '6' to listOf(Segment.A, Segment.F, Segment.G, Segment.C, Segment.D, Segment.E),
        '7' to listOf(Segment.A, Segment.B, Segment.C),
        '8' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F, Segment.G),
        '9' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.F, Segment.G),

        // Compact single-stroke approximations for S-52 text labels.  The old
        // font only knew a few letters and rendered most unsupported letters as
        // an X, which made object labels and sounding annotations look broken.
        'A' to listOf(Segment.A, Segment.B, Segment.C, Segment.E, Segment.F, Segment.G),
        'B' to listOf(Segment.F, Segment.E, Segment.G, Segment.C, Segment.D),
        'C' to listOf(Segment.A, Segment.D, Segment.E, Segment.F),
        'D' to listOf(Segment.B, Segment.C, Segment.D, Segment.E, Segment.G),
        'E' to listOf(Segment.A, Segment.D, Segment.E, Segment.F, Segment.G),
        'F' to listOf(Segment.A, Segment.E, Segment.F, Segment.G),
        'G' to listOf(Segment.A, Segment.F, Segment.E, Segment.D, Segment.C, Segment.G),
        'H' to listOf(Segment.F, Segment.E, Segment.G, Segment.B, Segment.C),
        'I' to listOf(Segment.A, Segment.Center, Segment.D),
        'J' to listOf(Segment.B, Segment.C, Segment.D, Segment.E),
        'K' to listOf(Segment.F, Segment.E, Segment.G, Segment.KUpper, Segment.KLower),
        'L' to listOf(Segment.F, Segment.E, Segment.D),
        'M' to listOf(Segment.F, Segment.E, Segment.B, Segment.C, Segment.MLeft, Segment.MRight),
        'N' to listOf(Segment.F, Segment.E, Segment.B, Segment.C, Segment.X1),
        'O' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F),
        'P' to listOf(Segment.A, Segment.B, Segment.F, Segment.E, Segment.G),
        'Q' to listOf(Segment.A, Segment.B, Segment.C, Segment.D, Segment.E, Segment.F, Segment.QTail),
        'R' to listOf(Segment.A, Segment.B, Segment.F, Segment.E, Segment.G, Segment.RLeg),
        'S' to listOf(Segment.A, Segment.F, Segment.G, Segment.C, Segment.D),
        'T' to listOf(Segment.A, Segment.Center),
        'U' to listOf(Segment.F, Segment.E, Segment.B, Segment.C, Segment.D),
        'V' to listOf(Segment.VLeft, Segment.VRight),
        'W' to listOf(Segment.F, Segment.E, Segment.B, Segment.C, Segment.WLeft, Segment.WRight),
        'X' to listOf(Segment.X1, Segment.X2),
        'Y' to listOf(Segment.YLeft, Segment.YRight, Segment.CenterLower),
        'Z' to listOf(Segment.A, Segment.X2, Segment.D),

        '-' to listOf(Segment.G),
        '.' to listOf(Segment.DotA, Segment.DotB),
        ',' to listOf(Segment.Comma),
        ':' to listOf(Segment.DotTop, Segment.DotA, Segment.DotB),
        '/' to listOf(Segment.Slash),
        '\\' to listOf(Segment.Backslash),
        '(' to listOf(Segment.ParenLeft),
        ')' to listOf(Segment.ParenRight),
        '+' to listOf(Segment.PlusH, Segment.PlusV),
        '°' to listOf(Segment.DegreeA, Segment.DegreeB, Segment.DegreeC, Segment.DegreeD),
        ' ' to emptyList()
    )

    fun lineVertices(text: String, anchor: ClipPoint, projector: GeometryProjector, pixelSize: Double): FloatArray =
        lineLayout(text, anchor, projector, pixelSize).vertices

    fun lineLayout(
        text: String,
        anchor: ClipPoint,
        projector: GeometryProjector,
        pixelSize: Double,
        maxChars: Int = 48
    ): GlyphLayout {
        val normalized = normalizeText(text, maxChars)
        if (normalized.isBlank()) return emptyLayout(anchor)
        val totalWidthPx = normalized.length * CHAR_ADVANCE * pixelSize
        val leftPx = -totalWidthPx * 0.5
        val topPx = -pixelSize * 0.50
        val bounds = boundsFor(
            anchor = anchor,
            projector = projector,
            leftPx = leftPx,
            topPx = topPx,
            widthPx = totalWidthPx,
            heightPx = pixelSize * GLYPH_HEIGHT,
            paddingPx = 2.0
        )
        val floats = FloatArrayBuilder(normalized.length * 16)
        appendGlyphs(
            out = floats,
            text = normalized,
            anchor = anchor,
            projector = projector,
            pixelSize = pixelSize,
            leftPx = leftPx,
            topPx = topPx
        )
        return GlyphLayout(floats.toFloatArray(), bounds)
    }

    /**
     * S-52 sounding labels are easier to read when the decimal part is shown as
     * a small lowered digit instead of a full-size decimal string.  For example,
     * `12.3` is rendered as `12` plus a small lowered `3` without a decimal dot.
     */
    fun soundingVertices(depthLabel: String, anchor: ClipPoint, projector: GeometryProjector): FloatArray =
        soundingLayout(depthLabel, anchor, projector).vertices

    fun soundingLayout(depthLabel: String, anchor: ClipPoint, projector: GeometryProjector): GlyphLayout {
        val parts = normalizeSounding(depthLabel)
        if (parts.main.isBlank() && parts.fraction.isBlank()) return emptyLayout(anchor)

        val mainSize = 11.0
        val fractionSize = 6.8
        val gapPx = if (parts.fraction.isBlank()) 0.0 else 1.4
        val mainWidthPx = parts.main.length * CHAR_ADVANCE * mainSize
        val fractionWidthPx = parts.fraction.length * CHAR_ADVANCE * fractionSize
        val totalWidthPx = mainWidthPx + gapPx + fractionWidthPx
        val leftPx = -totalWidthPx * 0.5
        val mainTopPx = -mainSize * 0.60
        val fractionTopPx = mainSize * 0.03
        val totalTopPx = min(mainTopPx, fractionTopPx)
        val totalBottomPx = max(mainTopPx + mainSize, fractionTopPx + fractionSize)

        val floats = FloatArrayBuilder((parts.main.length + parts.fraction.length).coerceAtLeast(1) * 16)
        appendGlyphs(
            out = floats,
            text = parts.main,
            anchor = anchor,
            projector = projector,
            pixelSize = mainSize,
            leftPx = leftPx,
            topPx = mainTopPx
        )
        if (parts.fraction.isNotBlank()) {
            appendGlyphs(
                out = floats,
                text = parts.fraction,
                anchor = anchor,
                projector = projector,
                pixelSize = fractionSize,
                leftPx = leftPx + mainWidthPx + gapPx,
                topPx = fractionTopPx
            )
        }

        val bounds = boundsFor(
            anchor = anchor,
            projector = projector,
            leftPx = leftPx,
            topPx = totalTopPx,
            widthPx = totalWidthPx,
            heightPx = totalBottomPx - totalTopPx,
            paddingPx = 2.0
        )
        return GlyphLayout(floats.toFloatArray(), bounds)
    }

    fun offsetVertices(vertices: FloatArray, dx: Float, dy: Float): FloatArray {
        if (vertices.isEmpty()) return vertices
        val out = vertices.copyOf()
        var i = 0
        while (i + 1 < out.size) {
            out[i] += dx
            out[i + 1] += dy
            i += 2
        }
        return out
    }

    private fun normalizeText(text: String, maxChars: Int): String {
        val source = text.trim().replace('_', ' ').uppercase().take(maxChars)
        if (source.isBlank()) return ""
        val builder = StringBuilder(source.length)
        var previousWasSpace = false
        for (ch in source) {
            val normalized = if (segmentMap.containsKey(ch)) ch else ' '
            if (normalized == ' ') {
                if (!previousWasSpace) builder.append(' ')
                previousWasSpace = true
            } else {
                builder.append(normalized)
                previousWasSpace = false
            }
        }
        return builder.toString().trim()
    }

    private fun appendGlyphs(
        out: FloatArrayBuilder,
        text: String,
        anchor: ClipPoint,
        projector: GeometryProjector,
        pixelSize: Double,
        leftPx: Double,
        topPx: Double
    ) {
        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        val advancePx = CHAR_ADVANCE * pixelSize
        for ((index, char) in text.withIndex()) {
            val glyphLeftPx = leftPx + index * advancePx
            val segments = segmentMap[char] ?: emptyList()
            for (segment in segments) {
                appendSegment(out, segment, anchor, sx, sy, pixelSize, glyphLeftPx, topPx)
            }
        }
    }

    private fun appendSegment(
        out: FloatArrayBuilder,
        segment: Segment,
        anchor: ClipPoint,
        sx: Double,
        sy: Double,
        pixelSize: Double,
        glyphLeftPx: Double,
        topPx: Double
    ) {
        val coordinates = segment.coordinates
        appendPoint(out, anchor, sx, sy, glyphLeftPx + coordinates.x0 * pixelSize, topPx + coordinates.y0 * pixelSize)
        appendPoint(out, anchor, sx, sy, glyphLeftPx + coordinates.x1 * pixelSize, topPx + coordinates.y1 * pixelSize)
    }

    private fun appendPoint(
        out: FloatArrayBuilder,
        anchor: ClipPoint,
        sx: Double,
        sy: Double,
        localXpx: Double,
        localYpx: Double
    ) {
        out.add((anchor.x + localXpx * sx).toFloat(), (anchor.y - localYpx * sy).toFloat())
    }

    private fun boundsFor(
        anchor: ClipPoint,
        projector: GeometryProjector,
        leftPx: Double,
        topPx: Double,
        widthPx: Double,
        heightPx: Double,
        paddingPx: Double
    ): LabelBounds {
        val sx = projector.pixelToClipX(1.0).toDouble()
        val sy = projector.pixelToClipY(1.0).toDouble()
        val x0 = anchor.x + (leftPx - paddingPx) * sx
        val x1 = anchor.x + (leftPx + widthPx + paddingPx) * sx
        val y0 = anchor.y - (topPx - paddingPx) * sy
        val y1 = anchor.y - (topPx + heightPx + paddingPx) * sy
        return LabelBounds(
            minX = min(x0, x1).toFloat(),
            minY = min(y0, y1).toFloat(),
            maxX = max(x0, x1).toFloat(),
            maxY = max(y0, y1).toFloat()
        )
    }

    private fun normalizeSounding(label: String): SoundingParts {
        val cleaned = label.trim().replace(',', '.').ifBlank { return SoundingParts("", "") }
        val sign = if (cleaned.startsWith("-")) "-" else ""
        val unsigned = if (sign.isNotEmpty()) cleaned.drop(1) else cleaned
        val dot = unsigned.indexOf('.')
        if (dot < 0) return SoundingParts((sign + unsigned.filter { it.isDigit() }).take(6), "")

        val integer = unsigned.substring(0, dot).filter { it.isDigit() }.ifBlank { "0" }
        val fraction = unsigned.substring(dot + 1).filter { it.isDigit() }.take(1)
        return SoundingParts((sign + integer).take(6), fraction)
    }

    private fun emptyLayout(anchor: ClipPoint): GlyphLayout = GlyphLayout(
        vertices = FloatArray(0),
        bounds = LabelBounds(anchor.x, anchor.y, anchor.x, anchor.y)
    )

    private data class SoundingParts(val main: String, val fraction: String)

    private data class SegmentCoordinates(val x0: Double, val y0: Double, val x1: Double, val y1: Double)

    private enum class Segment(val coordinates: SegmentCoordinates) {
        A(SegmentCoordinates(0.10, 0.00, 0.55, 0.00)),
        B(SegmentCoordinates(0.60, 0.05, 0.60, 0.45)),
        C(SegmentCoordinates(0.60, 0.55, 0.60, 0.95)),
        D(SegmentCoordinates(0.10, 1.00, 0.55, 1.00)),
        E(SegmentCoordinates(0.05, 0.55, 0.05, 0.95)),
        F(SegmentCoordinates(0.05, 0.05, 0.05, 0.45)),
        G(SegmentCoordinates(0.10, 0.50, 0.55, 0.50)),
        DotA(SegmentCoordinates(0.47, 0.92, 0.59, 0.92)),
        DotB(SegmentCoordinates(0.53, 0.86, 0.53, 0.98)),
        DotTop(SegmentCoordinates(0.53, 0.18, 0.53, 0.30)),
        Center(SegmentCoordinates(0.32, 0.00, 0.32, 1.00)),
        CenterLower(SegmentCoordinates(0.32, 0.50, 0.32, 1.00)),
        X1(SegmentCoordinates(0.05, 0.05, 0.60, 0.95)),
        X2(SegmentCoordinates(0.60, 0.05, 0.05, 0.95)),
        KUpper(SegmentCoordinates(0.10, 0.50, 0.60, 0.05)),
        KLower(SegmentCoordinates(0.10, 0.50, 0.60, 0.95)),
        MLeft(SegmentCoordinates(0.05, 0.05, 0.32, 0.45)),
        MRight(SegmentCoordinates(0.60, 0.05, 0.32, 0.45)),
        QTail(SegmentCoordinates(0.42, 0.72, 0.64, 1.06)),
        RLeg(SegmentCoordinates(0.30, 0.52, 0.62, 0.98)),
        VLeft(SegmentCoordinates(0.05, 0.05, 0.32, 0.98)),
        VRight(SegmentCoordinates(0.60, 0.05, 0.32, 0.98)),
        WLeft(SegmentCoordinates(0.05, 0.95, 0.32, 0.55)),
        WRight(SegmentCoordinates(0.60, 0.95, 0.32, 0.55)),
        YLeft(SegmentCoordinates(0.05, 0.05, 0.32, 0.50)),
        YRight(SegmentCoordinates(0.60, 0.05, 0.32, 0.50)),
        Slash(SegmentCoordinates(0.60, 0.04, 0.05, 0.98)),
        Backslash(SegmentCoordinates(0.05, 0.04, 0.60, 0.98)),
        Comma(SegmentCoordinates(0.53, 0.88, 0.42, 1.12)),
        ParenLeft(SegmentCoordinates(0.42, 0.05, 0.22, 0.50)),
        ParenRight(SegmentCoordinates(0.22, 0.05, 0.42, 0.50)),
        PlusH(SegmentCoordinates(0.18, 0.50, 0.50, 0.50)),
        PlusV(SegmentCoordinates(0.34, 0.30, 0.34, 0.70)),
        DegreeA(SegmentCoordinates(0.44, 0.04, 0.58, 0.04)),
        DegreeB(SegmentCoordinates(0.58, 0.04, 0.58, 0.18)),
        DegreeC(SegmentCoordinates(0.58, 0.18, 0.44, 0.18)),
        DegreeD(SegmentCoordinates(0.44, 0.18, 0.44, 0.04))
    }
}
