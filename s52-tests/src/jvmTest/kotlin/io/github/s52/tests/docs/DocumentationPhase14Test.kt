package io.github.s52.tests.docs

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class DocumentationPhase14Test {
    @Test
    fun phase14DocumentationIndexExists() {
        val requiredDocs = listOf(
            "docs/ARCHITECTURE.md",
            "docs/SAFETY_LEGAL.md",
            "docs/CONTRIBUTING.md",
            "docs/ADDING_CSP.md",
            "docs/TESTING_AND_VALIDATION.md",
            "docs/EXAMPLES_PHASE14.md",
            "docs/DOCUMENTATION_PHASE14.md"
        )

        requiredDocs.forEach { path ->
            val file = File(path)
            assertTrue(file.isFile, "Missing documentation file: $path")
            assertTrue(file.readText().length > 400, "Documentation file is unexpectedly small: $path")
        }
    }

    @Test
    fun sampleExamplesExistAndUsePublicFacade() {
        val samples = listOf(
            "samples/minimal-api/MinimalApi.kt",
            "samples/transcript/TranscriptExample.kt",
            "samples/custom-preslib/CustomPresLibExample.kt",
            "samples/webgl-browser/README.md"
        )

        samples.forEach { path ->
            val file = File(path)
            assertTrue(file.isFile, "Missing sample file: $path")
        }

        assertTrue(File("samples/minimal-api/MinimalApi.kt").readText().contains("S52.defaultRuntime()"))
        assertTrue(File("samples/transcript/TranscriptExample.kt").readText().contains("runtime.transcript"))
        assertTrue(File("samples/custom-preslib/CustomPresLibExample.kt").readText().contains("S52Runtime.from"))
        assertTrue(File("samples/webgl-browser/README.md").readText().contains("WebGlS52Renderer"))
    }
}
