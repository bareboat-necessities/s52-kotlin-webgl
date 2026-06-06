package io.github.s52.preslib.opencpn.inventory

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenCpnCsvCatalogParserTest {
    @Test
    fun parsesQuotedCsvLineWithEmbeddedCommaAndEscapedQuote() {
        val cells = OpenCpnCsvCatalogParser.parseCsvLine("1,\"light, major\",\"a \"\"quoted\"\" value\"")
        assertEquals(listOf("1", "light, major", "a \"quoted\" value"), cells)
    }
}
