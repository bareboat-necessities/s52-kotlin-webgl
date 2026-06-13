package io.github.s52.api

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.draw.DrawCommandKind
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.MarinerSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class S52DiagnosticBundleTest {
    @Test
    fun diagnosticBundleSummarizesManifestCommandsAndTranscript() {
        val session = S52.synthetic()
        val request = S52PortrayalRequest(
            features = listOf(depthArea(), sounding()),
            settings = MarinerSettings(
                safetyDepthMeters = 5.0,
                safetyContourMeters = 10.0,
                shallowContourMeters = 2.0,
                deepContourMeters = 30.0,
                showSoundings = true
            )
        )

        val bundle = session.diagnosticBundle(
            request = request,
            name = "-test",
            transcriptPreviewLineLimit = 1
        )

        assertFalse(bundle.hasErrors, bundle.toMarkdown())
        assertEquals("-test", bundle.manifest.name)
        assertEquals(2, bundle.featureCount)
        assertTrue(bundle.commandCount >= 2)
        assertTrue((bundle.commandCountsByKind[DrawCommandKind.AreaFill] ?: 0) >= 1)
        assertTrue((bundle.commandCountsByKind[DrawCommandKind.Sounding] ?: 0) >= 1)
        assertTrue(bundle.toMarkdown().contains("# S-52 Diagnostic Bundle"))
        assertTrue(bundle.toMarkdown().contains("Not for navigation") || bundle.toMarkdown().contains("not for navigation"))
        assertTrue(bundle.toProperties().contains("commandKind.sounding="))
        assertTrue(bundle.transcriptPreview().lines().size <= 2)
    }

    private fun depthArea(): EncFeature = EncFeature(
        id = 17_001,
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
        id = 17_002,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.2)),
        geometry = EncGeometry.Point(Coordinate(-73.995, 40.005, 3.2))
    )
}
