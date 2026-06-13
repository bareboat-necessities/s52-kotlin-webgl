package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** Starter for dredged-area depth portrayal. */
class DredgedAreaCsp : ConditionalSymbologyProcedure {
    override val name: String = "DRGARE"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        val drval1 = feature.attributes.double(S57Attribute.DRVAL1)
        val drval2 = feature.attributes.double(S57Attribute.DRVAL2)
        val color = when {
            drval2 != null && drval2 <= settings.safetyContourMeters -> "DEPVS"
            drval1 != null && drval1 < settings.shallowContourMeters -> "DEPIT"
            drval2 != null && drval2 >= settings.deepContourMeters -> "DEPDW"
            else -> "DEPMS"
        }
        val instructions = mutableListOf<S52Instruction>(
            S52Instruction.AreaColor(color),
            S52Instruction.AreaPattern("DRGARE01"),
            S52Instruction.SimpleLine("DASH", 1.0, "DRGHL")
        )
        if (settings.showText && drval2 != null) {
            val text = "dredged ${CspHelpers.formatDepth(drval2)}m"
            instructions += S52Instruction.Text(text, listOf(text, "DRGHL"), InstructionTextKinds.tx())
        }
        return instructions
    }
}
