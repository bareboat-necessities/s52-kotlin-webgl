package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** Phase 5 TOPMAR: selects a synthetic topmark symbol from TOPSHP. */
class TopmarkCsp : ConditionalSymbologyProcedure {
    override val name: String = "TOPMAR"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        require(feature.objectClass == S57ObjectClass.TOPMAR) {
            "TOPMAR CSP received ${feature.objectClass.acronym}"
        }
        val symbol = when (feature.attributes.int(S57Attribute.TOPSHP)) {
            1 -> "TOPMAR_CONE_UP01"
            2 -> "TOPMAR_CONE_DOWN01"
            3 -> "TOPMAR_SPHERE01"
            4 -> "TOPMAR_TWO_SPHERES01"
            5 -> "TOPMAR_CYLINDER01"
            6 -> "TOPMAR_X01"
            7 -> "TOPMAR_CROSS01"
            else -> "TOPMAR_UNKNOWN01"
        }
        return listOf(S52Instruction.Symbol(symbol))
    }
}
