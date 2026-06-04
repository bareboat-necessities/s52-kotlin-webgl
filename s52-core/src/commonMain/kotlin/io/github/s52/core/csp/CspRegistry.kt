package io.github.s52.core.csp

import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

interface CspRegistry {
    fun has(name: String): Boolean

    fun evaluate(
        name: String,
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction>
}

class MapCspRegistry(
    procedures: Iterable<ConditionalSymbologyProcedure>
) : CspRegistry {
    private val byName = procedures.associateBy { it.name.uppercase() }

    override fun has(name: String): Boolean = name.uppercase() in byName

    override fun evaluate(
        name: String,
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        val procedure = byName[name.uppercase()]
            ?: error("Missing S-52 conditional symbology procedure: $name")
        return procedure.evaluate(feature, settings, context)
    }
}

object EmptyCspRegistry : CspRegistry {
    override fun has(name: String): Boolean = false

    override fun evaluate(
        name: String,
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> = error("Missing S-52 conditional symbology procedure: $name")
}
