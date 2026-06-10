package io.github.s52.preslib.esri.coverage

import java.io.File

object EsriStrictCoverageClosureMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 7) {
            "Usage: EsriStrictCoverageClosureMain <OpenCpnGeneratedPresLib.kt> <symbol-registry.kt> <line-registry.kt> <pattern-registry.kt> <alias-dir> <report-dir> <strict:true|false>"
        }
        val openCpnGenerated = File(args[0])
        val symbolRegistry = File(args[1])
        val lineRegistry = File(args[2])
        val patternRegistry = File(args[3])
        val aliasDir = File(args[4])
        val reportDir = File(args[5]).apply { mkdirs() }
        val strict = args[6].toBooleanStrictOrNull() ?: true

        val openCpnRefs = extractOpenCpnRefs(openCpnGenerated)
        val generatedSymbols = readRegistryNames(symbolRegistry)
        val generatedLines = readRegistryNames(lineRegistry)
        val generatedPatterns = readRegistryNames(patternRegistry)
        val aliases = readAliases(aliasDir)

        val rows = openCpnRefs.map { ref ->
            val direct = ref in generatedSymbols || ref in generatedLines || ref in generatedPatterns ||
                "$ref.svg" in generatedSymbols || "$ref.svg" in generatedLines || "$ref.svg" in generatedPatterns
            val aliasTarget = aliases[ref]
            val aliasOk = aliasTarget != null && (aliasTarget.startsWith("GENERATED_") ||
                aliasTarget in generatedSymbols || aliasTarget in generatedLines || aliasTarget in generatedPatterns ||
                aliasTarget.removeSuffix(".svg") in generatedSymbols ||
                aliasTarget.removeSuffix(".svg") in generatedLines ||
                aliasTarget.removeSuffix(".svg") in generatedPatterns)
            CoverageRow(
                name = ref,
                status = when {
                    direct -> "native_generated"
                    aliasOk -> "aliased"
                    aliasTarget != null -> "alias_target_missing"
                    else -> "missing"
                },
                target = aliasTarget.orEmpty()
            )
        }.sortedWith(compareBy<CoverageRow> { it.status }.thenBy { it.name })

        val missing = rows.filter { it.status == "missing" || it.status == "alias_target_missing" }
        reportDir.resolve("missing-esri-coverage.csv").writeText(buildString {
            appendLine("name,status,target")
            missing.forEach { appendLine(listOf(it.name, it.status, it.target).joinToString(",") { csv(it) }) }
        })
        reportDir.resolve("strict-coverage.json").writeText(buildString {
            appendLine("{")
            appendLine("  \"openCpnReferenceCount\": ${openCpnRefs.size},")
            appendLine("  \"generatedSymbolCount\": ${generatedSymbols.size},")
            appendLine("  \"generatedLineCount\": ${generatedLines.size},")
            appendLine("  \"generatedPatternCount\": ${generatedPatterns.size},")
            appendLine("  \"aliasCount\": ${aliases.size},")
            appendLine("  \"missingCount\": ${missing.size},")
            appendLine("  \"strict\": $strict")
            appendLine("}")
        })

        println("ESRI strict coverage: ${openCpnRefs.size} references, ${missing.size} unresolved")
        if (strict) {
            check(generatedSymbols.isNotEmpty() || generatedLines.isNotEmpty() || generatedPatterns.isNotEmpty()) {
                "No generated ESRI vector assets were found. Run vector generation before strict coverage."
            }
            check(missing.isEmpty()) {
                "ESRI strict coverage is not closed: ${missing.size} unresolved references. See ${reportDir.resolve("missing-esri-coverage.csv").path}"
            }
        }
    }

    private fun extractOpenCpnRefs(file: File): Set<String> {
        if (!file.isFile) return emptySet()
        val text = file.readText()
        val candidates = linkedSetOf<String>()
        Regex("SY\\(([^),]+)").findAll(text).forEach { candidates += clean(it.groupValues[1]) }
        Regex("LS\\(([^),]+)").findAll(text).forEach { candidates += clean(it.groupValues[1]) }
        Regex("AP\\(([^),]+)").findAll(text).forEach { candidates += clean(it.groupValues[1]) }
        Regex("SourceSymbol\\(\\s*name\\s*=\\s*\"([^\"]+)\"").findAll(text).forEach { candidates += it.groupValues[1] }
        Regex("SourceLineStyle\\(\\s*name\\s*=\\s*\"([^\"]+)\"").findAll(text).forEach { candidates += it.groupValues[1] }
        Regex("SourcePattern\\(\\s*name\\s*=\\s*\"([^\"]+)\"").findAll(text).forEach { candidates += it.groupValues[1] }
        Regex("(?:^|\\n)([A-Z0-9_]{4,})\\t").findAll(text).forEach { match ->
            val value = match.groupValues[1]
            if (!value.startsWith("LOOKUP") && !value.startsWith("COLOR")) candidates += value
        }
        return candidates.filter { it.isNotBlank() }.toSortedSet()
    }

    private fun readRegistryNames(file: File): Set<String> {
        if (!file.isFile) return emptySet()
        val text = file.readText()
        val names = linkedSetOf<String>()
        Regex("\"([^\"]+)\"\\s+to\\s+EsriVector").findAll(text).forEach { names += it.groupValues[1] }
        Regex("name\\s*=\\s*\"([^\"]+)\"").findAll(text).forEach { names += it.groupValues[1] }
        return names.toSortedSet()
    }

    private fun readAliases(aliasDir: File): Map<String, String> = listOf(
        "esri-symbol-aliases.tsv",
        "esri-line-aliases.tsv",
        "esri-pattern-aliases.tsv"
    ).flatMap { fileName ->
        val file = aliasDir.resolve(fileName)
        if (!file.isFile) emptyList() else file.readLines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size >= 2) clean(parts[0]) to clean(parts[1]) else null
            }
    }.toMap()

    private fun clean(value: String): String = value.trim().trim('"', '\'', ' ')
    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}

private data class CoverageRow(val name: String, val status: String, val target: String)
