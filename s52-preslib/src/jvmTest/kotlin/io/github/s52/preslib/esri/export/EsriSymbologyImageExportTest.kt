package io.github.s52.preslib.esri.export

import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib
import kotlin.test.assertTrue

class EsriSymbologyImageExportTest {
    @Test
    fun phaseAtlasExporterEntryPointExists() {
        assertTrue(EsriSymbologyImageExportMain::class.java.name.endsWith("EsriSymbologyImageExportMain"))
    }

    @Test
    fun opencpnNameHelpersPreserveAtlasContract() {
        assertEquals("BOYLAT", openCpnObjectPrefix("BOYLAT13"))
        assertEquals("WRECKS", openCpnObjectPrefix("WRECKS05"))
        assertEquals("BOYLAT13", canonicalOpenCpnKey("boylat13.svg"))
        assertEquals("q20bconicalbuoy", normalize("Q20b_Conical_buoy"))
    }

    @Test
    fun opencpnCoverageOracleContainsFullObjectSetForEsriAtlas() {
        val pack = OpenCpnGeneratedPresLib.sourcePack()
        val objectCount = pack.lookupRecords.map { it.objectClassKey.acronym }.distinct().size

        assertEquals(OpenCpnGeneratedPresLib.SYMBOL_COUNT, pack.symbols.size)
        assertEquals(OpenCpnGeneratedPresLib.LINE_STYLE_COUNT, pack.lineStyles.size)
        assertEquals(OpenCpnGeneratedPresLib.PATTERN_COUNT, pack.patterns.size)
        assertTrue(objectCount >= 100, "ESRI atlas must be driven by the OpenCPN lookup object set, not only raw ESRI SVG filenames")
    }
}
