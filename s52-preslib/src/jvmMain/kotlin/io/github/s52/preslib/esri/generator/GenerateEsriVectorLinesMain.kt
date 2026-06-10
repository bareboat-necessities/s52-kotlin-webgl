package io.github.s52.preslib.esri.generator

import io.github.s52.preslib.esri.importer.EsriSvgCategory
import java.io.File
import kotlin.system.exitProcess

object GenerateEsriVectorLinesMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 3) {
            "Usage: GenerateEsriVectorLinesMain <esri-source-dir> <output-kotlin-file> <report-dir>"
        }
        val summary = EsriSvgAssetKotlinGenerator.generate(
            sourceRoot = File(args[0]),
            outputFile = File(args[1]),
            category = EsriSvgCategory.LINE,
            registryKind = RegistryKind.LINE
        )
        val reportDir = File(args[2]).apply { mkdirs() }
        val reportFile = reportDir.resolve("generated-vector-lines.json")
        writeReport(summary, reportFile, "generatedLineCount")
        if (summary.failedAssetCount > 0) {
            EsriGenerationFailurePolicy.warnPartialGeneration(
                kind = "lines",
                generated = summary.generatedAssetCount,
                failed = summary.failedAssetCount,
                reportPath = reportFile.path
            )
            if (EsriGenerationFailurePolicy.failOnSvgAssetFailures()) {
                exitProcess(1)
            }
        }
        println("Generated ${summary.generatedAssetCount} ESRI vector line styles to ${summary.generatedFile.path}")
    }
}

internal fun writeReport(summary: EsriAssetGenerationSummary, file: File, countKey: String) {
    file.writeText(buildString {
        appendLine("{")
        appendLine("  \"generatedFile\": \"${summary.generatedFile.invariantSeparatorsPath}\",")
        appendLine("  \"$countKey\": ${summary.generatedAssetCount},")
        appendLine("  \"failedAssetCount\": ${summary.failedAssetCount},")
        appendLine("  \"strictFailureEnabled\": ${EsriGenerationFailurePolicy.failOnSvgAssetFailures()},")
        appendLine("  \"failures\": [")
        summary.failures.forEachIndexed { index, failure ->
            append("    {\"name\": \"${failure.name}\", \"relativePath\": \"${failure.relativePath}\", \"reason\": \"${failure.reason.replace("\"", "'")}\"}")
            if (index != summary.failures.lastIndex) append(',')
            appendLine()
        }
        appendLine("  ]")
        appendLine("}")
    })
}
