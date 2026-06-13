package io.github.s52.tests.validation

import io.github.s52.core.draw.DrawCommandValidator
import io.github.s52.core.draw.S52DrawCommandTranscript
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack
import io.github.s52.tests.golden.GoldenTranscriptComparison

/**
 * Command-level S-64 / Chart-1-style validation runner.
 *
 * This intentionally validates the public portrayal boundary before pixel tests:
 * normalized ENC features -> S52PortrayalEngine -> S52DrawCommand transcript.
 */
class S64CommandValidationRunner(
    private val presLib: PresLibPack = PresLibPack.synthetic()
) {
    private val engine = S52PortrayalEngine(
        lookupTable = presLib.lookupTable,
        cspRegistry = DefaultCspRegistry.complete()
    )

    fun run(fixture: CommandValidationFixture): CommandValidationResult {
        val commands = engine.portray(fixture.features, fixture.settings, fixture.context)
        val commandValidation = DrawCommandValidator.validate(commands)
        val actualTranscript = S52DrawCommandTranscript.serialize(commands)
        val comparison = GoldenTranscriptComparison(fixture.expectedTranscript, actualTranscript)
        return CommandValidationResult(
            fixture = fixture,
            actualTranscript = actualTranscript,
            commandValidationMarkdown = commandValidation.toMarkdown(),
            commandsValid = !commandValidation.hasErrors,
            comparison = comparison
        )
    }

    fun report(fixtures: List<CommandValidationFixture>): S64ValidationReport =
        S64ValidationReport(fixtures.map(::run))
}

data class CommandValidationResult(
    val fixture: CommandValidationFixture,
    val actualTranscript: String,
    val commandValidationMarkdown: String,
    val commandsValid: Boolean,
    val comparison: GoldenTranscriptComparison
) {
    val passed: Boolean get() = commandsValid && comparison.matches

    fun failureMessage(): String = buildString {
        if (passed) return@buildString
        appendLine("Validation fixture '${fixture.id}' failed")
        if (!commandsValid) {
            appendLine("Command validation failed:")
            appendLine(commandValidationMarkdown)
        }
        if (!comparison.matches) {
            appendLine(comparison.failureMessage(fixture.id))
        }
    }
}

data class S64ValidationReport(
    val results: List<CommandValidationResult>
) {
    val total: Int get() = results.size
    val passed: Int get() = results.count { it.passed }
    val failed: Int get() = results.count { !it.passed }
    val isSuccess: Boolean get() = failed == 0

    fun toMarkdown(): String = buildString {
        appendLine("# S-64 / Chart-1 Command Validation Report")
        appendLine()
        appendLine("Total fixtures: $total")
        appendLine("Passed: $passed")
        appendLine("Failed: $failed")
        appendLine()
        results.forEach { result ->
            append("- ")
            append(if (result.passed) "PASS" else "FAIL")
            append(": ")
            append(result.fixture.id)
            append(" (")
            append(result.fixture.source.name)
            appendLine(")")
        }
    }
}
