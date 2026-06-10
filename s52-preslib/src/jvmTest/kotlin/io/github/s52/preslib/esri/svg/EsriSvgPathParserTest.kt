package io.github.s52.preslib.esri.svg

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsriSvgPathParserTest {
    @Test
    fun parsesBasicPathCommands() {
        val parsed = EsriSvgPathParser.parse("M0 0 L1 0 L1 1 Z")
        assertEquals(listOf('M', 'L', 'L', 'Z'), parsed.commands.map { it.command })
        assertTrue(parsed.unsupportedCommands.isEmpty())
    }

    @Test
    fun recordsUnsupportedArcCommand() {
        val parsed = EsriSvgPathParser.parse("M0 0 A1 1 0 0 1 2 2 Z")
        assertEquals(listOf('A'), parsed.unsupportedCommands)
    }
}
