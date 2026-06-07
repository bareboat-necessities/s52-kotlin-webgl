package io.github.s52.api

import io.github.s52.api.tools.S52SymbologyImageExporter
import io.github.s52.preslib.opencpn.OpenCpnChartSymbolsImporter
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
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
    fun exporterCopiesDayDuskDarkPngAtlasesWhenPresentBesideChartsymbolsXml() {
        val dir = File("build/test-opencpn-symbology-atlases")
        val inputDir = File("build/test-opencpn-with-atlases").also { it.deleteRecursively(); it.mkdirs() }
        val chartsymbols = inputDir.resolve("chartsymbols.xml").also { it.writeText(fixtureFile().readText()) }
        writeDummyPng(inputDir.resolve("rastersymbols-day.png"), Color(240, 220, 200))
        writeDummyPng(inputDir.resolve("rastersymbols-dusk.png"), Color(120, 100, 160))
        writeDummyPng(inputDir.resolve("rastersymbols-dark.png"), Color(40, 50, 70))

        val report = S52SymbologyImageExporter.exportImportedOpenCpn(dir, chartsymbols)
        assertTrue(report.fileCount > 0)
        assertTrue(dir.resolve("symbol-atlas-day.png").isFile)
        assertTrue(dir.resolve("symbol-atlas-dusk.png").isFile)
        assertTrue(dir.resolve("symbol-atlas-dark.png").isFile)
        assertEquals(12, ImageIO.read(dir.resolve("symbol-atlas-day.png")).width)
        val manifest = dir.resolve("manifest.properties").readText()
        assertTrue("pngSymbolAtlases=3" in manifest)
        assertTrue("pngSymbolAtlasFiles=symbol-atlas-day.png,symbol-atlas-dusk.png,symbol-atlas-dark.png" in manifest)
        val index = dir.resolve("index.html").readText()
        assertTrue("PNG symbol atlases (3)" in index)
        assertTrue("symbol-atlas-day.png" in index)
        assertTrue("symbol-atlas-dusk.png" in index)
        assertTrue("symbol-atlas-dark.png" in index)
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


    private fun writeDummyPng(file: File, color: Color) {
        val image = BufferedImage(12, 10, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = color
        g.fillRect(0, 0, image.width, image.height)
        g.color = Color.BLACK
        g.drawRect(0, 0, image.width - 1, image.height - 1)
        g.dispose()
        file.parentFile?.mkdirs()
        ImageIO.write(image, "png", file)
    }

    private fun fixtureFile(): File = File(requireNotNull(javaClass.getResource("/opencpn/chartsymbols-fixture.xml")).toURI())
}
