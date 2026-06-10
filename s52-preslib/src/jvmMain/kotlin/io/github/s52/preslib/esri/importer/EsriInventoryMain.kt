package io.github.s52.preslib.esri.importer

import io.github.s52.preslib.esri.svg.EsriSvgParser
import java.io.File

/**
 * Phase ESRI-1 inventory entry point.
 *
 * Args:
 * 0: ESRI nautical-chart-symbols source root
 * 1: output report directory
 */
object EsriInventoryMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size >= 2) { "Usage: EsriInventoryMain <esri-source-root> <report-dir>" }
        val layout = EsriSourceLayout(File(args[0]))
        val reportDir = File(args[1]).also { it.mkdirs() }
        val missing = layout.missingRequiredPaths()
        if (missing.isNotEmpty()) {
            writeMissingSourceReport(reportDir, layout, missing)
            throw IllegalStateException(
                "ESRI source is incomplete. See ${reportDir.resolve("inventory.json").path}. Missing: ${missing.joinToString()}"
            )
        }

        val map = EsriCustomSymbolMapParser.parse(layout.customSymbolMap)
        val svgFiles = layout.svgFiles()
        val svgSummaries = svgFiles.map { file ->
            val parsed = EsriSvgParser.parse(file, layout.svgCategory(file).name.lowercase())
            SvgSummary(
                path = file.relativeTo(layout.root).invariantSeparatorsPath,
                category = layout.svgCategory(file).name,
                width = parsed.widthRaw.orEmpty(),
                height = parsed.heightRaw.orEmpty(),
                viewBox = parsed.viewBox?.let { "${it.minX} ${it.minY} ${it.width} ${it.height}" }.orEmpty(),
                pathCount = parsed.paths.size,
                unsupported = parsed.unsupportedElements + parsed.unsupportedFeatures + parsed.unsupportedPathCommands
            )
        }
        val luaFiles = layout.luaFiles()

        val inventoryJson = buildString {
            appendLine("{")
            appendLine("  \"sourcePresent\": true,")
            appendLine("  \"sourceRoot\": ${EsriJson.quote(layout.root.absolutePath)},")
            appendLine("  \"customSymbolMap\": {")
            appendLine("    \"name\": ${EsriJson.quote(map.name)},")
            appendLine("    \"alias\": ${EsriJson.quote(map.alias)},")
            appendLine("    \"symbolScale\": ${EsriJson.quote(map.symbolScale)},")
            appendLine("    \"version\": ${EsriJson.quote(map.version)},")
            appendLine("    \"featureRules\": ${map.features.size},")
            appendLine("    \"directSymbolConditions\": ${map.directSymbolConditionCount},")
            appendLine("    \"functionConditions\": ${map.functionConditionCount},")
            appendLine("    \"objects\": ${EsriJson.stringArray(map.objectNames)},")
            appendLine("    \"symbolsReferenced\": ${EsriJson.stringArray(map.symbolNames)},")
            appendLine("    \"functionsReferenced\": ${EsriJson.stringArray(map.functionNames)}")
            appendLine("  },")
            appendLine("  \"assets\": {")
            appendLine("    \"pointSvgCount\": ${svgSummaries.count { it.category == "POINT" }},")
            appendLine("    \"lineSvgCount\": ${svgSummaries.count { it.category == "LINE" }},")
            appendLine("    \"patternSvgCount\": ${svgSummaries.count { it.category == "PATTERN" }},")
            appendLine("    \"luaCount\": ${luaFiles.size}")
            appendLine("  },")
            appendLine("  \"svgFiles\": [")
            appendLine(svgSummaries.joinToString(",\n") { it.toJson().prependIndent("    ") })
            appendLine("  ],")
            appendLine("  \"luaFiles\": ${EsriJson.stringArray(luaFiles.map { it.relativeTo(layout.root).invariantSeparatorsPath })}")
            appendLine("}")
        }
        reportDir.resolve("inventory.json").writeText(inventoryJson)
        reportDir.resolve("esri-inventory.txt").writeText(
            "ESRI source inventory\n" +
                "Source root: ${layout.root.absolutePath}\n" +
                "Feature rules: ${map.features.size}\n" +
                "Direct symbol conditions: ${map.directSymbolConditionCount}\n" +
                "Function conditions: ${map.functionConditionCount}\n" +
                "Point SVGs: ${svgSummaries.count { it.category == "POINT" }}\n" +
                "Line SVGs: ${svgSummaries.count { it.category == "LINE" }}\n" +
                "Pattern SVGs: ${svgSummaries.count { it.category == "PATTERN" }}\n" +
                "Lua files: ${luaFiles.size}\n"
        )
    }

    private fun writeMissingSourceReport(reportDir: File, layout: EsriSourceLayout, missing: List<String>) {
        reportDir.resolve("inventory.json").writeText(
            """
            {
              "sourcePresent": false,
              "sourceRoot": ${EsriJson.quote(layout.root.absolutePath)},
              "missing": ${EsriJson.stringArray(missing)}
            }
            """.trimIndent() + "\n"
        )
    }

    private data class SvgSummary(
        val path: String,
        val category: String,
        val width: String,
        val height: String,
        val viewBox: String,
        val pathCount: Int,
        val unsupported: List<String>
    ) {
        fun toJson(): String = buildString {
            append("{")
            append("\"path\": ${EsriJson.quote(path)}, ")
            append("\"category\": ${EsriJson.quote(category)}, ")
            append("\"width\": ${EsriJson.quote(width)}, ")
            append("\"height\": ${EsriJson.quote(height)}, ")
            append("\"viewBox\": ${EsriJson.quote(viewBox)}, ")
            append("\"pathCount\": $pathCount, ")
            append("\"unsupported\": ${EsriJson.stringArray(unsupported)}")
            append("}")
        }
    }
}
