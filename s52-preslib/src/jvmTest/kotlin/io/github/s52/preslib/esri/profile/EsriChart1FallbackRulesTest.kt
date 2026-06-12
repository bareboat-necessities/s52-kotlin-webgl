package io.github.s52.preslib.esri.profile

import io.github.s52.preslib.esri.rules.EsriRuleAction
import io.github.s52.preslib.esri.rules.EsriRuleFeature
import kotlin.test.Test
import kotlin.test.assertTrue

class EsriChart1FallbackRulesTest {
    @Test
    fun fallbackRulesAddChartOneCoverageWhenGeneratedRulesAreEmpty() {
        assertTrue(EsriInt1Profile.directRules.size >= 30)
        assertFunction("BOYLAT", 1, "chart1_buoy")
        assertFunction("BCNLAT", 1, "chart1_beacon")
        assertFunction("LNDMRK", 1, "chart1_landmark")
        assertFunction("DEPCNT", 2, "chart1_depth_contour")
        assertFunction("SLCONS", 2, "chart1_shore_construction")
        assertFunction("UWTROC", 1, "chart1_underwater_hazard")
        assertFunction("NAVLNE", 2, "chart1_routes")
    }

    private fun assertFunction(objectAcronym: String, primitive: Int, function: String) {
        val actions = EsriInt1Profile.directRuleActions(
            EsriRuleFeature(objectAcronym = objectAcronym, primitive = primitive)
        )
        assertTrue(actions.any { action -> action is EsriRuleAction.Function && function in action.names })
    }
}
