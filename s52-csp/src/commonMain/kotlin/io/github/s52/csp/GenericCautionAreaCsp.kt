package io.github.s52.csp

import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/**
 * Conservative typed fallback for area CSPs that are mostly category/status
 * dependent in the official Presentation Library. It is not a no-op: it emits a
 * visible pattern/boundary and optional object-name text, while leaving exact
 * official symbol selection for the later external Presentation Library import.
 */
class GenericCautionAreaCsp(
    override val name: String,
    private val patternName: String = "CAUTION01",
    private val colorToken: String = "CHMGD"
) : ConditionalSymbologyProcedure {
    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        val instructions = mutableListOf<S52Instruction>(
            S52Instruction.AreaPattern(patternName),
            S52Instruction.SimpleLine("DASH", 1.0, colorToken)
        )
        val nameText = CspHelpers.objectName(feature)
        if (settings.showText && nameText != null) {
            instructions += S52Instruction.Text(nameText, listOf(nameText, colorToken), InstructionTextKinds.tx())
        }
        return instructions
    }
}
