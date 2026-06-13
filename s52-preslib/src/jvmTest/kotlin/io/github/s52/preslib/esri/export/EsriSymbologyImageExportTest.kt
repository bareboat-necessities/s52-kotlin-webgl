package io.github.s52.preslib.esri.export

import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EsriSymbologyImageExportTest {

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

    @Test
    fun enhancedSvgRecolorsMonochromeEsriGeometryAndAddsIdentityOverlay() {
        val svg = EsriSymbologyImageExportMain.enhancedEsriSvgForOpenCpn(
            sourceXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 72 72">
                  <path d="M12 12 L60 12 L36 60 Z" style="fill:#000000;stroke:#000000"/>
                </svg>
            """.trimIndent(),
            openCpnName = "LIGHTS11",
            category = "POINT",
            esriName = "Light.svg",
            matchKind = "ALIAS",
            reason = "test alias"
        )

        assertTrue(svg.contains("data-enhanced-svg=\"true\""))
        assertTrue(svg.contains("data-match-kind=\"ALIAS\""))
        assertTrue(svg.contains("data-enhancement-palette=\"opencpn-inspired-day\""))
        assertTrue(svg.contains("fill:#ffd21f"), "Light symbols should receive an OpenCPN/S-52-inspired yellow fill")
        assertTrue(svg.contains("id=\"opencpn-enhancement\""), "Enhanced SVGs should carry deterministic identity/category marks")
        assertFalse(svg.contains("<?xml"), "Enhanced SVG copies must remain browser-loadable after metadata injection")
    }

    @Test
    fun enhancedUnresolvedPlaceholderIsDistinctInsteadOfBlank() {
        val svg = EsriSymbologyImageExportMain.enhancedPlaceholderSvg("NOESRI01", "POINT", "no match")

        assertTrue(svg.contains("data-enhanced-svg=\"true\""))
        assertTrue(svg.contains("<path"), "Enhanced unresolved slots should remain visible and reviewable in the portrayal-input set")
        assertTrue(svg.contains("<circle"), "Enhanced unresolved slots should include deterministic identity marks")
        assertTrue(svg.contains("stroke-linecap=\"round\""), "Enhanced unresolved slots should include a color stripe for review distinctness")
    }

}
