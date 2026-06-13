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
            "samples/integration/artifacts/README.md"
        )

        val missing = required.filterNot { File(root, it).isFile }
        assertTrue(missing.isEmpty(), "Missing Phase 19 artifact files: $missing")
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
