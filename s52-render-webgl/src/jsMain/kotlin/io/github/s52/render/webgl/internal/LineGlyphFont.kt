package io.github.s52.render.webgl.internal

internal object LineGlyphFont {
    private const val CHAR_ADVANCE: Double = 0.72

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
        '-' to listOf(Segment.G),
        '.' to listOf(Segment.DotA, Segment.DotB),
        'A' to listOf(Segment.A, Segment.B, Segment.C, Segment.E, Segment.F, Segment.G),
        'B' to listOf(Segment.C, Segment.D, Segment.E, Segment.F, Segment.G),
        'C' to listOf(Segment.A, Segment.D, Segment.E, Segment.F),
        'D' to listOf(Segment.B, Segment.C, Segment.D, Segment.E, Segment.G),
        'E' to listOf(Segment.A, Segment.D, Segment.E, Segment.F, Segment.G),
        'F' to listOf(Segment.A, Segment.E, Segment.F, Segment.G),
        'L' to listOf(Segment.D, Segment.E, Segment.F),
        'S' to listOf(Segment.A, Segment.F, Segment.G, Segment.C, Segment.D),
        'T' to listOf(Segment.A, Segment.Center),
        'X' to listOf(Segment.X1, Segment.X2),
        ' ' to emptyList()
    )

    fun lineVertices(text: String, anchor: ClipPoint, projector: GeometryProjector, pixelSize: Double): FloatArray {
        val normalized = text.uppercase().take(48)
        val floats = FloatArrayBuilder(normalized.length * 12)
        val totalWidthPx = normalized.length * CHAR_ADVANCE * pixelSize
        appendGlyphs(
            out = floats,
            text = normalized,
            anchor = anchor,
            projector = projector,
            pixelSize = pixelSize,
            leftPx = -totalWidthPx * 0.5,
            topPx = 0.0
        )
        return floats.toFloatArray()
    }

    /**
     * S-52 sounding labels are easier to read when the decimal part is shown as
     * a small lowered digit instead of a full-size seven-segment decimal string.
     * For example, `12.3` is rendered as `12` plus a small subscript `3`.
     */
    fun soundingVertices(depthLabel: String, anchor: ClipPoint, projector: GeometryProjector): FloatArray {
        val parts = normalizeSounding(depthLabel)
        val mainSize = 11.0
        val fractionSize = 7.0
        val gapPx = if (parts.fraction.isBlank()) 0.0 else 1.5
        val mainWidthPx = parts.main.length * CHAR_ADVANCE * mainSize
        val fractionWidthPx = parts.fraction.length * CHAR_ADVANCE * fractionSize
        val totalWidthPx = mainWidthPx + gapPx + fractionWidthPx
        val leftPx = -totalWidthPx * 0.5

        val floats = FloatArrayBuilder((parts.main.length + parts.fraction.length) * 12)
        appendGlyphs(
            out = floats,
            text = parts.main,
            anchor = anchor,
            projector = projector,
            pixelSize = mainSize,
            leftPx = leftPx,
            topPx = -mainSize * 0.52
        )
        if (parts.fraction.isNotBlank()) {
            appendGlyphs(
                out = floats,
                text = parts.fraction,
                anchor = anchor,
                projector = projector,
                pixelSize = fractionSize,
                leftPx = leftPx + mainWidthPx + gapPx,
                topPx = mainSize * 0.02
            )
        }
        return floats.toFloatArray()
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
            for (segment in (segmentMap[char] ?: listOf(Segment.X1, Segment.X2))) {
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

    private fun normalizeSounding(label: String): SoundingParts {
        val cleaned = label.trim().replace(',', '.').ifBlank { return SoundingParts("", "") }
        val dot = cleaned.indexOf('.')
        if (dot < 0) return SoundingParts(cleaned.uppercase().take(6), "")

        val main = cleaned.substring(0, dot).ifBlank { "0" }.uppercase().take(5)
        val fraction = cleaned.substring(dot + 1).filter { it.isDigit() }.take(1)
        return SoundingParts(main, fraction)
    }

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
        Center(SegmentCoordinates(0.32, 0.00, 0.32, 1.00)),
        X1(SegmentCoordinates(0.05, 0.05, 0.60, 0.95)),
        X2(SegmentCoordinates(0.60, 0.05, 0.05, 0.95))
    }
}
