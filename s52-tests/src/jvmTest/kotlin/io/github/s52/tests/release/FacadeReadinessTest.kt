package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class FacadeReadinessTest {
    private val root: File = findRepositoryRoot()

    @Test
    fun requiredFacadeFilesExist() {
        val required = listOf(
            "s52-api/build.gradle.kts",
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52PortrayalSession.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52PortrayalSessionTest.kt",
            "samples/integration/facade/README.md"
        )

        val missing = required.filterNot { root.resolve(it).isFile }
        assertTrue(missing.isEmpty(), "Missing Phase 16 facade files: $missing")
    }

    @Test
    fun settingsIncludesApiModule() {
        val settings = root.resolve("settings.gradle.kts").readText()
        assertTrue(settings.contains("include(\":s52-api\")"))
    }

    @Test
    fun ciAndReleaseUseChecks() {
        val ci = root.resolve(".github/workflows/ci.yml").readText()
        val release = root.resolve(".github/workflows/release.yml").readText()
        assertTrue(ci.contains("Check"))
        assertTrue(release.contains("Check"))
        assertTrue(release.contains("SourceArchive"))
    }

    @Test
    fun rootBuildDefinesTasks() {
        val build = root.resolve("build.gradle.kts").readText()
        assertTrue(build.contains("ApiAudit"))
        assertTrue(build.contains("Check"))
        assertTrue(build.contains("SourceArchive"))
    }

    private fun findRepositoryRoot(): File {
        var cursor = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (cursor.resolve("settings.gradle.kts").isFile) return cursor
            cursor = cursor.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }
}
