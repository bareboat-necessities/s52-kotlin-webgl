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
        val runtimeLookupCount = OpenCpnGeneratedPresLib.LOOKUP_COUNT + 2

        // Diagnostics report the normalized runtime pack. The raw generated
        // OpenCPN inventory stays at LOOKUP_COUNT, while the runtime pack adds
        // two compatibility rows for client-log coverage: ACHARE/Line and OBJL_0/Line.
        assertEquals(runtimeLookupCount, report.lookupCount)
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
        assertEquals(report.symbolCount, report.coverageIndex.symbols.declared)
        assertEquals(report.patternCount, report.coverageIndex.patterns.declared)
        assertTrue(report.coverageIndex.hpgl.compiledDisplayListAssetCount > 0)
        assertTrue(report.coverageIndex.hpgl.fillCapableAssetCount > 0)
    }

    @Test
    fun openCpnCoverageIndexSummarizesAssetResolution() {
        val index = S52OpenCpnDiagnostics.coverageIndex()

        assertTrue(index.symbols.declared >= index.symbols.resolved)
        assertTrue(index.lineStyles.declared >= index.lineStyles.resolved)
        assertTrue(index.patterns.declared >= index.patterns.resolved)
        assertTrue(index.hpgl.assetCount == index.hpgl.compiledDisplayListAssetCount)
        assertTrue(index.primitiveLookupCounts.isNotEmpty())
        assertTrue(index.presentationTableLookupCounts.isNotEmpty())
    }

    @Test
    fun openCpnDiagnosticsCanBeRenderedAsText() {
        val runtimeLookupCount = OpenCpnGeneratedPresLib.LOOKUP_COUNT + 2
        val text = S52OpenCpnDiagnostics.report().toPlainText(maxItems = 4)

        assertTrue("lookups=$runtimeLookupCount" in text)
        assertTrue("symbols=1093" in text)
        assertTrue("presentationTables=" in text)
        assertTrue("unresolvedCsps=" in text)
    }
}
