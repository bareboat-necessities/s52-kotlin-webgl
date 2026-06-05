package io.github.s52.api

import io.github.s52.api.tools.S52SymbologyImageExporter
import io.github.s52.preslib.opencpn.OpenCpnChartSymbolsImporter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class S52SymbologyImageExporterTest {
    @Test
    fun importerLoadsRealisticOpenCpnFixture() {
        val pack = OpenCpnChartSymbolsImporter.importFile(fixtureFile())
        assertTrue(pack.symbols.size >= S52SymbologyImageExporter.MinimumRealSymbolCount)
        assertTrue(pack.lineStyles.isNotEmpty())
        assertTrue(pack.patterns.isNotEmpty())
        assertEquals("opencpn-chartsymbols-imported", pack.metadata.edition)
    }

    @Test
    fun exporterWritesSvgArtifactsFromOpenCpnFixture() {
        val dir = File("build/test-opencpn-symbology-images")
        val report = S52SymbologyImageExporter.exportImportedOpenCpn(dir, fixtureFile())
        assertTrue(report.symbolCount >= S52SymbologyImageExporter.MinimumRealSymbolCount)
        assertTrue(dir.resolve("index.html").isFile)
        assertTrue(dir.resolve("manifest.properties").isFile)
        assertTrue(dir.resolve("symbols").listFiles().orEmpty().size >= S52SymbologyImageExporter.MinimumRealSymbolCount)
        assertTrue(dir.resolve("lines").listFiles().orEmpty().isNotEmpty())
        assertTrue(dir.resolve("patterns").listFiles().orEmpty().isNotEmpty())
        val manifest = dir.resolve("manifest.properties").readText()
        assertTrue("edition=opencpn-chartsymbols-imported" in manifest)
        assertTrue("synthetic=false" in manifest)
    }

    @Test
    fun exporterRejectsTinyPack() {
        val tiny = OpenCpnChartSymbolsImporter.importXml("""
            <chartsymbols>
              <symbols>
                <symbol><name>ONE1</name><vector width="10" height="10"><pivot x="5" y="5"/><HPGL>PU0,0;PD5,0;PD5,5;</HPGL></vector></symbol>
              </symbols>
            </chartsymbols>
        """.trimIndent(), "tiny.xml")
        assertFailsWith<IllegalArgumentException> {
            S52SymbologyImageExporter.exportSourcePack(File("build/tiny-symbology-images"), tiny)
        }
    }

    private fun fixtureFile(): File = File(requireNotNull(javaClass.getResource("/opencpn/chartsymbols-fixture.xml")) .toURI())
}
