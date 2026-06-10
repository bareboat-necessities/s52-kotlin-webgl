package io.github.s52.preslib.esri.csp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EsriHighPriorityCspsTest {
    @Test
    fun wrecksPointWithSafeDepthEmitsInDepthSymbolAndSounding() {
        val instructions = EsriCspRegistry.apply(
            "wrecks05",
            EsriCspFeature("WRECKS", primitive = 1, attributes = mapOf("VALSOU" to "35")),
            EsriPortrayalContext(safetyDepth = 30.0, safetyContour = 30.0)
        )
        assertTrue(instructions.any { it == EsriInstruction.Symbol("K1_Obstruction4mm_InDepthRangeWk.svg", null) })
        assertTrue(instructions.any { it is EsriInstruction.Sounding && it.depth == 35.0 })
    }

    @Test
    fun wrecksAreaWithNoDepthEmitsLineAndFill() {
        val instructions = EsriCspRegistry.apply(
            "wrecks05",
            EsriCspFeature("WRECKS", primitive = 3, attributes = mapOf("WATLEV" to "3")),
            EsriPortrayalContext()
        )
        assertTrue(instructions.any { it is EsriInstruction.SimpleLine })
        assertTrue(instructions.any { it is EsriInstruction.AreaFill })
    }

    @Test
    fun redLightEmitsRedLightSymbol() {
        val instructions = EsriCspRegistry.apply(
            "lights",
            EsriCspFeature("LIGHTS", primitive = 1, listAttributes = mapOf("COLOUR" to listOf("3"))),
            EsriPortrayalContext()
        )
        assertTrue(instructions.any { it == EsriInstruction.Symbol("P1_Light_red.svg", null) })
    }

    @Test
    fun registryContainsExpectedAliases() {
        assertTrue("wrecks05" in EsriCspRegistry.names)
        assertTrue("depare03" in EsriCspRegistry.names)
        assertTrue("cblohd02" in EsriCspRegistry.names)
    }
}
