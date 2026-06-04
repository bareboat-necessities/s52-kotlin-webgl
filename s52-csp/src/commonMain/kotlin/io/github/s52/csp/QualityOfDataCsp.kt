package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** Phase 6 starter for M_QUAL / quality-of-data portrayal. */
class QualityOfDataCsp : ConditionalSymbologyProcedure {
    override val name: String = "M_QUAL"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        val catzoc = feature.attributes.int(S57Attribute.CATZOC)
        val lowConfidence = catzoc == null || catzoc >= 4
        val pattern = if (lowConfidence) "MQUAL_LOW01" else "MQUAL_GOOD01"
        val color = if (lowConfidence) "QUASR" else "QUAPOS"

        val instructions = mutableListOf<S52Instruction>(
            S52Instruction.AreaPattern(pattern),
            S52Instruction.SimpleLine("DASH", 1.0, color)
        )

        if (settings.showText && catzoc != null) {
            val text = "CATZOC=$catzoc"
            instructions += S52Instruction.Text(text, listOf(text, color), InstructionTextKinds.tx())
        }

        return instructions
    }
}
