package io.github.s52.preslib.esri.smoke

import io.github.s52.preslib.esri.csp.EsriCspFeature
import io.github.s52.preslib.esri.csp.EsriPortrayalContext
import io.github.s52.preslib.esri.profile.EsriInt1Profile
import io.github.s52.preslib.esri.rules.EsriRuleFeature
import java.io.File

object EsriNoaaSmokeMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: EsriNoaaSmokeMain <fixture.tsv> <report-dir>" }
        val fixture = File(args[0])
        val reportDir = File(args[1]).apply { mkdirs() }
        val rows = readRows(fixture)
        check(rows.isNotEmpty()) { "NOAA smoke fixture has no rows: ${fixture.path}" }

        val results = rows.map { row -> runRow(row) }
        val passed = results.count { it.passed }
        val failed = results.filterNot { it.passed }

        reportDir.resolve("noaa-smoke-report.csv").writeText(buildString {
            appendLine("object,primitive,csp,directActions,cspInstructions,passed,notes")
            results.forEach { result ->
                appendLine(listOf(
                    result.row.objectAcronym,
                    result.row.primitive.toString(),
                    result.row.cspName,
                    result.directActions.toString(),
                    result.cspInstructions.toString(),
                    result.passed.toString(),
                    result.notes
                ).joinToString(",") { csv(it) })
            }
        })
        reportDir.resolve("noaa-smoke-report.json").writeText(
            """
            {
              "fixture": "${json(fixture.absolutePath)}",
              "rowCount": ${rows.size},
              "passed": $passed,
              "failed": ${failed.size},
              "profileSymbolCount": ${EsriInt1Profile.symbols.size},
              "profileLineCount": ${EsriInt1Profile.lines.size},
              "profilePatternCount": ${EsriInt1Profile.patterns.size},
              "profileDirectRuleCount": ${EsriInt1Profile.directRules.size}
            }
            """.trimIndent() + "\n"
        )

        println("ESRI NOAA smoke: $passed/${rows.size} rows passed")
        check(failed.isEmpty()) {
            "ESRI NOAA smoke failed for ${failed.size} rows. See ${reportDir.resolve("noaa-smoke-report.csv").path}"
        }
    }

    private fun runRow(row: SmokeRow): SmokeResult {
        val ruleFeature = EsriRuleFeature(
            objectAcronym = row.objectAcronym,
            primitive = row.primitive,
            attributes = row.attributes.mapValues { (_, value) -> value.split(',').map { it.trim() }.filter { it.isNotEmpty() } }
        )
        val directActions = EsriInt1Profile.directRuleActions(ruleFeature)
        val cspInstructions = if (row.cspName.isBlank()) emptyList() else EsriInt1Profile.cspInstructions(
            row.cspName,
            EsriCspFeature(
                acronym = row.objectAcronym,
                primitive = row.primitive,
                attributes = row.attributes.mapValues { it.value.substringBefore(',') },
                listAttributes = row.attributes.mapValues { (_, value) -> value.split(',').map { it.trim() }.filter { it.isNotEmpty() } },
                leastDepth = row.leastDepth,
                greatestDepth = row.greatestDepth,
                lowAccuracy = row.lowAccuracy
            ),
            EsriPortrayalContext()
        )
        val passed = directActions.isNotEmpty() || cspInstructions.isNotEmpty()
        val notes = when {
            directActions.isNotEmpty() && cspInstructions.isNotEmpty() -> "direct_and_csp"
            directActions.isNotEmpty() -> "direct_rule"
            cspInstructions.isNotEmpty() -> "csp"
            else -> "no_direct_rule_or_csp_output"
        }
        return SmokeResult(row, directActions.size, cspInstructions.size, passed, notes)
    }

    private fun readRows(file: File): List<SmokeRow> {
        check(file.isFile) { "Missing NOAA smoke fixture: ${file.path}" }
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val p = line.split('\t')
                SmokeRow(
                    objectAcronym = p.getOrNull(0).orEmpty(),
                    primitive = p.getOrNull(1)?.toIntOrNull() ?: 1,
                    cspName = p.getOrNull(2).orEmpty(),
                    attributes = parseAttributes(p.getOrNull(3).orEmpty()),
                    leastDepth = p.getOrNull(4)?.toDoubleOrNull(),
                    greatestDepth = p.getOrNull(5)?.toDoubleOrNull(),
                    lowAccuracy = p.getOrNull(6)?.toBooleanStrictOrNull() ?: false
                )
            }
    }

    private fun parseAttributes(value: String): Map<String, String> = value
        .split(';')
        .mapNotNull { assignment ->
            val trimmed = assignment.trim()
            if (trimmed.isBlank() || !trimmed.contains('=')) return@mapNotNull null
            trimmed.substringBefore('=').trim() to trimmed.substringAfter('=').trim()
        }
        .toMap()

    private fun csv(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
    private fun json(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
}

private data class SmokeRow(
    val objectAcronym: String,
    val primitive: Int,
    val cspName: String,
    val attributes: Map<String, String>,
    val leastDepth: Double?,
    val greatestDepth: Double?,
    val lowAccuracy: Boolean
)

private data class SmokeResult(
    val row: SmokeRow,
    val directActions: Int,
    val cspInstructions: Int,
    val passed: Boolean,
    val notes: String
)
