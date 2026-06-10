package io.github.s52.preslib.esri.profile

import io.github.s52.preslib.esri.alias.EsriAliasCoverage
import io.github.s52.preslib.esri.csp.EsriCspFeature
import io.github.s52.preslib.esri.csp.EsriCspRegistry
import io.github.s52.preslib.esri.csp.EsriInstruction
import io.github.s52.preslib.esri.csp.EsriPortrayalContext
import io.github.s52.preslib.esri.generated.EsriGeneratedLineRegistry
import io.github.s52.preslib.esri.generated.EsriGeneratedPatternRegistry
import io.github.s52.preslib.esri.generated.EsriGeneratedPresLib
import io.github.s52.preslib.esri.generated.EsriGeneratedRuleRegistry
import io.github.s52.preslib.esri.generated.EsriGeneratedSymbolRegistry
import io.github.s52.preslib.esri.rules.EsriPortrayalRule
import io.github.s52.preslib.esri.rules.EsriRuleAction
import io.github.s52.preslib.esri.rules.EsriRuleFeature
import io.github.s52.preslib.esri.vector.EsriVectorAreaPattern
import io.github.s52.preslib.esri.vector.EsriVectorLineStyle
import io.github.s52.preslib.esri.vector.EsriVectorSymbol

/**
 * Stable entry point for the ESRI/INT1 symbology profile.
 *
 * The profile intentionally remains independent from a concrete chartplotter or
 * S-57 parser. It accepts the small phase ESRI rule/CSP feature models and
 * exposes generated vector assets that WebGL renderers can consume directly.
 */
object EsriInt1Profile {
    val metadata: EsriProfileMetadata = EsriGeneratedPresLib.metadata

    val symbols: Map<String, EsriVectorSymbol> get() = EsriGeneratedSymbolRegistry.symbols
    val lines: Map<String, EsriVectorLineStyle> get() = EsriGeneratedLineRegistry.lines
    val patterns: Map<String, EsriVectorAreaPattern> get() = EsriGeneratedPatternRegistry.patterns
    val directRules: List<EsriPortrayalRule> get() = EsriGeneratedRuleRegistry.rules

    fun symbol(name: String): EsriVectorSymbol? = symbols[name] ?: symbols[name.removeSuffix(".svg")]
    fun line(name: String): EsriVectorLineStyle? = lines[name] ?: lines[name.removeSuffix(".svg")]
    fun pattern(name: String): EsriVectorAreaPattern? = patterns[name] ?: patterns[name.removeSuffix(".svg")]

    fun directRuleActions(feature: EsriRuleFeature): List<EsriRuleAction> = directRules
        .asSequence()
        .filter { it.matches(feature) }
        .sortedBy { it.sourceOrder }
        .map { it.action }
        .toList()

    fun cspInstructions(
        functionName: String,
        feature: EsriCspFeature,
        context: EsriPortrayalContext = EsriPortrayalContext()
    ): List<EsriInstruction> = EsriCspRegistry.apply(functionName, feature, context)

    fun audit(): EsriProfileAudit = EsriProfileAudit(
        metadata = metadata,
        symbolCount = symbols.size,
        renderableSymbolCount = symbols.values.count { it.isRenderable },
        lineCount = lines.size,
        renderableLineCount = lines.values.count { it.isRenderable },
        patternCount = patterns.size,
        renderablePatternCount = patterns.values.count { it.isRenderable },
        directRuleCount = directRules.size,
        cspCount = EsriCspRegistry.names.size,
        aliasCoverage = EsriGeneratedPresLib.aliasCoverage
    )
}

data class EsriProfileMetadata(
    val name: String,
    val edition: String,
    val sourceDescription: String,
    val generatedBy: String,
    val sourceRevision: String? = null
)

data class EsriProfileAudit(
    val metadata: EsriProfileMetadata,
    val symbolCount: Int,
    val renderableSymbolCount: Int,
    val lineCount: Int,
    val renderableLineCount: Int,
    val patternCount: Int,
    val renderablePatternCount: Int,
    val directRuleCount: Int,
    val cspCount: Int,
    val aliasCoverage: EsriAliasCoverage
) {
    val hasRenderableAssets: Boolean get() = renderableSymbolCount + renderableLineCount + renderablePatternCount > 0
}
