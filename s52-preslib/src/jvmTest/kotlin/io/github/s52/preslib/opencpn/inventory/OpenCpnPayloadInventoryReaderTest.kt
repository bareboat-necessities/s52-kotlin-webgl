package io.github.s52.preslib.opencpn.inventory

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenCpnPayloadInventoryReaderTest {
    @Test
    fun inventoriesCorrectedOpenCpnPayload() {
        val inventory = OpenCpnPayloadInventoryReader.read(findOpenCpnDirectory())
        val chartSymbols = assertNotNull(inventory.chartSymbols)

        assertEquals(5, chartSymbols.colorTables.size)
        assertTrue(chartSymbols.colorTables.all { it.colors.size == 63 })
        assertEquals(3057, chartSymbols.lookupCount)
        assertEquals(1093, chartSymbols.symbols.size)
        assertEquals(57, chartSymbols.lineStyles.size)
        assertEquals(30, chartSymbols.patterns.size)

        assertEquals(1083, chartSymbols.symbols.count { it.bitmap != null })
        assertEquals(375, chartSymbols.symbols.count { it.vector != null && it.vector.hpgl.isNotBlank() })
        assertEquals(57, chartSymbols.lineStyles.count { it.vector != null && it.vector.hpgl.isNotBlank() })
        assertEquals(8, chartSymbols.patterns.count { it.bitmap != null })
        assertEquals(25, chartSymbols.patterns.count { it.vector != null && it.vector.hpgl.isNotBlank() })

        assertEquals(3, inventory.rasterAtlases.size)
        assertTrue(inventory.rasterAtlases.all { it.width == 1500 && it.height == 1200 })
        assertEquals(setOf(OpenCpnRasterPaletteHint.Day, OpenCpnRasterPaletteHint.Dusk, OpenCpnRasterPaletteHint.Dark), inventory.rasterAtlases.map { it.paletteHint }.toSet())

        assertTrue(inventory.csvCatalog.objectClasses.size >= 217)
        assertTrue(inventory.csvCatalog.attributes.size >= 200)
        assertTrue(inventory.csvCatalog.expectedInputs.size >= 500)
        assertTrue(inventory.csvCatalog.attributeDecodes.size >= 100)

        assertFalse(inventory.diagnostics.hasIssues(), inventory.toHumanText())
    }

    private fun findOpenCpnDirectory(): File {
        val start = File(System.getProperty("user.dir")).absoluteFile
        generateSequence(start) { it.parentFile }.forEach { dir ->
            val candidate = dir.resolve("s52/opencpn")
            if (candidate.isDirectory) return candidate
            val parentCandidate = dir.resolve("../s52/opencpn").canonicalFile
            if (parentCandidate.isDirectory) return parentCandidate
        }
        error("Could not find s52/opencpn from $start")
    }
}
