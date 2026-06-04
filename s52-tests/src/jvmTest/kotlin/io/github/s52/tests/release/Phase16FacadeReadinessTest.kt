package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class Phase16FacadeReadinessTest {
    private val root: File = findRepositoryRoot()

    @Test
    fun requiredFacadeFilesExist() {
        val required = listOf(
            "s52-api/build.gradle.kts",
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52PortrayalSession.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52PortrayalSessionTest.kt",
            "docs/API_FACADE_PHASE16.md",
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
    fun ciAndReleaseUsePhase16Checks() {
        val ci = root.resolve(".github/workflows/ci.yml").readText()
        val release = root.resolve(".github/workflows/release.yml").readText()
        assertTrue(ci.contains("phase16Check"))
        assertTrue(release.contains("phase16Check"))
        assertTrue(release.contains("phase16SourceArchive"))
    }

    @Test
    fun rootBuildDefinesPhase16Tasks() {
        val build = root.resolve("build.gradle.kts").readText()
        assertTrue(build.contains("phase16ApiAudit"))
        assertTrue(build.contains("phase16Check"))
        assertTrue(build.contains("phase16SourceArchive"))
    }

    private fun findRepositoryRoot(): File {
        var cursor = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (cursor.resolve("settings.gradle.kts").isFile) return cursor
            cursor = cursor.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }
}
