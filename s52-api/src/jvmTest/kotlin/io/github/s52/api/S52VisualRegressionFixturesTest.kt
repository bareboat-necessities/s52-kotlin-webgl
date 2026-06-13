package io.github.s52.api

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.preslib.PresLibPack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class S52VisualRegressionFixturesTest {
    @Test
    fun phase33FixtureKeepsConcavePolygonWithHole() {
        val commands = S52VisualRegressionFixtures.phase33Commands(includeLabels = false)
        val patterned = commands.filterIsInstance<S52DrawCommand.AreaPattern>()
        val concave = patterned.first().geometry as EncGeometry.Polygon

        assertTrue(concave.outer.size >= 8)
        assertEquals(1, concave.holes.size)
        assertTrue(concave.holes.first().size >= 4)
    }

    @Test
    fun phase33FixtureReferencesRenderableOpenCpnAssets() {
        val pack = PresLibPack.openCpn()
        S52VisualRegressionFixtures.requiredSymbolNames.forEach { name ->
            assertNotNull(pack.symbols.find(name), "missing symbol $name")
        }
        S52VisualRegressionFixtures.requiredPatternNames.forEach { name ->
            assertNotNull(pack.patterns.find(name), "missing pattern $name")
        }
    }

    @Test
    fun phase33FixtureIncludesFailureModeSymbolsAndHpglPatterns() {
        val commands = S52VisualRegressionFixtures.phase33Commands(includeLabels = false)
        val symbols = commands.filterIsInstance<S52DrawCommand.PointSymbol>().map { it.symbolName }.toSet()
        val patterns = commands.filterIsInstance<S52DrawCommand.AreaPattern>().map { it.patternName }.toSet()

        assertTrue(S52VisualRegressionFixtures.requiredSymbolNames.all { it in symbols })
        assertTrue(S52VisualRegressionFixtures.requiredPatternNames.all { it in patterns })
    }
}
