package io.github.s52.tests.golden

import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/**
 * A command-level golden portrayal fixture.
 *
 * The expected output is stored as a transcript resource in jvmTest/resources.
 * Keeping expected files outside the code makes review diffs readable and keeps
 * Focused on stable renderer-independent command output, not pixels.
 */
data class GoldenPortrayalCase(
    val id: String,
    val description: String,
    val features: List<EncFeature>,
    val settings: MarinerSettings = MarinerSettings(),
    val context: PortrayalContext = PortrayalContext(
        compilationScale = 50_000.0,
        displayScale = 50_000.0
    ),
    val expectedResource: String = "/golden/$id.golden"
)
