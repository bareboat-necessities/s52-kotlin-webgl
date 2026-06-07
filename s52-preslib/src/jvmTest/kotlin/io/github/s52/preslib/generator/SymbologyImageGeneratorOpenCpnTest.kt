package io.github.s52.preslib.generator

import io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymbologyImageGeneratorOpenCpnTest {
    @Test
    fun generatedOpenCpnBitmapSymbolsRenderBitmapCellsNotRedPivotDots() {
        val output = Path.of("build", "test-opencpn-generated-symbology-images")
        SymbologyImageGenerator.generate(output, OpenCpnGeneratedPresLib.pack())

        val svg = output.resolve("symbols").resolve("achare02.svg").readText()

        assertTrue("data:image/png;base64," in svg, "OpenCPN bitmap symbol SVG must embed a cropped raster atlas cell")
        assertTrue("xmlns:xlink=\"http://www.w3.org/1999/xlink\"" in svg, "OpenCPN bitmap SVG must declare xlink for strict SVG viewers")
        assertTrue("xlink:href=\"data:image/png;base64," in svg, "OpenCPN bitmap SVG must include xlink:href, not only SVG2 href")
        assertFalse("<circle" in svg, "OpenCPN bitmap symbol SVG must not render only a red pivot circle")
        assertFalse("#d1242f" in svg.lowercase(), "OpenCPN bitmap symbol SVG must not contain the old red pivot marker color")
        assertFalse("atlas missing" in svg, "OpenCPN bitmap symbol SVG must find the bundled rastersymbols-day.png atlas")

        val embedded = decodeEmbeddedPng(svg)
        assertEquals(13, embedded.width, "ACHARE02 embedded crop width should match chartsymbols.xml bitmap width")
        assertEquals(16, embedded.height, "ACHARE02 embedded crop height should match chartsymbols.xml bitmap height")
        assertTrue(embedded.visiblePixelCount() > 0, "ACHARE02 embedded bitmap must not be transparent/empty")
        assertTrue(embedded.nonWhitePixelCount() > 0, "ACHARE02 embedded bitmap must contain visible symbol pixels")
    }

    private fun decodeEmbeddedPng(svg: String): BufferedImage {
        val match = Regex("data:image/png;base64,([A-Za-z0-9+/=]+)").find(svg)
            ?: error("SVG did not contain an embedded PNG data URI")
        val bytes = Base64.getDecoder().decode(match.groupValues[1])
        return requireNotNull(ImageIO.read(ByteArrayInputStream(bytes))) { "Embedded PNG data URI could not be decoded" }
    }

    private fun BufferedImage.visiblePixelCount(): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                if ((getRGB(x, y) ushr 24) != 0) count++
            }
        }
        return count
    }

    private fun BufferedImage.nonWhitePixelCount(): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val argb = getRGB(x, y)
                val alpha = argb ushr 24
                val rgb = argb and 0x00FFFFFF
                if (alpha != 0 && rgb != 0x00FFFFFF) count++
            }
        }
        return count
    }
}
