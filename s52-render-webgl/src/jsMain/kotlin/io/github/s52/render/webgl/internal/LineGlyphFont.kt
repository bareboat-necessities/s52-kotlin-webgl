package io.github.s52.render.webgl.internal

internal object LineGlyphFont {
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
        '.' to listOf(Segment.Dot),
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
        val sx = projector.pixelToClipX(pixelSize)
        val sy = projector.pixelToClipY(pixelSize)
        val charAdvance = 0.72f
        val floats = ArrayList<Float>()
        val normalized = text.uppercase().take(48)
        val totalWidth = normalized.length * charAdvance
        for ((index, char) in normalized.withIndex()) {
            val x0 = anchor.x + ((index * charAdvance - totalWidth * 0.5f) * sx)
            val y0 = anchor.y
            for (segment in (segmentMap[char] ?: listOf(Segment.X1, Segment.X2))) {
                val (a, b) = segmentPoints(segment)
                floats.add(x0 + a.first * sx); floats.add(y0 - a.second * sy)
                floats.add(x0 + b.first * sx); floats.add(y0 - b.second * sy)
            }
        }
        return floats.toFloatArray()
    }

    private fun segmentPoints(segment: Segment): Pair<Pair<Float, Float>, Pair<Float, Float>> = when (segment) {
        Segment.A -> Pair(Pair(0.10f, 0.00f), Pair(0.55f, 0.00f))
        Segment.B -> Pair(Pair(0.60f, 0.05f), Pair(0.60f, 0.45f))
        Segment.C -> Pair(Pair(0.60f, 0.55f), Pair(0.60f, 0.95f))
        Segment.D -> Pair(Pair(0.10f, 1.00f), Pair(0.55f, 1.00f))
        Segment.E -> Pair(Pair(0.05f, 0.55f), Pair(0.05f, 0.95f))
        Segment.F -> Pair(Pair(0.05f, 0.05f), Pair(0.05f, 0.45f))
        Segment.G -> Pair(Pair(0.10f, 0.50f), Pair(0.55f, 0.50f))
        Segment.Dot -> Pair(Pair(0.50f, 1.00f), Pair(0.60f, 0.90f))
        Segment.Center -> Pair(Pair(0.32f, 0.00f), Pair(0.32f, 1.00f))
        Segment.X1 -> Pair(Pair(0.05f, 0.05f), Pair(0.60f, 0.95f))
        Segment.X2 -> Pair(Pair(0.60f, 0.05f), Pair(0.05f, 0.95f))
    }

    private enum class Segment { A, B, C, D, E, F, G, Dot, Center, X1, X2 }
}
