package io.github.s52.preslib.opencpn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenCpnChartSymbolsImporterTest {
    @Test
    fun parsesOpenCpnXmlWithNestedHpgl() {
        val xml = buildString {
            append("<chartsymbols><color-tables><color-table name='Day'><color name='CHBLK' r='0' g='0' b='0'/><color name='CHRED' r='200' g='0' b='0'/></color-table></color-tables><symbols>")
            repeat(55) { i ->
                append("<symbol RCID='$i'><name>SYM%03d</name><definition>V</definition><color-ref>CHBLK CHRED</color-ref><vector width='16' height='16'><pivot x='8' y='8'/><HPGL>SP2;PU0,0;PD10,0;PD10,10;</HPGL></vector></symbol>".format(i))
            }
            append("</symbols><line-styles><line-style RCID='100'><name>DASH</name><color-ref>CHRED</color-ref><HPGL>SP1;PU0,0;PD10,0;</HPGL></line-style></line-styles><patterns><pattern RCID='200'><name>HATCH</name><definition>V</definition><color-ref>CHRED</color-ref><HPGL>SP1;PU0,0;PD10,10;</HPGL></pattern></patterns></chartsymbols>")
        }
        val renderable = OpenCpnChartSymbolsImporter.importRenderableXml(xml)
        assertEquals("opencpn-chartsymbols-imported", renderable.sourcePack.metadata.edition)
        assertTrue(renderable.symbols.size >= 55)
        assertTrue(renderable.lineStyles.isNotEmpty())
        assertTrue(renderable.patterns.isNotEmpty())
        assertEquals(listOf("CHBLK", "CHRED"), renderable.symbols.first().colorRefs)
        assertTrue(renderable.symbols.first().hpgl.contains("SP2"))
    }
}
