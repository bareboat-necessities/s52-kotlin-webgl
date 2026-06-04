package io.github.s52.tests.release

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class Phase19ArtifactsReadinessTest {
    @Test
    fun phase19ArtifactFilesArePresent() {
        val root = locateProjectRoot()
        val required = listOf(
            "s52-api/src/commonMain/kotlin/io/github/s52/api/S52ArtifactBundle.kt",
            "s52-api/src/jvmTest/kotlin/io/github/s52/api/S52ArtifactBundleTest.kt",
            "docs/ARTIFACTS_PHASE19.md",
            "samples/integration/artifacts/README.md"
        )

        val missing = required.filterNot { File(root, it).isFile }
        assertTrue(missing.isEmpty(), "Missing Phase 19 artifact files: $missing")
    }

    @Test
    fun phase19DocsKeepSafetyBoundary() {
        val root = locateProjectRoot()
        val readme = File(root, "README.md").readText()
        val phaseDocs = File(root, "docs/ARTIFACTS_PHASE19.md").readText()

        assertTrue(readme.contains("phase19Check"), "README must mention phase19Check")
        assertTrue(readme.contains("S52ArtifactBundle"), "README must mention artifact bundle API")
        assertTrue(phaseDocs.contains("not for navigation", ignoreCase = true), "Phase 19 docs must preserve safety boundary")
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
