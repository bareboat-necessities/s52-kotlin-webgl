package io.github.s52.preslib.esri.generator

import io.github.s52.preslib.esri.importer.EsriSvgCategory
import java.io.File
import kotlin.system.exitProcess

object GenerateEsriVectorPatternsMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 3) {
            "Usage: GenerateEsriVectorPatternsMain <esri-source-dir> <output-kotlin-file> <report-dir>"
        }
        val summary = EsriSvgAssetKotlinGenerator.generate(
            sourceRoot = File(args[0]),
            outputFile = File(args[1]),
            category = EsriSvgCategory.PATTERN,
            registryKind = RegistryKind.PATTERN
        )
        val reportDir = File(args[2]).apply { mkdirs() }
        val reportFile = reportDir.resolve("generated-vector-patterns.json")
        writeReport(summary, reportFile, "generatedPatternCount")
        if (summary.failedAssetCount > 0) {
            EsriGenerationFailurePolicy.warnPartialGeneration(
                kind = "patterns",
                generated = summary.generatedAssetCount,
                failed = summary.failedAssetCount,
                reportPath = reportFile.path
            )
            if (EsriGenerationFailurePolicy.failOnSvgAssetFailures()) {
                exitProcess(1)
            }
        }
        println("Generated ${summary.generatedAssetCount} ESRI vector area patterns to ${summary.generatedFile.path}")
    }
}
