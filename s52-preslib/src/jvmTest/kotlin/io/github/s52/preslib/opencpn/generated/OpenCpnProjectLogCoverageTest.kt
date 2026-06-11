package io.github.s52.preslib.opencpn.generated

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClassKey
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenCpnProjectLogCoverageTest {
    @Test
    fun openCpnPackHasLookupRowsForProjectLogObjectClasses() {
        val table = OpenCpnGeneratedPresLib.pack().lookupTable
        val expected = listOf(
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
            "UNSARE" to PrimitiveType.Area
        )

        expected.forEach { (acronym, primitive) ->
            assertTrue(
                table.candidates(S57ObjectClassKey.of(acronym), primitive).isNotEmpty(),
                "OpenCPN lookup table should have candidates for $acronym/$primitive"
            )
        }
    }
}
