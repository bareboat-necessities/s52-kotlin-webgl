package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** DEPCNT: promotes the selected safety contour to a stronger style. */
class DepthContourCsp : ConditionalSymbologyProcedure {
    override val name: String = "DEPCNT"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        require(feature.objectClass == S57ObjectClass.DEPCNT) {
            "DEPCNT CSP received ${feature.objectClass.acronym}"
        }
        val contour = feature.attributes.double(S57Attribute.VALDCO)
        val safety = contour != null && CspHelpers.isNear(contour, settings.safetyContourMeters)
        return listOf(
            if (safety) S52Instruction.SimpleLine("SOLD", 2.0, "DEPSC")
            else S52Instruction.SimpleLine("SOLD", 1.0, "CHGRD")
        )
    }
}
