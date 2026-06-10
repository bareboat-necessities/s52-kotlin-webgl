package io.github.s52.preslib.esri.generator

import java.io.File

object GenerateEsriDirectRulesMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 3) { "Usage: GenerateEsriDirectRulesMain <esri-source-dir> <output-kotlin-file> <report-dir>" }
        val summary = EsriRuleKotlinGenerator.generate(File(args[0]), File(args[1]))
        val reportDir = File(args[2]).apply { mkdirs() }
        reportDir.resolve("generated-direct-rules.json").writeText(buildString {
            appendLine("{")
            appendLine("  \"generatedFile\": \"${summary.generatedFile.invariantSeparatorsPath}\",")
            appendLine("  \"ruleCount\": ${summary.ruleCount},")
            appendLine("  \"symbolRuleCount\": ${summary.symbolRuleCount},")
            appendLine("  \"functionRuleCount\": ${summary.functionRuleCount}")
            appendLine("}")
        })
        println("Generated ${summary.ruleCount} ESRI direct/function rules to ${summary.generatedFile.path}")
    }
}
