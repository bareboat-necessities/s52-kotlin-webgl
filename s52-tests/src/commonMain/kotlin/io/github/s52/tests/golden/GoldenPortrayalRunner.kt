package io.github.s52.tests.golden

import io.github.s52.core.draw.DrawCommandValidator
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.draw.S52DrawCommandTranscript
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack

/** Runs golden cases through the public portrayal engine boundary. */
class GoldenPortrayalRunner(
    private val presLib: PresLibPack = PresLibPack.phase2Synthetic()
) {
    private val engine = S52PortrayalEngine(
        lookupTable = presLib.lookupTable,
        cspRegistry = DefaultCspRegistry.phase6Complete()
    )

    fun portray(case: GoldenPortrayalCase): List<S52DrawCommand> =
        engine.portray(case.features, case.settings, case.context)

    fun transcript(case: GoldenPortrayalCase): String {
        val commands = portray(case)
        val validation = DrawCommandValidator.validate(commands)
        require(!validation.hasErrors) { validation.toMarkdown() }
        return S52DrawCommandTranscript.serialize(commands)
    }
}
