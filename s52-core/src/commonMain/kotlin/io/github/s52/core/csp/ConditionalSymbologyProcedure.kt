package io.github.s52.core.csp

import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

interface ConditionalSymbologyProcedure {
    val name: String

    fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction>
}
