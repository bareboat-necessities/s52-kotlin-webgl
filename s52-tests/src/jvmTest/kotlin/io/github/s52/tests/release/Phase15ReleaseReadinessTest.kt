package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class Phase15ReleaseReadinessTest {
    private val root: File = findRepositoryRoot()

    @Test
    fun requiredReleaseFilesExist() {
        val required = listOf(
            "README.md",
            "CHANGELOG.md",
            "CONTRIBUTING.md",
            "SECURITY.md",
            "samples/integration/minimal-core/README.md",
            ".github/workflows/ci.yml",
            ".github/workflows/release.yml"
        )

        val missing = required.filterNot { root.resolve(it).isFile }
        assertTrue(missing.isEmpty(), "Missing Phase 15 release files: $missing")
    }

    @Test
    fun readmeKeepsSafetyBoundary() {
        val readme = root.resolve("README.md").readText()
        assertTrue(readme.contains("Experimental"))
        assertTrue(readme.contains("Not type-approved ECDIS"))
        assertTrue(readme.contains("Not for navigation"))
    }

    @Test
    fun ciUsesPhase15Check() {
        val ci = root.resolve(".github/workflows/ci.yml").readText()
        assertTrue(ci.contains("phase15Check") || ci.contains("phase16Check"))
    }

    @Test
    fun releaseWorkflowBuildsPhase15Archive() {
        val release = root.resolve(".github/workflows/release.yml").readText()
        assertTrue(release.contains("phase15Check") || release.contains("phase16Check"))
        assertTrue(release.contains("phase15SourceArchive") || release.contains("phase16SourceArchive"))
        assertTrue(release.contains("actions/upload-artifact"))
    }

    @Test
    fun rootBuildDefinesPhase15Tasks() {
        val build = root.resolve("build.gradle.kts").readText()
        assertTrue(build.contains("phase15ReleaseAudit"))
        assertTrue(build.contains("phase15SourceArchive"))
        assertTrue(build.contains("phase15Check"))
    }

    private fun findRepositoryRoot(): File {
        var cursor = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (cursor.resolve("settings.gradle.kts").isFile) return cursor
            cursor = cursor.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }
}
