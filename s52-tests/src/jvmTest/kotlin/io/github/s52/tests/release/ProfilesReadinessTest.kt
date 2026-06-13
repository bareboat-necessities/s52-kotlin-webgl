package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ProfilesReadinessTest {
    @Test
    fun ProfileFilesArePresent() {
        val root = locateProjectRoot()
        val required = listOf(
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52Profile.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52ProfileTest.kt",
            "docs/PROFILES_.md",
            "samples/integration/profiles/README.md"
        )

        val missing = required.filterNot { File(root, it).isFile }
        assertTrue(missing.isEmpty(), "Missing Phase 18 profile files: $missing")
    }

    @Test
    fun DocsKeepSafetyBoundary() {
        val root = locateProjectRoot()
        val readme = File(root, "README.md").readText()
        val phaseDocs = File(root, "docs/PROFILES_.md").readText()

        assertTrue(readme.contains("Check"), "README must mention Check")
        assertTrue(readme.contains("S52ProfileCatalog"), "README must mention profile API")
        assertTrue(phaseDocs.contains("not for navigation", ignoreCase = true), "Phase 18 docs must preserve safety boundary")
    }

    private fun locateProjectRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        repeat(8) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile ?: dir
        }
        error("Could not locate project root from ${System.getProperty("user.dir")}")
    }
}
