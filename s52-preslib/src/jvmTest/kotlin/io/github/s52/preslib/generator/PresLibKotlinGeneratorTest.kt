package io.github.s52.preslib.generator

import io.github.s52.preslib.generated.GeneratedPresLib
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresLibKotlinGeneratorTest {
    @Test
    fun generatorIsDeterministic() {
        val source = GeneratedPresLib.sourcePack()
        val first = PresLibKotlinGenerator.generate(source, "io.github.s52.generated", "GeneratedPack")
        val second = PresLibKotlinGenerator.generate(source, "io.github.s52.generated", "GeneratedPack")

        assertEquals(first, second)
        assertTrue(first.contains("object GeneratedPack"))
        assertTrue(first.contains("SourceLookupRecord"))
        assertTrue(first.contains("BOYLAT01"))
    }
}
