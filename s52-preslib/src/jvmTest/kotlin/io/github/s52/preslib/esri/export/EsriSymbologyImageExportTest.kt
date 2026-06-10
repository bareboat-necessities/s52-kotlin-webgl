package io.github.s52.preslib.esri.export

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
