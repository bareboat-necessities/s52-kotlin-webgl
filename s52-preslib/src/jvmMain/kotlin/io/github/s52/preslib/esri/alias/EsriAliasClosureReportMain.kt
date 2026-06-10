package io.github.s52.preslib.esri.alias

import io.github.s52.preslib.esri.importer.EsriCustomSymbolMapParser
import io.github.s52.preslib.esri.importer.EsriJson
import io.github.s52.preslib.esri.importer.EsriSourceLayout
import java.io.File

object EsriAliasClosureReportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 5) {
            "Usage: EsriAliasClosureReportMain <esri-source-dir> <OpenCpnGeneratedPresLib.kt> <generated-symbol-registry.kt> <alias-dir> <report-dir>"
        }
        val sourceRoot = File(args[0])
        val openCpnGenerated = File(args[1])
        val generatedSymbols = File(args[2])
        val aliasDir = File(args[3])
        val reportDir = File(args[4]).apply { mkdirs() }
        val layout = EsriSourceLayout(sourceRoot)
        layout.requireUsable()

        val map = EsriCustomSymbolMapParser.parse(layout.customSymbolMap)
        val directEsriSymbols = map.symbolNames
        val generatedEsriSymbols = readGeneratedSymbolNames(generatedSymbols)
        val aliases = EsriAliasTableReader.read(aliasDir.resolve("esri-symbol-aliases.tsv"))
        val openCpnRefs = extractOpenCpnSymbolCandidates(openCpnGenerated)

        val rows = openCpnRefs.map { ref ->
            val alias = aliases.firstOrNull { it.sourceName == ref }
            val status = when {
                ref in generatedEsriSymbols -> "native_esri_generated_symbol"
                ref in directEsriSymbols -> "native_esri_direct_symbol"
                alias != null && (alias.targetName in generatedEsriSymbols || alias.targetName in directEsriSymbols || alias.targetName.startsWith("GENERATED_")) -> "aliased"
                alias != null -> "alias_target_missing"
                else -> "missing_alias_or_generated_symbol"
            }
            AliasClosureRow(ref, alias?.targetName.orEmpty(), status, alias?.confidence.orEmpty(), alias?.reason.orEmpty())
        }.sortedWith(compareBy<AliasClosureRow> { it.status }.thenBy { it.sourceName })

        reportDir.resolve("alias-closure-report.csv").writeText(buildString {
            appendLine("sourceName,targetName,status,confidence,reason")
            rows.forEach { row ->
                appendLine(listOf(row.sourceName, row.targetName, row.status, row.confidence, row.reason).joinToString(",") { csv(it) })
            }
        })
        reportDir.resolve("alias-closure-report.json").writeText(buildString {
            appendLine("{")
            appendLine("  \"openCpnSymbolCandidateCount\": ${openCpnRefs.size},")
            appendLine("  \"generatedEsriSymbolCount\": ${generatedEsriSymbols.size},")
            appendLine("  \"directEsriSymbolCount\": ${directEsriSymbols.size},")
            appendLine("  \"aliasCount\": ${aliases.size},")
            appendLine("  \"missingCount\": ${rows.count { it.status == "missing_alias_or_generated_symbol" }},")
            appendLine("  \"missing\": ${EsriJson.stringArray(rows.filter { it.status == "missing_alias_or_generated_symbol" }.map { it.sourceName })}")
            appendLine("}")
        })
        println("Wrote ESRI alias closure report with ${rows.size} OpenCPN symbol candidates to ${reportDir.path}")
    }

    private fun readGeneratedSymbolNames(file: File): Set<String> {
        if (!file.isFile) return emptySet()
        val text = file.readText()
        return Regex("\"([^\"]+\\.svg)\"\\s+to\\s+EsriVectorSymbol").findAll(text).map { it.groupValues[1] }.toSortedSet()
    }

    private fun extractOpenCpnSymbolCandidates(file: File): Set<String> {
        if (!file.isFile) return emptySet()
        val text = file.readText()
        val candidates = linkedSetOf<String>()
        Regex("SY\\(([^),]+)").findAll(text).forEach { candidates += it.groupValues[1].trim().trim('"') }
        Regex("LS\\(([^),]+)").findAll(text).forEach { candidates += it.groupValues[1].trim().trim('"') }
        Regex("AP\\(([^),]+)").findAll(text).forEach { candidates += it.groupValues[1].trim().trim('"') }
        Regex("SourceSymbol\\(\\s*name\\s*=\\s*\"([^\"]+)\"").findAll(text).forEach { candidates += it.groupValues[1] }
        Regex("SourceLineStyle\\(\\s*name\\s*=\\s*\"([^\"]+)\"").findAll(text).forEach { candidates += it.groupValues[1] }
        Regex("SourcePattern\\(\\s*name\\s*=\\s*\"([^\"]+)\"").findAll(text).forEach { candidates += it.groupValues[1] }
        // OpenCPN generated pack stores compact TSV blobs. Symbol names normally appear at the start of data lines.
        Regex("(?:^|\\n)([A-Z0-9_]{4,})\\t").findAll(text).forEach { match ->
            val value = match.groupValues[1]
            if (!value.startsWith("LOOKUP") && !value.startsWith("COLOR")) candidates += value
        }
        return candidates.filter { it.isNotBlank() }.toSortedSet()
    }

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}

private data class AliasClosureRow(
    val sourceName: String,
    val targetName: String,
    val status: String,
    val confidence: String,
    val reason: String
)
