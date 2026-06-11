package io.github.s52.preslib.validation

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.generated.GeneratedPhase2PresLib
import io.github.s52.preslib.source.PresLibMetadata
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceAttributeFilter
import io.github.s52.preslib.source.SourceColor
import io.github.s52.preslib.source.SourceColorTable
import io.github.s52.preslib.source.SourceLookupRecord
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticCompletenessValidatorTest {
    @Test
    fun generatedSyntheticSourcePackIsStaticallyCompleteExceptCspResolutionWhenNoRegistryIsProvided() {
        val report = StaticCompletenessValidator.validateSource(GeneratedPhase2PresLib.sourcePack())

        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.lookupRecordCount >= 20)
        assertTrue(report.instructionCount >= report.lookupRecordCount)
        assertTrue("DEPARE" in report.referencedCsps)
        assertTrue("DATCVR" in report.referencedCsps)
        assertTrue("LANDA" in report.referencedColors)
    }

    @Test
    fun reportsStaticCompletenessFailuresWithActionableDiagnostics() {
        val source = PresLibSourcePack(
            metadata = PresLibMetadata("broken", "test", "test", "test"),
            colorTables = listOf(SourceColorTable(S52Palette.DayBright, listOf(SourceColor("CHBLK", 0, 0, 0)))),
            symbols = emptyList(),
            lineStyles = emptyList(),
            patterns = emptyList(),
            lookupRecords = listOf(
                SourceLookupRecord(
                    objectClass = S57ObjectClass.BOYLAT,
                    primitive = PrimitiveType.Point,
                    instruction = "SY(MISSING01);LC(MISSINGLINE);AP(MISSINGPAT);AC(MISSINGCOLOR);CS(MISSINGCSP)",
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 1,
                    displayPriority = 1,
                    attributeFilter = SourceAttributeFilter.TextEquals(S57Attribute.DRVAL1, "wrong-kind")
                ),
                SourceLookupRecord(
                    objectClass = S57ObjectClass.DEPARE,
                    primitive = PrimitiveType.Point,
                    instruction = "SY(BROKEN",
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 1,
                    displayPriority = 1
                )
            )
        )

        val report = StaticCompletenessValidator.validateSource(
            source,
            implementedCsps = setOf("DEPARE"),
            requiredPalettes = S52Palette.entries.toSet()
        )

        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.MissingSymbol }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.MissingLineStyle }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.MissingPattern }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.MissingColor }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.MissingPalette }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.MissingCsp }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.InvalidPrimitive }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.InstructionParseFailure }, report.toMarkdown())
        assertTrue(report.diagnostics.any { it is StaticCompletenessDiagnostic.IncompatibleAttributeFilter }, report.toMarkdown())
    }
}
