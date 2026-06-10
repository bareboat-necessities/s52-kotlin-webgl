package io.github.s52.preslib.esri.svg

import io.github.s52.preslib.esri.importer.EsriJson
import io.github.s52.preslib.esri.importer.EsriSourceLayout
import java.io.File

/**
 * Phase ESRI-2 SVG subset validator.
 *
 * Args:
 * 0: ESRI nautical-chart-symbols source root
 * 1: output report directory
 */
object ValidateEsriSvgSubsetMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size >= 2) { "Usage: ValidateEsriSvgSubsetMain <esri-source-root> <report-dir>" }
        val layout = EsriSourceLayout(File(args[0]))
        val reportDir = File(args[1]).also { it.mkdirs() }
        layout.requireUsable()

        val rows = layout.svgFiles().map { file ->
            runCatching {
                val svg = EsriSvgParser.parse(file, layout.svgCategory(file).name.lowercase())
                SvgValidationRow(
                    path = file.relativeTo(layout.root).invariantSeparatorsPath,
                    category = layout.svgCategory(file).name,
                    parsed = true,
                    validViewBox = svg.viewBox?.isValid == true,
                    hasGeometry = svg.hasGeometry,
                    pathCount = svg.paths.size,
                    unsupported = svg.unsupportedElements + svg.unsupportedFeatures + svg.unsupportedPathCommands.map { "path-command:$it" },
                    error = null
                )
            }.getOrElse { error ->
                SvgValidationRow(
                    path = file.relativeTo(layout.root).invariantSeparatorsPath,
                    category = layout.svgCategory(file).name,
                    parsed = false,
                    validViewBox = false,
                    hasGeometry = false,
                    pathCount = 0,
                    unsupported = emptyList(),
                    error = error.message ?: error::class.qualifiedName.orEmpty()
                )
            }
        }

        reportDir.resolve("svg-subset-report.csv").writeText(buildString {
            appendLine("path,category,parsed,validViewBox,hasGeometry,pathCount,unsupported,error")
            rows.forEach { row ->
                appendLine(listOf(
                    row.path,
                    row.category,
                    row.parsed.toString(),
                    row.validViewBox.toString(),
                    row.hasGeometry.toString(),
                    row.pathCount.toString(),
                    row.unsupported.joinToString("|"),
                    row.error.orEmpty()
                ).joinToString(",") { csv(it) })
            }
        })

        reportDir.resolve("svg-subset-report.json").writeText(buildString {
            appendLine("{")
            appendLine("  \"total\": ${rows.size},")
            appendLine("  \"parsed\": ${rows.count { it.parsed }},")
            appendLine("  \"withUnsupportedFeatures\": ${rows.count { it.unsupported.isNotEmpty() }},")
            appendLine("  \"withErrors\": ${rows.count { it.error != null }},")
            appendLine("  \"rows\": [")
            appendLine(rows.joinToString(",\n") { it.toJson().prependIndent("    ") })
            appendLine("  ]")
            appendLine("}")
        })

        val invalid = rows.filter { !it.parsed || !it.validViewBox || !it.hasGeometry || it.unsupported.isNotEmpty() }
        check(invalid.isEmpty()) {
            "ESRI SVG subset validation failed for ${invalid.size} file(s). See ${reportDir.resolve("svg-subset-report.csv").path}"
        }
    }

    private data class SvgValidationRow(
        val path: String,
        val category: String,
        val parsed: Boolean,
        val validViewBox: Boolean,
        val hasGeometry: Boolean,
        val pathCount: Int,
        val unsupported: List<String>,
        val error: String?
    ) {
        fun toJson(): String = buildString {
            append("{")
            append("\"path\": ${EsriJson.quote(path)}, ")
            append("\"category\": ${EsriJson.quote(category)}, ")
            append("\"parsed\": $parsed, ")
            append("\"validViewBox\": $validViewBox, ")
            append("\"hasGeometry\": $hasGeometry, ")
            append("\"pathCount\": $pathCount, ")
            append("\"unsupported\": ${EsriJson.stringArray(unsupported)}, ")
            append("\"error\": ${error?.let(EsriJson::quote) ?: "null"}")
            append("}")
        }
    }

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}
