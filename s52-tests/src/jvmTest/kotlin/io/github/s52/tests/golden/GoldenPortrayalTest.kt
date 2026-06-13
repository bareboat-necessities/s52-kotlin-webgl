package io.github.s52.tests.golden

import io.github.s52.core.draw.S52DrawCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GoldenPortrayalTest {
    private val runner = GoldenPortrayalRunner()

    @Test
    fun everyGoldenCaseMatchesCheckedInTranscript() {
        Phase10GoldenCases.all().forEach { case ->
            val expected = readResource(case.expectedResource)
            val actual = runner.transcript(case)
            val comparison = GoldenTranscriptComparison(expected, actual)
            assertTrue(comparison.matches, comparison.failureMessage(case.id))
        }
    }

    @Test
    fun goldenCaseIdsAreStableAndUnique() {
        val ids = Phase10GoldenCases.all().map { it.id }

        assertEquals(ids.sorted(), ids.sorted())
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.all { it.matches(Regex("[a-z0-9-]+")) })
    }

    @Test
    fun visibilityCaseSuppressesSoundingAndLightTextButKeepsLightGraphics() {
        val commands = runner.portray(Phase10GoldenCases.visibilitySettings())

        assertFalse(commands.any { it is S52DrawCommand.Sounding })
        assertFalse(commands.any { it is S52DrawCommand.Text })
        assertTrue(commands.any { it is S52DrawCommand.PointSymbol && it.symbolName == "LIGHTS11" })
        assertTrue(commands.any { it is S52DrawCommand.LineComplex && it.lineStyleName == "LIGHTSECTOR01" })
    }

    @Test
    fun transcriptComparisonReportsFirstMismatch() {
        val comparison = GoldenTranscriptComparison(
            expected = "one\ntwo\nthree\n",
            actual = "one\nTWO\nthree\n"
        )

        assertFalse(comparison.matches)
        val message = comparison.failureMessage("demo")
        assertTrue(message.contains("line 2"), message)
        assertTrue(message.contains("expected: two"), message)
        assertTrue(message.contains("actual:   TWO"), message)
    }

    private fun readResource(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
            ?: error("Missing golden resource $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
