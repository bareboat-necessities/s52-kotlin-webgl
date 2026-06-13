package io.github.s52.preslib.esri.vector

/**
 * Runtime-side ESRI vector asset model.
 *
 * ESRI-3 generated point-symbol meshes. Phases ESRI-8/9 extend the same
 * generated Kotlin mesh model to complex line styles and area-pattern tiles.
 * Runtime renderers consume triangles directly; they must not parse SVG files.
 */
data class EsriSvgViewBox(
    val minX: Float,
    val minY: Float,
    val width: Float,
    val height: Float
) {
    val isValid: Boolean get() = width > 0f && height > 0f
}

enum class EsriVectorCategory { POINT, LINE, PATTERN, UNKNOWN }

enum class EsriPrimitiveType { TRIANGLES }

sealed interface EsriPaint {
    data class Token(val token: String) : EsriPaint
    data class LiteralHex(val hex: String) : EsriPaint
    data object None : EsriPaint
}

data class EsriMesh(
    /** Packed x/y pairs in source SVG viewBox units. */
    val vertices: FloatArray,
    /** Triangle indices into vertices. */
    val indices: ShortArray,
    val paint: EsriPaint,
    val primitive: EsriPrimitiveType = EsriPrimitiveType.TRIANGLES
) {
    val vertexCount: Int get() = vertices.size / 2
    val triangleCount: Int get() = indices.size / 3
    val isRenderable: Boolean get() = vertexCount > 0 && triangleCount > 0 && paint !is EsriPaint.None

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EsriMesh) return false
        return vertices.contentEquals(other.vertices) &&
            indices.contentEquals(other.indices) &&
            paint == other.paint &&
            primitive == other.primitive
    }

    override fun hashCode(): Int {
        var result = vertices.contentHashCode()
        result = 31 * result + indices.contentHashCode()
        result = 31 * result + paint.hashCode()
        result = 31 * result + primitive.hashCode()
        return result
    }
}

data class EsriVectorSymbol(
    val name: String,
    val category: EsriVectorCategory,
    val viewBox: EsriSvgViewBox,
    val widthMm: Float?,
    val heightMm: Float?,
    val pivotX: Float,
    val pivotY: Float,
    val meshes: List<EsriMesh>
) {
    val isRenderable: Boolean get() = viewBox.isValid && meshes.any { it.isRenderable }
}

data class EsriVectorLineStyle(
    val name: String,
    val viewBox: EsriSvgViewBox,
    val widthMm: Float?,
    val heightMm: Float?,
    val repeatMm: Float,
    val pivotX: Float,
    val pivotY: Float,
    val meshes: List<EsriMesh>
) {
    val isRenderable: Boolean get() = viewBox.isValid && repeatMm > 0f && meshes.any { it.isRenderable }
}

data class EsriVectorAreaPattern(
    val name: String,
    val viewBox: EsriSvgViewBox,
    val tileWidthMm: Float,
    val tileHeightMm: Float,
    val pivotX: Float,
    val pivotY: Float,
    val meshes: List<EsriMesh>
) {
    val isRenderable: Boolean get() = viewBox.isValid && tileWidthMm > 0f && tileHeightMm > 0f && meshes.any { it.isRenderable }
}

object EmptyEsriVectorSymbolRegistry {
    val symbols: Map<String, EsriVectorSymbol> = emptyMap()
}

object EmptyEsriVectorLineRegistry {
    val lines: Map<String, EsriVectorLineStyle> = emptyMap()
}

object EmptyEsriVectorPatternRegistry {
    val patterns: Map<String, EsriVectorAreaPattern> = emptyMap()
}
