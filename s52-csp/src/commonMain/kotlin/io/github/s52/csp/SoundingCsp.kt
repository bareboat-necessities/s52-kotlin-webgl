package io.github.s52.csp

import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.instruction.TextSpec
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** Phase 5 SOUNDG: hides soundings when requested and colors unsafe soundings. */
class SoundingCsp : ConditionalSymbologyProcedure {
    override val name: String = "SOUNDG"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        require(feature.objectClass == S57ObjectClass.SOUNDG) {
            "SOUNDG CSP received ${feature.objectClass.acronym}"
        }
        if (!settings.showSoundings) return emptyList()

        val depth = CspHelpers.depth(feature) ?: return emptyList()
        val text = CspHelpers.formatDepth(depth)
        val color = if (depth <= settings.safetyDepthMeters) "SNDG2" else "SNDG1"
        val args = listOf(text, color)
        return listOf(
            S52Instruction.Text(
                textExpression = text,
                rawArgs = args,
                kind = InstructionKind.TX,
                spec = TextSpec(text, args, InstructionKind.TX)
            )
        )
    }
}
