package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.instruction.TextSpec
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** LIGHTS: emits the light symbol, optional sector line, and optional description text. */
class LightsCsp : ConditionalSymbologyProcedure {
    override val name: String = "LIGHTS"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        require(feature.objectClass == S57ObjectClass.LIGHTS) {
            "LIGHTS CSP received ${feature.objectClass.acronym}"
        }
        val result = mutableListOf<S52Instruction>(S52Instruction.Symbol("LIGHTS11"))
        val hasSector = feature.attributes.double(S57Attribute.SECTR1) != null &&
            feature.attributes.double(S57Attribute.SECTR2) != null
        if (hasSector) result += S52Instruction.ComplexLine("LIGHTSECTOR01")
        if (settings.showLightDescriptions) {
            val text = CspHelpers.lightDescription(feature)
            result += S52Instruction.Text(
                textExpression = text,
                rawArgs = listOf(text, "LITYW"),
                kind = InstructionKind.TX,
                spec = TextSpec(text, listOf(text, "LITYW"), InstructionKind.TX)
            )
        }
        return result
    }
}
