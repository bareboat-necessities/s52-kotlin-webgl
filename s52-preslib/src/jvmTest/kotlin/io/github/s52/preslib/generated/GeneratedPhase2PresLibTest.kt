package io.github.s52.preslib.generated

import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.validation.PresLibValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneratedPhase2PresLibTest {
    @Test
    fun generatedPhase2PackIsInternallyValid() {
        val pack = GeneratedPhase2PresLib.pack()
        val report = PresLibValidator.validate(pack)

        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.lookupRecordCount >= 8)
        assertTrue(report.referencedSymbols.contains("BOYLAT01"))
        assertTrue(report.referencedLineStyles.contains("COALNE01"))
        assertTrue(report.referencedPatterns.contains("APACHR01"))
        assertTrue(report.referencedCsps.containsAll(setOf("DEPARE", "DEPCNT", "SOUNDG", "WRECKS", "OBSTRN", "LIGHTS", "TOPMAR")))
    }

    @Test
    fun generatedPhase2PackContainsAllPalettes() {
        val pack = GeneratedPhase2PresLib.pack()

        S52Palette.entries.forEach { palette ->
            assertNotNull(pack.colors.color(palette, "LANDA"))
            assertNotNull(pack.colors.color(palette, "DEPVS"))
        }
    }

    @Test
    fun generatedLookupRowsParseIntoTypedInstructions() {
        val pack = GeneratedPhase2PresLib.pack()
        val records = pack.lookupTable.records()

        assertTrue(records.any { it.objectClass == S57ObjectClass.DEPARE && it.instructions.single() is S52Instruction.Conditional })
        assertTrue(records.any { it.objectClass == S57ObjectClass.LIGHTS && it.instructions.any { ins -> ins is S52Instruction.Text } })
    }
}
