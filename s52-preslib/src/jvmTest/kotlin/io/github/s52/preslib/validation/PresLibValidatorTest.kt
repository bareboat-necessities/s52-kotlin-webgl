package io.github.s52.preslib.validation

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.source.PresLibMetadata
import io.github.s52.preslib.source.PresLibPackBuilder
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceColor
import io.github.s52.preslib.source.SourceColorTable
import io.github.s52.preslib.source.SourceLineStyle
import io.github.s52.preslib.source.SourceLookupRecord
import io.github.s52.preslib.source.SourcePattern
import io.github.s52.preslib.source.SourceSymbol
import kotlin.test.Test
import kotlin.test.assertTrue

class PresLibValidatorTest {
    @Test
    fun reportsMissingReferencedAssets() {
        val source = PresLibSourcePack(
            metadata = PresLibMetadata("broken", "test", "test", "test"),
            colorTables = S52Palette.entries.map { palette ->
                SourceColorTable(palette, listOf(SourceColor("CHBLK", 0, 0, 0)))
            },
            symbols = emptyList(),
            lineStyles = emptyList(),
            patterns = emptyList(),
            lookupRecords = listOf(
                SourceLookupRecord(
                    objectClass = S57ObjectClass.BOYLAT,
                    primitive = PrimitiveType.Point,
                    instruction = "SY(MISSING01);LS(MISSING,1,MISSINGCOLOR);AP(MISSINGPAT)",
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 1,
                    displayPriority = 1
                )
            )
        )

        val report = PresLibValidator.validate(PresLibPackBuilder.build(source))

        assertTrue(report.diagnostics.any { it is PresLibValidationDiagnostic.MissingSymbol })
        assertTrue(report.diagnostics.any { it is PresLibValidationDiagnostic.MissingLineStyle })
        assertTrue(report.diagnostics.any { it is PresLibValidationDiagnostic.MissingPattern })
        assertTrue(report.diagnostics.any { it is PresLibValidationDiagnostic.MissingColor })
    }

    @Test
    fun reportsDuplicateSourceNames() {
        val source = PresLibSourcePack(
            metadata = PresLibMetadata("dup", "test", "test", "test"),
            colorTables = listOf(
                SourceColorTable(S52Palette.DayBright, listOf(SourceColor("LANDA", 1, 2, 3), SourceColor("landa", 4, 5, 6)))
            ),
            symbols = listOf(SourceSymbol("SYM01"), SourceSymbol("sym01")),
            lineStyles = listOf(SourceLineStyle("SOLD"), SourceLineStyle("sold")),
            patterns = listOf(SourcePattern("PAT01"), SourcePattern("pat01")),
            lookupRecords = emptyList()
        )

        val diagnostics = PresLibValidator.validateSource(source)

        assertTrue(diagnostics.count { it is PresLibValidationDiagnostic.DuplicateSourceName } >= 4)
    }
}
