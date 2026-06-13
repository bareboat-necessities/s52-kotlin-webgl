package io.github.s52.preslib.esri.importer

import java.io.File

/**
 * Initial coverage/gap report.
 *
 * Args:
 * 0: ESRI nautical-chart-symbols source root
 * 1: OpenCpnGeneratedPresLib.kt path
 * 2: output report directory
 */
object EsriCoverageReportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size >= 3) { "Usage: EsriCoverageReportMain <esri-source-root> <OpenCpnGeneratedPresLib.kt> <report-dir>" }
        val layout = EsriSourceLayout(File(args[0]))
        val openCpnGenerated = File(args[1])
        val reportDir = File(args[2]).also { it.mkdirs() }
        layout.requireUsable()
        require(openCpnGenerated.isFile) { "Missing OpenCPN generated pack file: ${openCpnGenerated.path}" }

        val map = EsriCustomSymbolMapParser.parse(layout.customSymbolMap)
        val openCpn = OpenCpnGeneratedCoverageReader.read(openCpnGenerated)
        val esriObjects = map.objectNames
        val requiredObjects = openCpn.requiredObjects
        val matched = requiredObjects.intersect(esriObjects)
        val missing = requiredObjects.subtract(esriObjects)

        reportDir.resolve("opencpn-required-coverage.json").writeText(buildString {
            appendLine("{")
            appendLine("  \"lookupCount\": ${openCpn.lookupCount ?: "null"},")
            appendLine("  \"symbolCount\": ${openCpn.symbolCount ?: "null"},")
            appendLine("  \"lineStyleCount\": ${openCpn.lineStyleCount ?: "null"},")
            appendLine("  \"patternCount\": ${openCpn.patternCount ?: "null"},")
            appendLine("  \"colorTableCount\": ${openCpn.colorTableCount ?: "null"},")
            appendLine("  \"requiredObjectCountHeuristic\": ${requiredObjects.size},")
            appendLine("  \"esriDirectObjectCount\": ${esriObjects.size},")
            appendLine("  \"directObjectMatches\": ${matched.size},")
            appendLine("  \"directObjectMissing\": ${missing.size},")
            appendLine("  \"requiredObjects\": ${EsriJson.stringArray(requiredObjects)},")
            appendLine("  \"esriObjects\": ${EsriJson.stringArray(esriObjects)}")
            appendLine("}")
        })

        reportDir.resolve("initial-gap-report.csv").writeText(buildString {
            appendLine("object,status,source")
            if (requiredObjects.isEmpty()) {
                appendLine(csv("UNKNOWN") + "," + csv("heuristic_object_extraction_empty") + "," + csv(openCpnGenerated.path))
            } else {
                requiredObjects.forEach { obj ->
                    val status = if (obj in esriObjects) "direct_esri_object_rule_present" else "missing_direct_esri_object_rule_or_requires_alias_or_csp"
                    appendLine(listOf(obj, status, "OpenCpnGeneratedPresLib.kt").joinToString(",") { csv(it) })
                }
            }
        })
    }

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}

internal data class OpenCpnGeneratedCoverage(
    val lookupCount: Int?,
    val symbolCount: Int?,
    val lineStyleCount: Int?,
    val patternCount: Int?,
    val colorTableCount: Int?,
    val requiredObjects: Set<String>
)

internal object OpenCpnGeneratedCoverageReader {
    fun read(file: File): OpenCpnGeneratedCoverage {
        val text = file.readText()
        return OpenCpnGeneratedCoverage(
            lookupCount = constInt(text, "LOOKUP_COUNT"),
            symbolCount = constInt(text, "SYMBOL_COUNT"),
            lineStyleCount = constInt(text, "LINE_STYLE_COUNT"),
            patternCount = constInt(text, "PATTERN_COUNT"),
            colorTableCount = constInt(text, "COLOR_TABLE_COUNT"),
            requiredObjects = extractObjectKeys(text)
        )
    }

    private fun constInt(text: String, name: String): Int? = Regex("const\\s+val\\s+$name\\s*:\\s*Int\\s*=\\s*(\\d+)")
        .find(text)
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()

    private fun extractObjectKeys(text: String): Set<String> {
        val candidates = linkedSetOf<String>()
        // Generated lookup TSV rows normally start with an S-57 object acronym, then a primitive enum.
        Regex("(?:^|\\n)([A-Z0-9_]{5,})\\t(?:POINT|LINE|AREA|ANY|Point|Line|Area|[123])\\t").findAll(text)
            .forEach { candidates += it.groupValues[1] }
        // Fallback heuristic: source object keys may also be materialized through S57ObjectClassKey.of("OBJNAM").
        Regex("S57ObjectClassKey\\.of\\(\"([A-Z0-9_]{5,})\"\\)").findAll(text)
            .forEach { candidates += it.groupValues[1] }
        return candidates.toSortedSet()
    }
}
