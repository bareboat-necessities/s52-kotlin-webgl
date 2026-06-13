package io.github.s52.preslib.esri.svg

import kotlin.math.hypot

/**
 * Converts parsed ESRI SVG paths into a conservative triangle mesh.
 *
 * Intentionally keeps the algorithm simple and deterministic:
 * - filled paths are triangulated as polygon fans per closed subpath;
 * - stroked paths are expanded to rectangular segment strips;
 * - unsupported SVG path commands have already been rejected by validation.
 *
 * Later phases can replace this with robust tessellation without changing the
 * generated runtime model or WebGL API.
 */
object EsriSvgMeshGenerator {
    fun generate(document: EsriSvgDocument): List<EsriGeneratedSvgMesh> {
        require(document.isSubsetSupported) {
            "Unsupported ESRI SVG subset in ${document.sourceFile}: " +
                "elements=${document.unsupportedElements}, features=${document.unsupportedFeatures}, commands=${document.unsupportedPathCommands}"
        }
        return document.paths.flatMap { path -> meshesForPath(path) }.filter { it.isRenderable }
    }

    private fun meshesForPath(path: EsriSvgPath): List<EsriGeneratedSvgMesh> {
        val transform = EsriSvgTransformParser.parse(path.transform)
        val polylines = EsriSvgPathFlattener.flatten(path.pathData)
            .map { line -> line.map(transform::apply) }
        val out = mutableListOf<EsriGeneratedSvgMesh>()
        val fillPaint = paint(path.fill ?: path.style["fill"])
        if (fillPaint != EsriGeneratedPaint.None) {
            polylines.mapNotNull { fanMesh(it, fillPaint, path.id) }.forEach(out::add)
        }
        val strokePaint = paint(path.stroke ?: path.style["stroke"])
        val strokeWidth = path.strokeWidth ?: path.style["stroke-width"]?.toDoubleOrNull() ?: 0.0
        if (strokePaint != EsriGeneratedPaint.None && strokeWidth > 0.0) {
            polylines.mapNotNull { strokeMesh(it, strokeWidth, strokePaint, path.id) }.forEach(out::add)
        }
        return out
    }

    private fun paint(raw: String?): EsriGeneratedPaint {
        val value = raw?.trim()?.lowercase().orEmpty()
        if (value.isBlank() || value == "none" || value == "transparent") return EsriGeneratedPaint.None
        return when (value) {
            "#231f20", "#000", "#000000", "black", "rgb(0,0,0)", "rgb(35,31,32)" -> EsriGeneratedPaint.Token("CHBLK")
            "#fff", "#ffffff", "white", "rgb(255,255,255)" -> EsriGeneratedPaint.Token("CHWHT")
            else -> EsriGeneratedPaint.LiteralHex(value)
        }
    }

    private fun fanMesh(pointsRaw: List<EsriPoint>, paint: EsriGeneratedPaint, id: String?): EsriGeneratedSvgMesh? {
        val points = withoutDuplicateClose(pointsRaw)
        if (points.size < 3) return null
        val vertices = FloatArray(points.size * 2)
        points.forEachIndexed { index, p ->
            vertices[index * 2] = p.x.toFloat()
            vertices[index * 2 + 1] = p.y.toFloat()
        }
        val indices = ShortArray((points.size - 2) * 3)
        var j = 0
        for (i in 1 until points.size - 1) {
            indices[j++] = 0
            indices[j++] = i.toShort()
            indices[j++] = (i + 1).toShort()
        }
        return EsriGeneratedSvgMesh(vertices, indices, paint, id)
    }

    private fun strokeMesh(points: List<EsriPoint>, width: Double, paint: EsriGeneratedPaint, id: String?): EsriGeneratedSvgMesh? {
        if (points.size < 2) return null
        val vertices = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        val half = width / 2.0
        for (i in 0 until points.size - 1) {
            val a = points[i]
            val b = points[i + 1]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx, dy)
            if (len <= 1.0e-12) continue
            val nx = -dy / len * half
            val ny = dx / len * half
            val base = (vertices.size / 2).toShort()
            append(vertices, a.x + nx, a.y + ny)
            append(vertices, a.x - nx, a.y - ny)
            append(vertices, b.x - nx, b.y - ny)
            append(vertices, b.x + nx, b.y + ny)
            indices += base
            indices += (base + 1).toShort()
            indices += (base + 2).toShort()
            indices += base
            indices += (base + 2).toShort()
            indices += (base + 3).toShort()
        }
        if (indices.isEmpty()) return null
        return EsriGeneratedSvgMesh(vertices.toFloatArray(), indices.toShortArray(), paint, id)
    }

    private fun append(out: MutableList<Float>, x: Double, y: Double) {
        out += x.toFloat()
        out += y.toFloat()
    }

    private fun withoutDuplicateClose(points: List<EsriPoint>): List<EsriPoint> =
        if (points.size > 1 && points.first() == points.last()) points.dropLast(1) else points
}
