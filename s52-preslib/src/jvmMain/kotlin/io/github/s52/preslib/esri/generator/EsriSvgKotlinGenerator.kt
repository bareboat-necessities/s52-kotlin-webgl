package io.github.s52.preslib.esri.generator

import io.github.s52.preslib.esri.importer.EsriSourceLayout
import io.github.s52.preslib.esri.svg.EsriGeneratedPaint
import io.github.s52.preslib.esri.svg.EsriGeneratedSvgMesh
import io.github.s52.preslib.esri.svg.EsriSvgMeshGenerator
import io.github.s52.preslib.esri.svg.EsriSvgParser
import java.io.File

object EsriSvgKotlinGenerator {
    fun generate(sourceRoot: File, outputFile: File): EsriKotlinGenerationSummary {
        val layout = EsriSourceLayout(sourceRoot)
        layout.requireUsable()
        val symbols = layout.svgFiles().mapNotNull { svg ->
            val category = layout.svgCategory(svg).name
            val document = EsriSvgParser.parse(svg, category)
            if (!document.isSubsetSupported) {
                return@mapNotNull GeneratedSymbolFailure(
                    name = svg.name,
                    relativePath = svg.relativeTo(sourceRoot).invariantSeparatorsPath,
                    reason = "unsupported subset: elements=${document.unsupportedElements}, features=${document.unsupportedFeatures}, commands=${document.unsupportedPathCommands}"
                )
            }
            val meshes = EsriSvgMeshGenerator.generate(document)
            if (meshes.isEmpty()) {
                GeneratedSymbolFailure(
                    name = svg.name,
                    relativePath = svg.relativeTo(sourceRoot).invariantSeparatorsPath,
                    reason = "no renderable meshes"
                )
            } else {
                GeneratedSymbolSuccess(
                    name = svg.name,
                    relativePath = svg.relativeTo(sourceRoot).invariantSeparatorsPath,
                    category = category,
                    widthMm = document.widthMm,
                    heightMm = document.heightMm,
                    viewBox = document.viewBox,
                    meshes = meshes
                )
            }
        }

        val successes = symbols.filterIsInstance<GeneratedSymbolSuccess>()
        val failures = symbols.filterIsInstance<GeneratedSymbolFailure>()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(renderRegistry(successes))
        return EsriKotlinGenerationSummary(
            generatedFile = outputFile,
            generatedSymbolCount = successes.size,
            failedSymbolCount = failures.size,
            failures = failures
        )
    }

    private fun renderRegistry(symbols: List<GeneratedSymbolSuccess>): String = buildString {
        appendLine("package io.github.s52.preslib.esri.generated")
        appendLine()
        appendLine("import io.github.s52.preslib.esri.vector.EsriMesh")
        appendLine("import io.github.s52.preslib.esri.vector.EsriPaint")
        appendLine("import io.github.s52.preslib.esri.vector.EsriPrimitiveType")
        appendLine("import io.github.s52.preslib.esri.vector.EsriSvgViewBox")
        appendLine("import io.github.s52.preslib.esri.vector.EsriVectorCategory")
        appendLine("import io.github.s52.preslib.esri.vector.EsriVectorSymbol")
        appendLine()
        appendLine("/** Generated from ESRI nautical-chart-symbols SVG assets by Phase ESRI-3. */")
        appendLine("object EsriGeneratedSymbolRegistry {")
        appendLine("    const val SYMBOL_COUNT: Int = ${symbols.size}")
        appendLine("    val symbols: Map<String, EsriVectorSymbol> = linkedMapOf(")
        symbols.forEachIndexed { index, symbol ->
            append("        ")
            append(kq(symbol.name))
            append(" to ")
            append(renderSymbol(symbol))
            if (index != symbols.lastIndex) append(',')
            appendLine()
        }
        appendLine("    )")
        appendLine("}")
    }

    private fun renderSymbol(symbol: GeneratedSymbolSuccess): String = buildString {
        append("EsriVectorSymbol(")
        append("name = ${kq(symbol.name)}, ")
        append("category = EsriVectorCategory.${symbol.category}, ")
        val viewBox = symbol.viewBox
        if (viewBox == null) {
            append("viewBox = EsriSvgViewBox(0f, 0f, 1f, 1f), ")
        } else {
            append("viewBox = EsriSvgViewBox(${kf(viewBox.minX)}, ${kf(viewBox.minY)}, ${kf(viewBox.width)}, ${kf(viewBox.height)}), ")
        }
        append("widthMm = ${symbol.widthMm?.let(::kf) ?: "null"}, ")
        append("heightMm = ${symbol.heightMm?.let(::kf) ?: "null"}, ")
        append("pivotX = 0f, pivotY = 0f, ")
        append("meshes = listOf(")
        symbol.meshes.forEachIndexed { index, mesh ->
            append(renderMesh(mesh))
            if (index != symbol.meshes.lastIndex) append(", ")
        }
        append(")")
        append(")")
    }

    private fun renderMesh(mesh: EsriGeneratedSvgMesh): String = buildString {
        append("EsriMesh(")
        append("vertices = floatArrayOf(${mesh.vertices.joinToString { kf(it.toDouble()) }}), ")
        append("indices = shortArrayOf(${mesh.indices.joinToString { it.toString() }}), ")
        append("paint = ${renderPaint(mesh.paint)}, ")
        append("primitive = EsriPrimitiveType.TRIANGLES")
        append(")")
    }

    private fun renderPaint(paint: EsriGeneratedPaint): String = when (paint) {
        is EsriGeneratedPaint.Token -> "EsriPaint.Token(${kq(paint.token)})"
        is EsriGeneratedPaint.LiteralHex -> "EsriPaint.LiteralHex(${kq(paint.hex)})"
        EsriGeneratedPaint.None -> "EsriPaint.None"
    }

    private fun kq(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").let { "\"$it\"" }
    private fun kf(value: Double): String = if (value.isFinite()) {
        val text = "%.6f".format(java.util.Locale.US, value).trimEnd('0').trimEnd('.')
        (if (text == "-0") "0" else text) + "f"
    } else {
        "0f"
    }
}

sealed interface GeneratedSymbolResult

data class GeneratedSymbolSuccess(
    val name: String,
    val relativePath: String,
    val category: String,
    val widthMm: Double?,
    val heightMm: Double?,
    val viewBox: io.github.s52.preslib.esri.svg.EsriSvgViewBox?,
    val meshes: List<EsriGeneratedSvgMesh>
) : GeneratedSymbolResult

data class GeneratedSymbolFailure(
    val name: String,
    val relativePath: String,
    val reason: String
) : GeneratedSymbolResult

data class EsriKotlinGenerationSummary(
    val generatedFile: File,
    val generatedSymbolCount: Int,
    val failedSymbolCount: Int,
    val failures: List<GeneratedSymbolFailure>
)
