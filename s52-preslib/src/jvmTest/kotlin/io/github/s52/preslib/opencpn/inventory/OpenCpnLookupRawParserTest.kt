package io.github.s52.preslib.opencpn.inventory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenCpnLookupRawParserTest {
    @Test
    fun parsesAllOpenCpnLookupRowsWithoutDroppingPrivateObjectNames() {
        val inventory = OpenCpnPayloadInventoryReader.read(openCpnDir())
        val chartSymbols = requireNotNull(inventory.chartSymbols)

        assertEquals(3057, chartSymbols.lookups.size)
        assertTrue(chartSymbols.lookups.any { it.objectClassKey.acronym == "\$AREAS" })
        assertTrue(chartSymbols.lookups.any { it.objectClassKey.acronym == "\$LINES" })
        assertTrue(chartSymbols.lookups.any { it.objectClassKey.acronym == "\$CSYMB" })
        assertTrue(chartSymbols.lookups.any { it.objectClassKey.acronym == "\$TEXTS" })

        val achare = chartSymbols.lookups.first { it.objectClassKey.acronym == "ACHARE" && "CATACH8" in it.attribCodes }
        assertEquals(OpenCpnAttributeFilter.EqualsInt::class, achare.attributeFilter::class)
        assertEquals(setOf("ACHARE02"), achare.instructionRefs.symbols)
        assertTrue("RESTRN01" in achare.instructionRefs.cspNames)
    }

    @Test
    fun parsesCommonAttribCodeForms() {
        assertEquals("CATACH == 8", OpenCpnAttribCodeParser.parse("CATACH8").description)
        assertEquals("COLOUR in [1, 3]", OpenCpnAttribCodeParser.parse("COLOUR3,1").description)
        assertEquals("DRVAL1 exists", OpenCpnAttribCodeParser.parse("DRVAL1?").description)
        assertEquals("CONDTN exists", OpenCpnAttribCodeParser.parse("CONDTN").description)
        assertEquals("FUNCTN == 5", OpenCpnAttribCodeParser.parse("fnctnm5").description)
        assertEquals("CATTRK == 3", OpenCpnAttribCodeParser.parse("cattml3").description)
    }

    @Test
    fun reportsLookupReferenceDiagnostics() {
        val inventory = OpenCpnPayloadInventoryReader.read(openCpnDir())
        val diagnostics = assertNotNull(inventory.lookupDiagnostics)

        assertEquals(3057, diagnostics.lookupCount)
        assertTrue(OpenCpnLookupTableName.Plain in diagnostics.tableNames)
        assertTrue(OpenCpnLookupTableName.Symbolized in diagnostics.tableNames)
        assertTrue(OpenCpnLookupTableName.Simplified in diagnostics.tableNames)
        assertTrue(OpenCpnLookupTableName.Paper in diagnostics.tableNames)
        assertTrue(OpenCpnLookupTableName.Lines in diagnostics.tableNames)
        assertTrue("LIGHTS05" in diagnostics.cspNames)
        assertTrue("DEPARE01" in diagnostics.cspNames)
    }

    private fun openCpnDir(): java.io.File = java.io.File("s52/opencpn")
}
