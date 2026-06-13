package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DiagnosticsReadinessTest {
    private val root: File = findRepositoryRoot()

    @Test
    fun requiredDiagnosticsFilesExist() {
        val required = listOf(
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52DiagnosticBundle.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52DiagnosticBundleTest.kt",
            "docs/DIAGNOSTICS_PHASE17.md",
            "samples/integration/diagnostics/README.md"
        )

        val missing = required.filterNot { root.resolve(it).isFile }
        assertTrue(missing.isEmpty(), "Missing Phase 17 diagnostic files: $missing")
    }

    @Test
    fun ciAndReleaseUsePhase17Checks() {
        val ci = root.resolve(".github/workflows/ci.yml").readText()
        val release = root.resolve(".github/workflows/release.yml").readText()
        assertTrue(ci.contains("phase17Check"))
        assertTrue(release.contains("phase17Check"))
        assertTrue(release.contains("phase17SourceArchive"))
    }

    @Test
    fun rootBuildDefinesPhase17Tasks() {
        val build = root.resolve("build.gradle.kts").readText()
        assertTrue(build.contains("phase17DiagnosticsAudit"))
        assertTrue(build.contains("phase17Check"))
        assertTrue(build.contains("phase17SourceArchive"))
    }

    private fun findRepositoryRoot(): File {
        var cursor = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (cursor.resolve("settings.gradle.kts").isFile) return cursor
            cursor = cursor.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }
}
