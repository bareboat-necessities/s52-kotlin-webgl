package io.github.s52.api

import io.github.s52.api.tools.S52SymbologyImageExporter
import io.github.s52.preslib.opencpn.OpenCpnChartSymbolsImporter
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
    fun exporterEmbedsBitmapCellsInsteadOfRenderingPivotMarkers() {
        val root = File("build/test-opencpn-bitmap-fixture").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        val atlas = BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val red = if (x < 4) 0x20 else 0xC0
                val green = if (y < 4) 0x80 else 0x20
                atlas.setRGB(x, y, (0xFF shl 24) or (red shl 16) or (green shl 8) or 0x40)
            }
        }
        ImageIO.write(atlas, "png", root.resolve("rastersymbols-day.png"))

        val symbols = buildString {
            repeat(55) { index ->
                append(
                    """
                    <symbol RCID="$index">
                      <name>BMP%03d</name>
                      <definition>R</definition>
                      <bitmap width="4" height="4">
                        <pivot x="2" y="2"/>
                        <origin x="0" y="0"/>
                        <graphics-location x="0" y="0"/>
                      </bitmap>
                    </symbol>
                    """.trimIndent().format(index)
                )
            }
        }
        val xml = """
            <chartsymbols>
              <symbols>$symbols</symbols>
            </chartsymbols>
        """.trimIndent()
        val chartsymbols = root.resolve("chartsymbols.xml")
        chartsymbols.writeText(xml)

        val output = File("build/test-opencpn-bitmap-svg-export")
        S52SymbologyImageExporter.exportImportedOpenCpn(output, chartsymbols)
        val svg = output.resolve("symbols/BMP000.svg").readText()

        assertTrue("data:image/png;base64," in svg, "Bitmap SVG must embed the cropped atlas cell so gallery <img> thumbnails render reliably")
        assertFalse("<circle" in svg, "Bitmap SVG must not draw the red pivot marker that made every failed atlas image look like a red circle")
        assertFalse("fill=\"#D1242F\"" in svg, "Bitmap SVG must not contain the old red pivot marker color")
        assertFalse("href=\"../rastersymbols-day.png\"" in svg, "Bitmap SVG should not depend on a nested external atlas reference")
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
