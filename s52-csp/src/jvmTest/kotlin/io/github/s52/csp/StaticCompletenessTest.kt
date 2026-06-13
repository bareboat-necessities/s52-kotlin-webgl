package io.github.s52.csp

import io.github.s52.preslib.generated.GeneratedPresLib
import io.github.s52.preslib.validation.StaticCompletenessValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticCompletenessTest {
    @Test
    fun generatedPresentationLibraryHasZeroStaticCompletenessDiagnosticsWithRegistry() {
        val report = StaticCompletenessValidator.validatePack(
            pack = GeneratedPresLib.pack(),
            implementedCsps = CspId.completeNames()
        )

        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.referencedCsps.isNotEmpty())
        assertTrue(report.missingCsps.isEmpty(), report.toMarkdown())
    }

    @Test
    fun sourcePresentationLibraryHasZeroStaticCompletenessDiagnosticsWithRegistry() {
        val report = StaticCompletenessValidator.validateSource(
            source = GeneratedPresLib.sourcePack(),
            implementedCsps = CspId.completeNames()
        )

        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.referencedSymbols.contains("BOYLAT01"))
        assertTrue(report.referencedLineStyles.contains("COALNE01"))
        assertTrue(report.referencedColors.contains("LANDA"))
    }
}
