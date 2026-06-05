package io.github.s52.preslib.opencpn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenCpnChartSymbolsImporterTest {
    @Test
    fun parsesFixture() {
        val xml = buildString {
            append("<chartsymbols><symbols>")
            repeat(55) { i -> append("<symbol name='SYM%03d'><vector>PU0,0;PD10,0;PD10,10;PD0,10;PD0,0;</vector></symbol>".format(i)) }
            append("</symbols><line-styles><line-style name='DASH'/></line-styles><patterns><pattern name='HATCH'/></patterns></chartsymbols>")
        }
        val pack = OpenCpnChartSymbolsImporter.importXml(xml)
        assertEquals("opencpn-chartsymbols-imported", pack.metadata.edition)
        assertTrue(pack.symbols.size >= 55)
        assertTrue(pack.lineStyles.isNotEmpty())
        assertTrue(pack.patterns.isNotEmpty())
    }
}
