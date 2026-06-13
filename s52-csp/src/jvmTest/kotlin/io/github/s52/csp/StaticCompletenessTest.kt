package io.github.s52.csp

import io.github.s52.preslib.generated.GeneratedPhase2PresLib
import io.github.s52.preslib.validation.StaticCompletenessValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticCompletenessTest {
    @Test
    fun generatedPresentationLibraryHasZeroStaticCompletenessDiagnosticsWithPhase6Registry() {
        val report = StaticCompletenessValidator.validatePack(
            pack = GeneratedPhase2PresLib.pack(),
            implementedCsps = CspId.completePhase6Names()
        )

        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.referencedCsps.isNotEmpty())
        assertTrue(report.missingCsps.isEmpty(), report.toMarkdown())
    }

    @Test
    fun sourcePresentationLibraryHasZeroStaticCompletenessDiagnosticsWithPhase6Registry() {
        val report = StaticCompletenessValidator.validateSource(
            source = GeneratedPhase2PresLib.sourcePack(),
            implementedCsps = CspId.completePhase6Names()
        )

        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.referencedSymbols.contains("BOYLAT01"))
        assertTrue(report.referencedLineStyles.contains("COALNE01"))
        assertTrue(report.referencedColors.contains("LANDA"))
    }
}
