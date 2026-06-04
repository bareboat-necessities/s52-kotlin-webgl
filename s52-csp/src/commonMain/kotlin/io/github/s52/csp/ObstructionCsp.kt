package io.github.s52.csp

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** Phase 5 OBSTRN: separates dangerous and non-dangerous obstruction portrayal. */
class ObstructionCsp : ConditionalSymbologyProcedure {
    override val name: String = "OBSTRN"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        require(feature.objectClass == S57ObjectClass.OBSTRN) {
            "OBSTRN CSP received ${feature.objectClass.acronym}"
        }
        val category = feature.attributes.int(S57Attribute.CATOBS)
        val depth = CspHelpers.depth(feature)
        val dangerous = category == 6 || CspHelpers.isUnsafeDepth(depth, settings.safetyDepthMeters)

        return when (feature.primitive) {
            PrimitiveType.Area -> listOf(
                S52Instruction.AreaPattern(if (dangerous) "DANGER01" else "OBSTRN_AREA01"),
                S52Instruction.SimpleLine("DASH", if (dangerous) 2.0 else 1.0, if (dangerous) "DNGHL" else "CHGRD")
            )
            else -> listOf(S52Instruction.Symbol(if (dangerous) "OBSTRN_DANGER01" else "OBSTRN01"))
        }
    }
}
