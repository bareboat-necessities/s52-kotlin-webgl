package io.github.s52.api

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.MarinerSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S52PortrayalSessionTest {
    @Test
    fun defaultSyntheticSessionIsStaticallyComplete() {
        val session = S52.synthetic()
        val manifest = session.manifest()

        assertTrue(manifest.staticallyComplete, manifest.toMarkdown())
        assertTrue(manifest.lookupRecords > 0)
        assertTrue(manifest.implementedCsps == manifest.referencedCsps)
        assertTrue(manifest.safetyStatus.contains("not for navigation"))
    }

    @Test
    fun facadePortraysFeaturesAndReturnsStableTranscript() {
        val session = S52.synthetic()
        val result = session.portray(
            S52PortrayalRequest(
                features = listOf(depthArea(), sounding()),
                settings = MarinerSettings(
                    safetyDepthMeters = 5.0,
                    safetyContourMeters = 10.0,
                    shallowContourMeters = 2.0,
                    deepContourMeters = 30.0,
                    showSoundings = true
                )
            )
        )

        assertFalse(result.hasErrors, result.diagnosticsMarkdown())
        assertTrue(result.commands.isNotEmpty())
        assertTrue(result.transcript.contains("\"kind\":\"area-fill\""), result.transcript)
        assertTrue(result.transcript.contains("\"kind\":\"sounding\""), result.transcript)
    }

    private fun depthArea(): EncFeature = EncFeature(
        id = 16_001,
        objectClass = S57ObjectClass.DEPARE,
        primitive = PrimitiveType.Area,
        attributes = S57Attributes.of(
            S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
            S57Attribute.DRVAL2 to S57Value.Decimal(4.0)
        ),
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.0, 40.0),
                Coordinate(-73.99, 40.0),
                Coordinate(-73.99, 40.01),
                Coordinate(-74.0, 40.0)
            )
        )
    )

    private fun sounding(): EncFeature = EncFeature(
        id = 16_002,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.2)),
        geometry = EncGeometry.Point(Coordinate(-73.995, 40.005, 3.2))
    )
}
