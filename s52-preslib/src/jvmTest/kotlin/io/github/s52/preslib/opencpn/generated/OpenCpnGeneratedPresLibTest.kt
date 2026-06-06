package io.github.s52.preslib.opencpn.generated

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenCpnGeneratedPresLibTest {
    @Test
    fun generatedSourcePackContainsCompleteOpenCpnInventory() {
        val source = OpenCpnGeneratedPresLib.sourcePack()
        assertEquals(3057, source.lookupRecords.size)
        assertEquals(1093, source.symbols.size)
        assertEquals(57, source.lineStyles.size)
        assertEquals(30, source.patterns.size)
        assertEquals(5, source.colorTables.size)
        assertTrue(source.colorTables.all { it.colors.size == 63 })
        assertEquals(1083, source.symbols.count { it.bitmap != null })
        assertEquals(375, source.symbols.count { !it.vectorHpgl.isNullOrBlank() })
        assertEquals(57, source.lineStyles.count { !it.vectorHpgl.isNullOrBlank() })
        assertEquals(8, source.patterns.count { it.bitmap != null })
        assertEquals(25, source.patterns.count { !it.vectorHpgl.isNullOrBlank() })
    }

    @Test
    fun runtimePackExposesOpenCpnRegistriesWithoutChangingRendererDefaults() {
        val pack = OpenCpnGeneratedPresLib.pack()
        assertEquals(3057, pack.lookupTable.records().size)
        assertEquals(1093, pack.symbols.names().size)
        assertEquals(57, pack.lineStyles.names().size)
        assertEquals(30, pack.patterns.names().size)
        assertTrue(pack.symbols.require("ACHARE02").bitmap != null)
        assertTrue(pack.symbols.require("ACHARE02").vectorHpgl?.isNotBlank() == true)
    }
}
