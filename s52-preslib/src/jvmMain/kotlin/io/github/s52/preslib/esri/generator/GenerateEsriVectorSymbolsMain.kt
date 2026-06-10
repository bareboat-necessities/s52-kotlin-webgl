package io.github.s52.preslib.esri.generator

import java.io.File
import kotlin.system.exitProcess

object GenerateEsriVectorSymbolsMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 3) {
            "Usage: GenerateEsriVectorSymbolsMain <esri-source-dir> <output-kotlin-file> <report-dir>"
        }
        val sourceDir = File(args[0])
        val outputFile = File(args[1])
        val reportDir = File(args[2]).apply { mkdirs() }
        val summary = EsriSvgKotlinGenerator.generate(sourceDir, outputFile)
        val reportFile = reportDir.resolve("generated-vector-symbols.json")
        writeReport(summary, reportFile)
        if (summary.failedSymbolCount > 0) {
            EsriGenerationFailurePolicy.warnPartialGeneration(
                kind = "symbols",
                generated = summary.generatedSymbolCount,
                failed = summary.failedSymbolCount,
                reportPath = reportFile.path
            )
            if (EsriGenerationFailurePolicy.failOnSvgAssetFailures()) {
                exitProcess(1)
            }
        }
        println("Generated ${summary.generatedSymbolCount} ESRI vector symbols to ${outputFile.path}")
    }

    private fun writeReport(summary: EsriKotlinGenerationSummary, file: File) {
        file.writeText(buildString {
            appendLine("{")
            appendLine("  \"generatedFile\": \"${summary.generatedFile.invariantSeparatorsPath}\",")
            appendLine("  \"generatedSymbolCount\": ${summary.generatedSymbolCount},")
            appendLine("  \"failedSymbolCount\": ${summary.failedSymbolCount},")
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
}
