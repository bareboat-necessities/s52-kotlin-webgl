package io.github.s52.preslib.generator

import io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SymbologyImageGeneratorOpenCpnTest {
    @Test
    fun generatedOpenCpnBitmapSymbolsRenderBitmapCellsNotRedPivotDots() {
        val output = Path.of("build", "test-opencpn-generated-symbology-images")
        SymbologyImageGenerator.generate(output, OpenCpnGeneratedPresLib.pack())

        val svg = output.resolve("symbols").resolve("achare02.svg").readText()

        assertTrue("data:image/png;base64," in svg, "OpenCPN bitmap symbol SVG must embed a cropped raster atlas cell")
        assertFalse("<circle" in svg, "OpenCPN bitmap symbol SVG must not render only a red pivot circle")
        assertFalse("#d1242f" in svg.lowercase(), "OpenCPN bitmap symbol SVG must not contain the old red pivot marker color")
        assertFalse("atlas missing" in svg, "OpenCPN bitmap symbol SVG must find the bundled rastersymbols-day.png atlas")
    }
}
