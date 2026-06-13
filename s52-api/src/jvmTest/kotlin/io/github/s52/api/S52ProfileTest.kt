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
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class S52ProfileTest {
    @Test
    fun catalogHasStableUniqueIds() {
        val ids = S52ProfileCatalog.all.map { it.id }

        assertEquals(ids.distinct(), ids)
        assertEquals(S52ProfileCatalog.safetyDay, S52ProfileCatalog.require("safety-day"))
        assertEquals(S52ProfileCatalog.nightMinimal, S52ProfileCatalog.fromPreset(S52ProfilePreset.NightMinimal))
        assertNotNull(S52ProfileCatalog.find("diagnostics-all"))
        assertTrue(S52ProfileCatalog.markdownCatalog().contains("safety-day"))
    }

    @Test
    fun profileSummaryAndPropertiesAreStable() {
        val profile = S52ProfileCatalog.safetyDay
        val summary = profile.summary()

        assertEquals("safety-day", summary.id)
        assertEquals(DisplayCategory.Standard, summary.displayCategory)
        assertEquals(S52Palette.DayBright, summary.palette)
        assertTrue(summary.toMarkdown().contains("Safety contour"))
        assertTrue(profile.toProperties().contains("palette=DayBright"))
        assertTrue(profile.toProperties().contains("enabledViewingGroups="))
    }

    @Test
    fun sessionPortraysUsingProfileConvenienceFunction() {
        val session = S52.synthetic()
        val result = session.portray(
            features = listOf(depthArea(), sounding()),
            profile = S52ProfileCatalog.safetyDay
        )

        assertFalse(result.hasErrors, result.diagnosticsMarkdown())
        assertTrue(result.commands.any { it.kind == DrawCommandKind.AreaFill })
        assertTrue(result.commands.any { it.kind == DrawCommandKind.Sounding })
    }

    @Test
    fun nightMinimalProfileSuppressesSoundingsAndText() {
        val session = S52.synthetic()
        val result = session.portray(
            features = listOf(depthArea(), sounding()),
            profile = S52ProfileCatalog.nightMinimal
        )

        assertFalse(result.hasErrors, result.diagnosticsMarkdown())
        assertTrue(result.commands.any { it.kind == DrawCommandKind.AreaFill })
        assertTrue(result.commands.none { it.kind == DrawCommandKind.Sounding })
        assertTrue(result.commands.none { it.kind == DrawCommandKind.Text })
    }

    @Test
    fun diagnosticBundleConvenienceUsesProfile() {
        val bundle = S52.synthetic().diagnosticBundle(
            features = listOf(depthArea(), sounding()),
            profile = S52ProfileCatalog.diagnosticsAll,
            name = "-profile-test",
            transcriptPreviewLineLimit = 2
        )

        assertFalse(bundle.hasErrors, bundle.toMarkdown())
        assertEquals("-profile-test", bundle.manifest.name)
        assertEquals(2, bundle.featureCount)
        assertTrue(bundle.toProperties().contains("commandKind."))
    }

    private fun depthArea(): EncFeature = EncFeature(
        id = 18_001,
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
        id = 18_002,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.2)),
        geometry = EncGeometry.Point(Coordinate(-73.995, 40.005, 3.2))
    )
}
