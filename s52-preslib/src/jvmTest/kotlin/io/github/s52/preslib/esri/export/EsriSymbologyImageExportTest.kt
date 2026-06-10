package io.github.s52.preslib.esri.export

import kotlin.test.Test
import kotlin.test.assertTrue

class EsriSymbologyImageExportTest {
    @Test
    fun phaseAtlasExporterEntryPointExists() {
        assertTrue(EsriSymbologyImageExportMain::class.java.name.endsWith("EsriSymbologyImageExportMain"))
    }
}
