package io.github.s52.preslib.esri.svg

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EsriSvgParserTest {
    @Test
    fun parsesMinimalEsriLikeSvg() {
        val file = File.createTempFile("esri-symbol", ".svg")
        file.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <svg width="2.4042mm" height="2.2577mm" viewBox="-1.2 -1.93 2.4042 2.2577" xmlns="http://www.w3.org/2000/svg">
              <path id="solid_color" d="M0 0 L1 0 L1 1 Z" style="fill:#231f20"/>
              <path d="M0 0 L1 1" style="fill:none;stroke:#231f20;stroke-width:.11994"/>
            </svg>
            """.trimIndent()
        )

        val parsed = EsriSvgParser.parse(file, category = "point")
        assertEquals(2.4042, parsed.widthMm!!, 1.0e-6)
        assertEquals(2.2577, parsed.heightMm!!, 1.0e-6)
        assertNotNull(parsed.viewBox)
        assertTrue(parsed.viewBox!!.isValid)
        assertEquals(2, parsed.paths.size)
        assertTrue(parsed.hasGeometry)
        assertTrue(parsed.isSubsetSupported)
    }
}
