package io.github.s52.tests.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S64CommandValidationPhase11Test {
    private val fixtureResources = listOf(
        "/validation/chart1-depth-danger.s52v",
        "/validation/s64-display-settings.s52v",
        "/validation/s64-overlay-quality.s52v"
    )

    @Test
    fun phase11ValidationFixturesParse() {
        val fixtures = loadFixtures()

        assertEquals(3, fixtures.size)
        assertTrue(fixtures.all { it.id.matches(Regex("[a-z0-9-]+")) })
        assertTrue(fixtures.all { it.features.isNotEmpty() })
        assertTrue(fixtures.all { it.expectedLineCount > 0 })
        assertEquals(fixtures.size, fixtures.map { it.id }.toSet().size)
    }

    @Test
    fun phase11ValidationFixturesPassCommandLevelValidation() {
        val report = S64CommandValidationRunner().report(loadFixtures())

        assertTrue(report.isSuccess, report.toMarkdown() + "\n" + report.results.joinToString("\n") { it.failureMessage() })
        assertEquals(3, report.total)
        assertEquals(3, report.passed)
        assertEquals(0, report.failed)
    }

    @Test
    fun reportIdentifiesTranscriptMismatch() {
        val fixture = loadFixtures().first()
        val broken = fixture.copy(
            expectedTranscript = fixture.expectedTranscript.replace("DEPVS", "BROKEN", ignoreCase = false)
        )
        val result = S64CommandValidationRunner().run(broken)

        assertFalse(result.passed)
        val message = result.failureMessage()
        assertTrue(message.contains(fixture.id), message)
        assertTrue(message.contains("line 1"), message)
        assertTrue(message.contains("BROKEN"), message)
    }

    @Test
    fun parserSupportsExternalFixtureValueTypes() {
        val text = """
            id=parser-value-types
            source=Custom
            description=Parser smoke test.
            settings.enabledViewingGroups=21010,33010
            settings.disabledViewingGroups=99999
            context.viewportId=parser-smoke
            feature=1|DEPARE|Area|DRVAL1=D:0.0;DRVAL2=D:4.0;OBJNAM=T:Name;RESTRN=L:I:1,I:7|POLYGON(-1,1;0,1;0,2;-1,1)
            expect:
            {}
        """.trimIndent()

        val fixture = ValidationFixtureParser.parse(text)

        assertEquals("parser-value-types", fixture.id)
        assertEquals(setOf(21010, 33010), fixture.settings.enabledViewingGroups)
        assertEquals(setOf(99999), fixture.settings.disabledViewingGroups)
        assertEquals("parser-smoke", fixture.context.viewportId)
        assertEquals(1, fixture.features.size)
    }

    private fun loadFixtures(): List<CommandValidationFixture> =
        fixtureResources.map { path -> ValidationFixtureParser.parse(readResource(path)) }

    private fun readResource(path: String): String {
        val stream = javaClass.getResourceAsStream(path)
            ?: error("Missing validation fixture $path")
        return stream.bufferedReader().use { it.readText() }
    }
}
