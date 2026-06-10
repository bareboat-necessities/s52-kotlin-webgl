package io.github.s52.preslib.esri.generator

import io.github.s52.preslib.esri.importer.EsriCondition
import io.github.s52.preslib.esri.importer.EsriCustomSymbolMapParser
import io.github.s52.preslib.esri.importer.EsriFeatureRule
import io.github.s52.preslib.esri.importer.EsriRule
import io.github.s52.preslib.esri.importer.EsriSourceLayout
import java.io.File

object EsriRuleKotlinGenerator {
    fun generate(sourceRoot: File, outputFile: File): EsriRuleGenerationSummary {
        val layout = EsriSourceLayout(sourceRoot)
        layout.requireUsable()
        val map = EsriCustomSymbolMapParser.parse(layout.customSymbolMap)
        val generated = mutableListOf<GeneratedEsriRule>()
        var order = 0
        for (feature in map.features) {
            for (condition in feature.conditions) {
                val action = when {
                    condition.symbolName != null -> GeneratedRuleAction.Symbol(condition.symbolName)
                    condition.functionNames.isNotEmpty() -> GeneratedRuleAction.Function(condition.functionNames)
                    else -> null
                }
                if (action != null) {
                    generated += GeneratedEsriRule(feature, condition, action, order++)
                }
            }
        }
        outputFile.parentFile.mkdirs()
        outputFile.writeText(renderRegistry(generated))
        return EsriRuleGenerationSummary(outputFile, generated.size, generated.count { it.action is GeneratedRuleAction.Symbol }, generated.count { it.action is GeneratedRuleAction.Function })
    }

    private fun renderRegistry(rules: List<GeneratedEsriRule>): String = buildString {
        appendLine("package io.github.s52.preslib.esri.generated")
        appendLine()
        appendLine("import io.github.s52.preslib.esri.rules.EsriPortrayalRule")
        appendLine("import io.github.s52.preslib.esri.rules.EsriRuleAction")
        appendLine("import io.github.s52.preslib.esri.rules.EsriRuleFilter")
        appendLine("import io.github.s52.preslib.esri.rules.EsriRuleOperator")
        appendLine()
        appendLine("/** Generated from ESRI CustomSymbolMap.xml by Phase ESRI-5. */")
        appendLine("object EsriGeneratedRuleRegistry {")
        appendLine("    const val RULE_COUNT: Int = ${rules.size}")
        appendLine("    val rules: List<EsriPortrayalRule> = listOf(")
        rules.forEachIndexed { index, rule ->
            append("        ")
            append(renderRule(rule))
            if (index != rules.lastIndex) append(',')
            appendLine()
        }
        appendLine("    )")
        appendLine("}")
    }

    private fun renderRule(rule: GeneratedEsriRule): String = buildString {
        append("EsriPortrayalRule(")
        append("objects = listOf(${rule.feature.objects.joinToString { kq(it) }}), ")
        append("primitive = ${rule.feature.primitive?.toString() ?: "null"}, ")
        append("action = ${renderAction(rule.action)}, ")
        append("filters = listOf(${rule.condition.rules.joinToString { renderFilter(it) }}), ")
        append("sourceOrder = ${rule.sourceOrder}")
        append(")")
    }

    private fun renderAction(action: GeneratedRuleAction): String = when (action) {
        is GeneratedRuleAction.Symbol -> "EsriRuleAction.Symbol(${kq(action.symbolName)})"
        is GeneratedRuleAction.Function -> "EsriRuleAction.Function(listOf(${action.functionNames.joinToString { kq(it) }}))"
    }

    private fun renderFilter(rule: EsriRule): String {
        val op = "EsriRuleOperator.${operatorName(rule.operator)}"
        val values = "listOf(${rule.values.joinToString { kq(it) }})"
        return when (rule.type.trim().uppercase()) {
            "A" -> "EsriRuleFilter.Attribute(field = ${kq(rule.field.orEmpty())}, operator = $op, values = $values)"
            "L" -> "EsriRuleFilter.ListAttribute(field = ${kq(rule.field.orEmpty())}, operator = $op, values = $values)"
            "F" -> "EsriRuleFilter.CoincidentFeature(objectName = ${kq(rule.objectName.orEmpty())}, operator = $op, display = ${rule.display.equals("true", ignoreCase = true)})"
            else -> "EsriRuleFilter.Attribute(field = ${kq(rule.field.orEmpty())}, operator = EsriRuleOperator.UNKNOWN, values = $values)"
        }
    }

    private fun operatorName(value: String): String = when (value.trim().lowercase()) {
        "equal" -> "EQUAL"
        "notequal" -> "NOT_EQUAL"
        "lt" -> "LT"
        "lte" -> "LTE"
        "gt" -> "GT"
        "gte" -> "GTE"
        "between" -> "BETWEEN"
        "like" -> "LIKE"
        "notlike" -> "NOT_LIKE"
        "containsany" -> "CONTAINS_ANY"
        "containsnone" -> "CONTAINS_NONE"
        "containsall" -> "CONTAINS_ALL"
        "hasmultiple" -> "HAS_MULTIPLE"
        "exists" -> "EXISTS"
        "fetch" -> "FETCH"
        else -> "UNKNOWN"
    }

    private fun kq(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").let { "\"$it\"" }
}

private data class GeneratedEsriRule(
    val feature: EsriFeatureRule,
    val condition: EsriCondition,
    val action: GeneratedRuleAction,
    val sourceOrder: Int
)

private sealed interface GeneratedRuleAction {
    data class Symbol(val symbolName: String) : GeneratedRuleAction
    data class Function(val functionNames: List<String>) : GeneratedRuleAction
}

data class EsriRuleGenerationSummary(
    val generatedFile: File,
    val ruleCount: Int,
    val symbolRuleCount: Int,
    val functionRuleCount: Int
)
