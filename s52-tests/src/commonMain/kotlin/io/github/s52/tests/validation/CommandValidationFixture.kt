package io.github.s52.tests.validation

import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/**
 * External command-level validation fixture.
 *
 * The fixture shape is intentionally close to S-64 / Chart-1 workflows but
 * independent of any proprietary test data: the caller supplies normalized ENC
 * features plus the command transcript expected from the portrayal engine.
 */
data class CommandValidationFixture(
    val id: String,
    val source: ValidationFixtureSource,
    val description: String,
    val settings: MarinerSettings,
    val context: PortrayalContext,
    val features: List<EncFeature>,
    val expectedTranscript: String
) {
    val expectedLineCount: Int get() = expectedTranscript.trimEnd().lineSequence().count()
}

enum class ValidationFixtureSource {
    SyntheticChart1,
    SyntheticS64,
    ExternalS64,
    Custom;

    companion object {
        fun parse(value: String): ValidationFixtureSource = entries.firstOrNull {
            it.name.equals(value.trim(), ignoreCase = true)
        } ?: error("Unknown validation fixture source '$value'")
    }
}
