package io.github.s52.preslib.opencpn

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenCpnChartSymbolsImporterTest {
    @Test
    fun parsesOpenCpnXmlHpglFixture() {
        val pack = OpenCpnChartSymbolsImporter.importXml(openCpnFixture(symbolCount = 55))
        assertEquals("opencpn-chartsymbols-imported", pack.metadata.edition)
        assertTrue(pack.symbols.size >= 55)
        assertTrue(pack.lineStyles.any { it.name == "DASH" })
        assertTrue(pack.patterns.any { it.name == "HATCH" })
        assertTrue(pack.symbols.first().commands.isNotEmpty())
    }

    private fun openCpnFixture(symbolCount: Int): String = buildString {
        append("<chartsymbols>")
        append("<color-tables><color-table name='DAY'><color name='CHBLK' r='0' g='0' b='0'/></color-table></color-tables>")
        append("<line-styles><line-style RCID='1'><name>DASH</name><description>dash</description><HPGL>PU0,0;PD20,0;</HPGL><vector width='20' height='4'><pivot x='0' y='0'/></vector></line-style></line-styles>")
        append("<patterns><pattern RCID='2'><name>HATCH</name><description>hatch</description><definition>V</definition><HPGL>PU0,0;PD10,10;</HPGL><vector width='10' height='10'><pivot x='5' y='5'/></vector></pattern></patterns>")
        append("<symbols>")
        repeat(symbolCount) { i ->
            append("<symbol RCID='${i + 10}'><name>SYM%03d</name><description>symbol</description><definition>V</definition><prefer-bitmap>no</prefer-bitmap><vector width='16' height='16'><pivot x='8' y='8'/><HPGL>PU0,0;PD10,0;PD10,10;PD0,10;PD0,0;</HPGL></vector></symbol>".format(i))
        }
        append("</symbols></chartsymbols>")
    }
}
