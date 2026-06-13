package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** Starter for data-coverage boundary/no-data behavior. */
class DataCoverageCsp : ConditionalSymbologyProcedure {
    override val name: String = "DATCVR"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        val categoryOfCoverage = feature.attributes.int(S57Attribute.CATCOV)
        return if (categoryOfCoverage == 2) {
            listOf(
                S52Instruction.AreaColor("NODTA"),
                S52Instruction.AreaPattern("NODATA01"),
                S52Instruction.ComplexLine("DATCVR01")
            )
        } else {
            listOf(S52Instruction.ComplexLine("DATCVR01"))
        }
    }
}
