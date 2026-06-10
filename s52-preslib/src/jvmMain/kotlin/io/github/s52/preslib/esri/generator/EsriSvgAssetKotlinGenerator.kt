package io.github.s52.preslib.esri.generator

import io.github.s52.preslib.esri.importer.EsriSourceLayout
import io.github.s52.preslib.esri.importer.EsriSvgCategory
import io.github.s52.preslib.esri.svg.EsriGeneratedPaint
import io.github.s52.preslib.esri.svg.EsriGeneratedSvgMesh
import io.github.s52.preslib.esri.svg.EsriSvgMeshGenerator
import io.github.s52.preslib.esri.svg.EsriSvgParser
import java.io.File
import java.util.Locale

/** Generates Kotlin registries for ESRI line and area-pattern SVG assets. */
object EsriSvgAssetKotlinGenerator {
    internal fun generate(
        sourceRoot: File,
        outputFile: File,
        category: EsriSvgCategory,
        registryKind: RegistryKind
    ): EsriAssetGenerationSummary {
        val layout = EsriSourceLayout(sourceRoot)
        layout.requireUsable()
        val results = layout.svgFiles()
            .filter { layout.svgCategory(it) == category }
            .map { svg -> resultFor(sourceRoot, layout, svg) }

        val successes = results.filterIsInstance<GeneratedAssetSuccess>()
        val failures = results.filterIsInstance<GeneratedAssetFailure>()
        outputFile.parentFile.mkdirs()
        outputFile.writeText(renderRegistry(successes, registryKind))
        return EsriAssetGenerationSummary(
            generatedFile = outputFile,
            generatedAssetCount = successes.size,
            failedAssetCount = failures.size,
            failures = failures
        )
    }

    private fun resultFor(sourceRoot: File, layout: EsriSourceLayout, svg: File): GeneratedAssetResult {
        val category = layout.svgCategory(svg).name
        val document = EsriSvgParser.parse(svg, category)
        if (!document.isSubsetSupported) {
            return GeneratedAssetFailure(
                name = svg.name,
                relativePath = svg.relativeTo(sourceRoot).invariantSeparatorsPath,
                reason = "unsupported subset: elements=${document.unsupportedElements}, features=${document.unsupportedFeatures}, commands=${document.unsupportedPathCommands}"
            )
        }
        val meshes = EsriSvgMeshGenerator.generate(document)
        return if (meshes.isEmpty()) {
            GeneratedAssetFailure(
                name = svg.name,
                relativePath = svg.relativeTo(sourceRoot).invariantSeparatorsPath,
                reason = "no renderable meshes"
            )
        } else {
            GeneratedAssetSuccess(
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

    private fun renderRegistry(assets: List<GeneratedAssetSuccess>, kind: RegistryKind): String = buildString {
        appendLine("package io.github.s52.preslib.esri.generated")
        appendLine()
        appendLine("import io.github.s52.preslib.esri.vector.EsriMesh")
        appendLine("import io.github.s52.preslib.esri.vector.EsriPaint")
        appendLine("import io.github.s52.preslib.esri.vector.EsriPrimitiveType")
        appendLine("import io.github.s52.preslib.esri.vector.EsriSvgViewBox")
        when (kind) {
            RegistryKind.LINE -> appendLine("import io.github.s52.preslib.esri.vector.EsriVectorLineStyle")
            RegistryKind.PATTERN -> appendLine("import io.github.s52.preslib.esri.vector.EsriVectorAreaPattern")
        }
        appendLine()
        appendLine("/** Generated from ESRI nautical-chart-symbols SVG assets by Phase ESRI-${if (kind == RegistryKind.LINE) "8" else "9"}. */")
        when (kind) {
            RegistryKind.LINE -> {
                appendLine("object EsriGeneratedLineRegistry {")
                appendLine("    const val LINE_COUNT: Int = ${assets.size}")
                appendLine("    val lines: Map<String, EsriVectorLineStyle> = linkedMapOf(")
                assets.forEachIndexed { index, asset ->
                    append("        ${kq(asset.name)} to ${renderLine(asset)}")
                    if (index != assets.lastIndex) append(',')
                    appendLine()
                }
                appendLine("    )")
                appendLine("}")
            }
            RegistryKind.PATTERN -> {
                appendLine("object EsriGeneratedPatternRegistry {")
                appendLine("    const val PATTERN_COUNT: Int = ${assets.size}")
                appendLine("    val patterns: Map<String, EsriVectorAreaPattern> = linkedMapOf(")
                assets.forEachIndexed { index, asset ->
                    append("        ${kq(asset.name)} to ${renderPattern(asset)}")
                    if (index != assets.lastIndex) append(',')
                    appendLine()
                }
                appendLine("    )")
                appendLine("}")
            }
        }
    }

    private fun renderLine(asset: GeneratedAssetSuccess): String = buildString {
        append("EsriVectorLineStyle(")
        append("name = ${kq(asset.name)}, ")
        append("viewBox = ${renderViewBox(asset)}, ")
        append("widthMm = ${asset.widthMm?.let(::kf) ?: "null"}, ")
        append("heightMm = ${asset.heightMm?.let(::kf) ?: "null"}, ")
        val repeat = asset.widthMm ?: asset.viewBox?.width ?: 10.0
        append("repeatMm = ${kf(repeat.coerceAtLeast(0.1))}, ")
        append("pivotX = 0f, pivotY = 0f, ")
        append("meshes = listOf(${asset.meshes.joinToString { renderMesh(it) }})")
        append(")")
    }

    private fun renderPattern(asset: GeneratedAssetSuccess): String = buildString {
        append("EsriVectorAreaPattern(")
        append("name = ${kq(asset.name)}, ")
        append("viewBox = ${renderViewBox(asset)}, ")
        val tileW = asset.widthMm ?: asset.viewBox?.width ?: 10.0
        val tileH = asset.heightMm ?: asset.viewBox?.height ?: 10.0
        append("tileWidthMm = ${kf(tileW.coerceAtLeast(0.1))}, ")
        append("tileHeightMm = ${kf(tileH.coerceAtLeast(0.1))}, ")
        append("pivotX = 0f, pivotY = 0f, ")
        append("meshes = listOf(${asset.meshes.joinToString { renderMesh(it) }})")
        append(")")
    }

    private fun renderViewBox(asset: GeneratedAssetSuccess): String {
        val viewBox = asset.viewBox
        return if (viewBox == null) {
            "EsriSvgViewBox(0f, 0f, 1f, 1f)"
        } else {
            "EsriSvgViewBox(${kf(viewBox.minX)}, ${kf(viewBox.minY)}, ${kf(viewBox.width)}, ${kf(viewBox.height)})"
        }
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
        val text = "%.6f".format(Locale.US, value).trimEnd('0').trimEnd('.')
        (if (text == "-0") "0" else text) + "f"
    } else {
        "0f"
    }
}

enum class RegistryKind { LINE, PATTERN }

sealed interface GeneratedAssetResult

data class GeneratedAssetSuccess(
    val name: String,
    val relativePath: String,
    val category: String,
    val widthMm: Double?,
    val heightMm: Double?,
    val viewBox: io.github.s52.preslib.esri.svg.EsriSvgViewBox?,
    val meshes: List<EsriGeneratedSvgMesh>
) : GeneratedAssetResult

data class GeneratedAssetFailure(
    val name: String,
    val relativePath: String,
    val reason: String
) : GeneratedAssetResult

data class EsriAssetGenerationSummary(
    val generatedFile: File,
    val generatedAssetCount: Int,
    val failedAssetCount: Int,
    val failures: List<GeneratedAssetFailure>
)
