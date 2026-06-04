package io.github.s52.api

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S52ApiPhase12Test {
    @Test
    fun defaultRuntimePortraysSyntheticFeature() {
        val runtime = S52.defaultRuntime()
        val result = runtime.portrayValidated(listOf(depthArea()))

        assertTrue(result.isValid)
        assertEquals(1, result.commands.size)
        assertEquals("0.14.0-SNAPSHOT", S52.version.toString())
    }

    @Test
    fun transcriptIsDeterministicThroughFacade() {
        val runtime = S52.defaultRuntime()
        val settings = S52.defaultSettings(safetyContourMeters = 6.0, safetyDepthMeters = 6.0)
        val context = S52.defaultContext(settings, viewportId = "phase12-test")

        val first = runtime.transcript(listOf(depthArea(), sounding()), settings, context)
        val second = runtime.transcript(listOf(depthArea(), sounding()), settings, context)

        assertEquals(first, second)
        assertTrue(first.contains("area-fill"))
        assertTrue(first.contains("sounding"))
    }

    @Test
    fun lookupExplanationIsAvailableFromRuntime() {
        val explanation = S52.defaultRuntime().explainLookup(depthArea())

        assertTrue(explanation.candidateCount >= 1)
        assertFalse(explanation.matches.isEmpty())
    }

    private fun depthArea(): EncFeature = EncFeature(
        id = 1,
        objectClass = S57ObjectClass.DEPARE,
        primitive = PrimitiveType.Area,
        attributes = S57Attributes.of(
            S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
            S57Attribute.DRVAL2 to S57Value.Decimal(4.0)
        ),
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.0, 40.0),
                Coordinate(-73.9, 40.0),
                Coordinate(-73.9, 40.1),
                Coordinate(-74.0, 40.1),
                Coordinate(-74.0, 40.0)
            )
        )
    )

    private fun sounding(): EncFeature = EncFeature(
        id = 2,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(4.2)),
        geometry = EncGeometry.Point(Coordinate(-73.95, 40.05))
    )
}
