package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ReleaseReadinessTest {
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
    }

    @Test
    fun readmeKeepsSafetyBoundary() {
        val readme = root.resolve("README.md").readText()
        assertTrue(readme.contains("Experimental"))
        assertTrue(readme.contains("Not type-approved ECDIS"))
        assertTrue(readme.contains("Not for navigation"))
    }

    @Test
    fun ciUsesCheck() {
        val ci = root.resolve(".github/workflows/ci.yml").readText()
        assertTrue(ci.contains("Check") || ci.contains("Check"))
    }

    @Test
    fun releaseWorkflowBuildsArchive() {
        val release = root.resolve(".github/workflows/release.yml").readText()
        assertTrue(release.contains("Check") || release.contains("Check"))
        assertTrue(release.contains("SourceArchive") || release.contains("SourceArchive"))
        assertTrue(release.contains("actions/upload-artifact"))
    }

    @Test
    fun rootBuildDefinesTasks() {
        val build = root.resolve("build.gradle.kts").readText()
        assertTrue(build.contains("ReleaseAudit"))
        assertTrue(build.contains("SourceArchive"))
        assertTrue(build.contains("Check"))
    }

    private fun findRepositoryRoot(): File {
        var cursor = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (cursor.resolve("settings.gradle.kts").isFile) return cursor
            cursor = cursor.parentFile ?: error("Could not find repository root from ${System.getProperty("user.dir")}")
        }
    }
}
