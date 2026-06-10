package io.github.s52.preslib.esri.svg

import java.io.File

data class EsriSvgDocument(
    val sourceFile: File,
    val category: String,
    val widthRaw: String?,
    val heightRaw: String?,
    val widthMm: Double?,
    val heightMm: Double?,
    val viewBox: EsriSvgViewBox?,
    val paths: List<EsriSvgPath>,
    val unsupportedElements: List<String>,
    val unsupportedFeatures: List<String>,
    val unsupportedPathCommands: List<String>
) {
    val hasGeometry: Boolean get() = paths.any { it.pathData.commands.isNotEmpty() }
    val isSubsetSupported: Boolean
        get() = unsupportedElements.isEmpty() && unsupportedFeatures.isEmpty() && unsupportedPathCommands.isEmpty()
}

data class EsriSvgViewBox(
    val minX: Double,
    val minY: Double,
    val width: Double,
    val height: Double
) {
    val isValid: Boolean get() = width > 0.0 && height > 0.0
}

data class EsriSvgPath(
    val id: String?,
    val d: String,
    val style: Map<String, String>,
    val fill: String?,
    val stroke: String?,
    val strokeWidth: Double?,
    val fillRule: String?,
    val transform: String?,
    val pathData: EsriSvgPathData
)

data class EsriSvgPathData(
    val commands: List<EsriSvgPathCommand>,
    val unsupportedCommands: List<Char>
)

data class EsriSvgPathCommand(
    val command: Char,
    val relative: Boolean,
    val values: List<Double>
)

data class EsriPoint(val x: Double, val y: Double)

data class EsriPolygon(val points: List<EsriPoint>) {
    val isClosed: Boolean get() = points.size >= 3
}

data class EsriGeneratedSvgMesh(
    val vertices: FloatArray,
    val indices: ShortArray,
    val paint: EsriGeneratedPaint,
    val sourcePathId: String?
) {
    val vertexCount: Int get() = vertices.size / 2
    val triangleCount: Int get() = indices.size / 3
    val isRenderable: Boolean get() = vertexCount > 0 && triangleCount > 0 && paint != EsriGeneratedPaint.None
}

sealed interface EsriGeneratedPaint {
    data class LiteralHex(val hex: String) : EsriGeneratedPaint
    data class Token(val token: String) : EsriGeneratedPaint
    data object None : EsriGeneratedPaint
}
