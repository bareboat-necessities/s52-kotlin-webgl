package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class Phase18ProfilesReadinessTest {
    @Test
    fun phase18ProfileFilesArePresent() {
        val root = locateProjectRoot()
        val required = listOf(
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52Profile.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52ProfileTest.kt",
            "docs/PROFILES_PHASE18.md",
            "samples/integration/profiles/README.md"
        )

        val missing = required.filterNot { File(root, it).isFile }
        assertTrue(missing.isEmpty(), "Missing Phase 18 profile files: $missing")
    }

    @Test
    fun phase18DocsKeepSafetyBoundary() {
        val root = locateProjectRoot()
        val readme = File(root, "README.md").readText()
        val phaseDocs = File(root, "docs/PROFILES_PHASE18.md").readText()

        assertTrue(readme.contains("phase18Check"), "README must mention phase18Check")
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
