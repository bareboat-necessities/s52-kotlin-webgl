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
        writeReport(summary, reportDir.resolve("generated-vector-symbols.json"))
        if (summary.failedSymbolCount > 0) {
            System.err.println("Generated ${summary.generatedSymbolCount} ESRI vector symbols; ${summary.failedSymbolCount} failed. See ${reportDir.resolve("generated-vector-symbols.json")}")
            exitProcess(1)
        }
        println("Generated ${summary.generatedSymbolCount} ESRI vector symbols to ${outputFile.path}")
    }

    private fun writeReport(summary: EsriKotlinGenerationSummary, file: File) {
        file.writeText(buildString {
            appendLine("{")
            appendLine("  \"generatedFile\": \"${summary.generatedFile.invariantSeparatorsPath}\",")
            appendLine("  \"generatedSymbolCount\": ${summary.generatedSymbolCount},")
            appendLine("  \"failedSymbolCount\": ${summary.failedSymbolCount},")
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
