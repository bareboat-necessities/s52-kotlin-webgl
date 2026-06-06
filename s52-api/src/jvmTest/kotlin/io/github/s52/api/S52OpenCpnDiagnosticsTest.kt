package io.github.s52.api

import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class S52OpenCpnDiagnosticsTest {
    @Test
    fun openCpnDiagnosticsExposeGeneratedPayloadCounts() {
        val report = S52OpenCpnDiagnostics.report()

        assertEquals(OpenCpnGeneratedPresLib.LOOKUP_COUNT, report.lookupCount)
        assertEquals(OpenCpnGeneratedPresLib.SYMBOL_COUNT, report.symbolCount)
        assertEquals(OpenCpnGeneratedPresLib.LINE_STYLE_COUNT, report.lineStyleCount)
        assertEquals(OpenCpnGeneratedPresLib.PATTERN_COUNT, report.patternCount)
        assertEquals(OpenCpnGeneratedPresLib.COLOR_TABLE_COUNT, report.colorTableCount)
        assertEquals(63, report.colorsPerPalette[S52Palette.DayBright])
        assertEquals(63, report.colorsPerPalette[S52Palette.Dusk])
        assertEquals(63, report.colorsPerPalette[S52Palette.Night])
        assertTrue(report.rasterSymbolCount >= 1000)
        assertTrue(report.vectorSymbolCount >= 300)
        assertTrue(report.vectorLineStyleCount == report.lineStyleCount)
        assertTrue(report.knownRasterAtlases.contains("rastersymbols-day.png"))
    }

    @Test
    fun openCpnDiagnosticsCanBeRenderedAsText() {
        val text = S52OpenCpnDiagnostics.report().toPlainText(maxItems = 4)

        assertTrue("lookups=3057" in text)
        assertTrue("symbols=1093" in text)
        assertTrue("presentationTables=" in text)
        assertTrue("unresolvedCsps=" in text)
    }
}
