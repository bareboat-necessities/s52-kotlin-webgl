package io.github.s52.preslib.opencpn.generated

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClassKey
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class OpenCpnProjectLogCoverageTest {
    @Test
    fun openCpnPackHasLookupRowsForProjectLogObjectClasses() {
        val table = OpenCpnGeneratedPresLib.pack().lookupTable
        val expected = listOf(
            "ACHARE" to PrimitiveType.Line,
            "ACHBRT" to PrimitiveType.Area,
            "BUAARE" to PrimitiveType.Area,
            "CBLARE" to PrimitiveType.Area,
            "CTNARE" to PrimitiveType.Area,
            "DRYDOC" to PrimitiveType.Area,
            "HRBFAC" to PrimitiveType.Point,
            "HRBFAC" to PrimitiveType.Area,
            "LNDRGN" to PrimitiveType.Area,
            "PIPARE" to PrimitiveType.Area,
            "SLOTOP" to PrimitiveType.Line,
            "UNSARE" to PrimitiveType.Area,
            "OBJL_0" to PrimitiveType.Line
        )

        expected.forEach { (acronym, primitive) ->
            assertTrue(
                table.candidates(S57ObjectClassKey.of(acronym), primitive).isNotEmpty(),
                "OpenCPN lookup table should have candidates for $acronym/$primitive"
            )
        }
    }
    @Test
    fun openCpnCompatibilityRowsCoverRemainingClientLogPrimitiveGaps() {
        val table = OpenCpnGeneratedPresLib.pack().lookupTable

        val achareLine = table.candidates(S57ObjectClassKey.of("ACHARE"), PrimitiveType.Line)
        assertTrue(achareLine.isNotEmpty(), "ACHARE/Line should have a compatibility line-style lookup")
        assertTrue(
            achareLine.any { record -> record.instructions.isNotEmpty() },
            "ACHARE/Line should render with a real line instruction"
        )

        val objl0Line = table.candidates(S57ObjectClassKey.of("OBJL_0"), PrimitiveType.Line)
        assertEquals(1, objl0Line.size, "OBJL_0/Line should be modeled as exactly one no-op row")
        assertTrue(objl0Line.single().instructions.isEmpty(), "OBJL_0/Line should stay non-rendering")
    }

}
