package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/**
 * Starter implementation of the critical DEPARE conditional symbology.
 *
 * This models the mariner shallow/safety/deep contour settings
 * and emits renderer-independent S-52 instructions. Official Presentation
 * Library symbol names remain external to the generated PresLib pack.
 */
class DepthAreaCsp : ConditionalSymbologyProcedure {
    override val name: String = "DEPARE"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        require(feature.objectClass == S57ObjectClass.DEPARE) {
            "DEPARE CSP received ${feature.objectClass.acronym}"
        }

        val drval1 = feature.attributes.double(S57Attribute.DRVAL1)
        val drval2 = feature.attributes.double(S57Attribute.DRVAL2)

        val color = when {
            drval2 != null && drval2 <= settings.safetyContourMeters -> "DEPVS"
            drval1 != null && drval1 < settings.shallowContourMeters -> "DEPIT"
            drval1 != null && drval1 >= settings.deepContourMeters -> "DEPDW"
            else -> "DEPMS"
        }

        return listOf(S52Instruction.AreaColor(color))
    }
}
