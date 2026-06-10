package io.github.s52.preslib.esri.export

import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EsriSymbologyImageExportTest {
    @Test
    fun phaseAtlasExporterEntryPointExists() {
        assertTrue(EsriSymbologyImageExportMain::class.java.name.endsWith("EsriSymbologyImageExportMain"))
    }

    @Test
    fun opencpnNameHelpersPreserveAtlasContract() {
        assertEquals("BOYLAT", openCpnObjectPrefix("BOYLAT13"))
        assertEquals("WRECKS", openCpnObjectPrefix("WRECKS05"))
        assertEquals("BOYLAT13", canonicalOpenCpnKey("boylat13.svg"))
        assertEquals("q20bconicalbuoy", normalize("Q20b_Conical_buoy"))
    }

    @Test
    fun opencpnCoverageOracleContainsFullObjectSetForEsriAtlas() {
        val pack = OpenCpnGeneratedPresLib.sourcePack()
        val objectCount = pack.lookupRecords.map { it.objectClassKey.acronym }.distinct().size

        assertEquals(OpenCpnGeneratedPresLib.SYMBOL_COUNT, pack.symbols.size)
        assertEquals(OpenCpnGeneratedPresLib.LINE_STYLE_COUNT, pack.lineStyles.size)
        assertEquals(OpenCpnGeneratedPresLib.PATTERN_COUNT, pack.patterns.size)
        assertTrue(objectCount >= 100, "ESRI atlas must be driven by the OpenCPN lookup object set, not only raw ESRI SVG filenames")
    }

    @Test
    fun copiedEsriSvgStripsXmlDeclarationBeforeMetadataComment() {
        val copied = EsriSymbologyImageExportMain.esriSvgCopyAsStandaloneXml(
            "\uFEFF<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<svg xmlns=\"http://www.w3.org/2000/svg\"/>",
            "OpenCPN slot -- sanitize"
        )

        assertTrue(copied.startsWith("<!-- OpenCPN slot - - sanitize -->\n<svg"))
        assertFalse(copied.contains("<?xml"), "Generated SVG copies must not put metadata before an XML declaration")
        assertFalse(copied.contains("-- sanitize"), "XML comments must not contain illegal double hyphen sequences")
    }

    @Test
    fun unresolvedSlotsAreBlankNotGenericFallbackArt() {
        val svg = EsriSymbologyImageExportMain.unresolvedSvgForTest("NOESRI01", "no match")

        assertTrue(svg.contains("data-match-kind=\"UNRESOLVED\""))
        assertFalse(svg.contains("<rect"), "Unresolved exports must not draw fake fallback boxes")
        assertFalse(svg.contains("<path"), "Unresolved exports must not draw generic fallback symbols")
    }

    @Test
    fun exporterNoLongerHasCategoryOrRenderFallbackMatchKinds() {
        val kinds = EsriSymbologyImageExportMain.matchKindNamesForTest()

        assertFalse("CATEGORY_FALLBACK" in kinds)
        assertFalse("RENDER_FALLBACK" in kinds)
        assertTrue("UNRESOLVED" in kinds)
    }

}
