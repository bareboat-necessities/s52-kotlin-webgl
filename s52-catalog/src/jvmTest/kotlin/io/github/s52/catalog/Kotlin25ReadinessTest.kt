package io.github.s52.catalog

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class Kotlin25ReadinessTest {
    @Test
    fun projectOwnedSourcesDoNotUsePrivateConstructorDataClasses() {
        val root = File(System.getProperty("user.dir")).parentFile ?: File(".")
        val offenders = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.inGeneratedOrBuildOutput() }
            .filter { file -> privateConstructorDataClassRegex.containsMatchIn(file.readText()) }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .toList()

        assertTrue(
            offenders.isEmpty(),
            "Kotlin 2.5 readiness: avoid data class private constructor patterns. Offenders: $offenders"
        )
    }

    private fun File.inGeneratedOrBuildOutput(): Boolean {
        val path = invariantSeparatorsPath
        return "/build/" in path || "/.gradle/" in path || "/.kotlin/" in path
    }

    private companion object {
        val privateConstructorDataClassRegex = Regex("data\\s+class\\s+[^\\n{(]+private\\s+constructor")
    }
}
