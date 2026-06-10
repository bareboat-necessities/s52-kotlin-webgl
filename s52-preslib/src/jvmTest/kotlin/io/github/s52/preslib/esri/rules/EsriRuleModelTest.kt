package io.github.s52.preslib.esri.rules

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EsriRuleModelTest {
    @Test
    fun attributeEqualMatchesNullLiteralAndValue() {
        val rule = EsriRuleFilter.Attribute("BCNSHP", EsriRuleOperator.EQUAL, listOf("1"))
        assertTrue(rule.matches(EsriRuleFeature("BCNLAT", 1, attributes = mapOf("BCNSHP" to listOf("1")))))
        assertFalse(rule.matches(EsriRuleFeature("BCNLAT", 1, attributes = mapOf("BCNSHP" to listOf("2")))))
    }

    @Test
    fun containsAllRequiresEveryExpectedListValue() {
        val rule = EsriRuleFilter.ListAttribute("COLOUR", EsriRuleOperator.CONTAINS_ALL, listOf("2", "4"))
        assertTrue(rule.matches(EsriRuleFeature("BOYLAT", 1, attributes = mapOf("COLOUR" to listOf("2", "4")))))
        assertFalse(rule.matches(EsriRuleFeature("BOYLAT", 1, attributes = mapOf("COLOUR" to listOf("2")))))
    }

    @Test
    fun portrayalRuleChecksObjectPrimitiveAndFilters() {
        val rule = EsriPortrayalRule(
            objects = listOf("BOYLAT"),
            primitive = 1,
            action = EsriRuleAction.Symbol("Q20b_Conical_buoy.svg"),
            filters = listOf(EsriRuleFilter.Attribute("BOYSHP", EsriRuleOperator.EQUAL, listOf("1")))
        )
        assertTrue(rule.matches(EsriRuleFeature("BOYLAT", 1, attributes = mapOf("BOYSHP" to listOf("1")))))
        assertFalse(rule.matches(EsriRuleFeature("BOYLAT", 2, attributes = mapOf("BOYSHP" to listOf("1")))))
    }
}
