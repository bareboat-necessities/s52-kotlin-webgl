package io.github.s52.preslib.esri.generator

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsriSvgKotlinGeneratorTest {
    @Test
    fun writesGeneratedRegistryFile() {
        val root = Files.createTempDirectory("esri-source").toFile().apply { deleteOnExit() }
        val cpl = root.resolve("CustomPresentationLibrary")
        val point = cpl.resolve("symbols/point")
        cpl.resolve("symbols/line").mkdirs()
        cpl.resolve("symbols/pattern").mkdirs()
        cpl.resolve("lua").mkdirs()
        point.mkdirs()
        cpl.resolve("CustomSymbolMap.xml").writeText("<conditionMap name=\"INT1\"/>")
        point.resolve("QTEST.svg").writeText("""
            <svg width="1mm" height="1mm" viewBox="0 0 10 10" xmlns="http://www.w3.org/2000/svg">
              <path d="M 0 0 L 10 0 L 0 10 Z" fill="#231f20"/>
            </svg>
        """.trimIndent())
        val out = root.resolve("out/EsriGeneratedSymbolRegistry.kt")
        val summary = EsriSvgKotlinGenerator.generate(root, out)
        assertEquals(1, summary.generatedSymbolCount)
        assertEquals(0, summary.failedSymbolCount)
        val text = out.readText()
        assertContains(text, "object EsriGeneratedSymbolRegistry")
        assertContains(text, "QTEST.svg")
        assertContains(text, "EsriPaint.Token")
        assertTrue(text.contains("floatArrayOf"))
    }
}
