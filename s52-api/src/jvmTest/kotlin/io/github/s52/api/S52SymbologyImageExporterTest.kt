package io.github.s52.api

import io.github.s52.api.tools.S52SymbologyImageExporter
import io.github.s52.preslib.opencpn.OpenCpnChartSymbolsImporter
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun exporterWritesBoundsSafeColoredSvgArtifactsFromOpenCpnFixture() {
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
        assertTrue("svgColorAware=true" in manifest)
        assertTrue("svgContourAware=true" in manifest)
        assertTrue("svgBoundsAware=true" in manifest)
        assertTrue("lineStyleSampleRepeated=true" in manifest)
        assertTrue("hpglArcCenterAware=true" in manifest)
        assertTrue("svgCompactColorRefAware=true" in manifest)

        val coloredSymbolSvg = dir.resolve("symbols/SYM000.svg").readText()
        assertTrue("#C80000" in coloredSymbolSvg)
        assertTrue("#00A000" in coloredSymbolSvg)
        assertTrue("colorRefs=CHBLK,CHRED,CHGRN" in coloredSymbolSvg)
        assertTrue("unresolvedColors=" in coloredSymbolSvg)
        assertTrue("fill-rule=\"evenodd\"" in coloredSymbolSvg)
        assertTrue("overflow=\"visible\"" in coloredSymbolSvg)
        assertTrue("viewBox=\"0 0" in coloredSymbolSvg)

        val lineStyleSvg = dir.resolve("lines/LINETST.svg").readText()
        assertTrue("line-style-tile" in lineStyleSvg)
        assertTrue("repeated line-style sample" in lineStyleSvg)
        assertTrue(lineStyleSvg.split("line-style-tile").size > 2)
    }

    @Test
    fun exporterRejectsTinyPack() {
        val tiny = OpenCpnChartSymbolsImporter.importXml(
            """
            <chartsymbols>
              <symbols>
                <symbol><name>ONE</name><definition>V</definition><color-ref>CHBLK</color-ref><vector width="8" height="8"><HPGL>SP1;PU0,0;PD5,0;PD5,5;</HPGL></vector></symbol>
              </symbols>
            </chartsymbols>
            """.trimIndent(),
            "tiny.xml"
        )
        assertFailsWith<IllegalArgumentException> {
            S52SymbologyImageExporter.exportSourcePack(File("build/tiny-symbology-images"), tiny)
        }
    }

    private fun fixtureFile(): File = File(requireNotNull(javaClass.getResource("/opencpn/chartsymbols-fixture.xml")).toURI())
}
