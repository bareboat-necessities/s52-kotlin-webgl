package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** Starter for seabed/nature-of-surface area portrayal. */
class SeabedAreaCsp : ConditionalSymbologyProcedure {
    override val name: String = "SBDARE"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        val instructions = mutableListOf<S52Instruction>(S52Instruction.AreaPattern("SBDARE01"))
        val nature = feature.attributes.ints(S57Attribute.NATCON).takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "NATCON=")
        if (settings.showText && nature != null) {
            instructions += S52Instruction.Text(nature, listOf(nature, "CHGRD"), InstructionTextKinds.tx())
        }
        return instructions
    }
}
