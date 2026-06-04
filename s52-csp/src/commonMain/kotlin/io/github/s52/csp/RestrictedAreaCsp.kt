package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/**
 * Starter Phase 6 restricted/caution-area CSP.
 *
 * This keeps the behavior renderer-independent and deliberately conservative:
 * restricted/caution areas get a synthetic caution pattern, dashed boundary, and
 * optional text derived only from typed S-57 attributes.
 */
class RestrictedAreaCsp(
    override val name: String = "RESARE",
    private val patternName: String = "RESTRN01"
) : ConditionalSymbologyProcedure {
    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        val instructions = mutableListOf<S52Instruction>()
        instructions += S52Instruction.AreaPattern(patternName)
        instructions += S52Instruction.SimpleLine(style = "DASH", width = 1.0, colorToken = "CHMGD")

        val text = CspHelpers.objectName(feature)
            ?: feature.attributes.ints(S57Attribute.RESTRN).takeIf { it.isNotEmpty() }?.joinToString(prefix = "RESTRN=")
            ?: feature.attributes.ints(S57Attribute.CATREA).takeIf { it.isNotEmpty() }?.joinToString(prefix = "CATREA=")

        if (settings.showText && text != null) {
            instructions += S52Instruction.Text(text, listOf(text, "CHMGD"), InstructionTextKinds.tx())
        }

        return instructions
    }
}
