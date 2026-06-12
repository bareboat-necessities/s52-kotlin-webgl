package io.github.s52.preslib.esri.csp

import kotlin.test.Test
import kotlin.test.assertTrue

class EsriChart1FallbackCspsTest {
    @Test
    fun chartOneBuoyUsesLateralShapeAttributes() {
        val instructions = EsriCspRegistry.apply(
            "chart1_buoy",
            EsriCspFeature("BOYLAT", primitive = 1, attributes = mapOf("CATLAM" to "2")),
            EsriPortrayalContext()
        )
        assertTrue(instructions.any { it == EsriInstruction.Symbol("Q20b_Conical_buoy.svg", null) })
    }

    @Test
    fun chartOneSafetyContourUsesHeavyLineAndLabel() {
        val instructions = EsriCspRegistry.apply(
            "chart1_depth_contour",
            EsriCspFeature("DEPCNT", primitive = 2, attributes = mapOf("VALDCO" to "30")),
            EsriPortrayalContext(safetyContour = 30.0)
        )
        assertTrue(instructions.any { it is EsriInstruction.SimpleLine && it.width >= 0.7 })
        assertTrue(instructions.any { it is EsriInstruction.Text && it.text == "30" })
    }

    @Test
    fun chartOneLandmarkUsesCategorySpecificSymbol() {
        val instructions = EsriCspRegistry.apply(
            "chart1_landmark",
            EsriCspFeature("LNDMRK", primitive = 1, attributes = mapOf("CATLMK" to "5", "CONVIS" to "1")),
            EsriPortrayalContext()
        )
        assertTrue(instructions.any { it == EsriInstruction.Symbol("E22_Chimney.svg", null) })
    }

    @Test
    fun chartOneUnderwaterHazardCanEmitIsolatedDanger() {
        val instructions = EsriCspRegistry.apply(
            "chart1_underwater_hazard",
            EsriCspFeature("OBSTRN", primitive = 1, attributes = mapOf("VALSOU" to "4"), leastDepth = 4.0, greatestDepth = 40.0),
            EsriPortrayalContext(safetyDepth = 30.0, safetyContour = 30.0)
        )
        assertTrue(instructions.any { it == EsriInstruction.Symbol("ISODGR01", null) })
    }
}
