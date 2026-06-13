package io.github.s52.preslib.generated

import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.validation.PresLibValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneratedPresLibTest {
    @Test
    fun generatedPackIsInternallyValid() {
        val pack = GeneratedPresLib.pack()
        val report = PresLibValidator.validate(pack)

        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.lookupRecordCount >= 8)
        assertTrue(report.referencedSymbols.contains("BOYLAT01"))
        assertTrue(report.referencedLineStyles.contains("COALNE01"))
        assertTrue(report.referencedCsps.containsAll(setOf("DEPARE", "DEPCNT", "SOUNDG", "WRECKS", "OBSTRN", "LIGHTS", "TOPMAR")))
        assertTrue(report.referencedCsps.containsAll(setOf("ACHARE", "RESARE", "PRCARE", "TESARE", "FAIRWY", "DRGARE", "SBDARE", "M_QUAL", "DATCVR")))
    }

    @Test
    fun generatedPackContainsAllPalettes() {
        val pack = GeneratedPresLib.pack()

        S52Palette.entries.forEach { palette ->
            assertNotNull(pack.colors.color(palette, "LANDA"))
            assertNotNull(pack.colors.color(palette, "DEPVS"))
        }
    }

    @Test
    fun generatedLookupRowsParseIntoTypedInstructions() {
        val pack = GeneratedPresLib.pack()
        val records = pack.lookupTable.records()

        assertTrue(records.any { it.objectClass == S57ObjectClass.DEPARE && it.instructions.single() is S52Instruction.Conditional })
        assertTrue(records.any { it.objectClass == S57ObjectClass.LIGHTS && it.instructions.single() is S52Instruction.Conditional })
        assertTrue(records.any { it.objectClass == S57ObjectClass.M_QUAL && it.instructions.single() is S52Instruction.Conditional })
    }
}
