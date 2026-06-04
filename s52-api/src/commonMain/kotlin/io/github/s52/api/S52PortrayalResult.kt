package io.github.s52.api

import io.github.s52.core.draw.DrawCommandValidationReport
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.draw.S52DrawCommandTranscript
import io.github.s52.preslib.validation.StaticCompletenessReport

/** Result returned by high-level portrayal facades and [S52Runtime.portrayValidated]. */
data class S52PortrayalResult(
    val commands: List<S52DrawCommand>,
    val validation: DrawCommandValidationReport,
    val staticCompleteness: StaticCompletenessReport = emptyStaticCompletenessReport(),
    val transcript: String = S52DrawCommandTranscript.serialize(commands)
) {
    val commandValidation: DrawCommandValidationReport get() = validation
    val isValid: Boolean get() = !validation.hasErrors
    val hasErrors: Boolean get() = validation.hasErrors || staticCompleteness.hasErrors

    fun diagnosticsMarkdown(): String = buildString {
        appendLine(staticCompleteness.toMarkdown())
        appendLine()
        appendLine(validation.toMarkdown())
    }

    companion object {
        private fun emptyStaticCompletenessReport(): StaticCompletenessReport = StaticCompletenessReport(
            diagnostics = emptyList(),
            lookupRecordCount = 0,
            instructionCount = 0,
            symbolCount = 0,
            lineStyleCount = 0,
            patternCount = 0,
            paletteCount = 0,
            referencedSymbols = emptySet(),
            referencedLineStyles = emptySet(),
            referencedPatterns = emptySet(),
            referencedColors = emptySet(),
            referencedCsps = emptySet(),
            implementedCsps = emptySet()
        )
    }
}
