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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class S52ArtifactBundleTest {
    @Test
    fun artifactBundleExportsStableNamedFiles() {
        val bundle = S52.synthetic().artifactBundle(
            features = listOf(depthArea(), sounding()),
            profile = S52ProfileCatalog.safetyDay,
            name = "phase19-artifact-test"
        )

        assertFalse(bundle.hasErrors, bundle.toMarkdownIndex())
        assertNotNull(bundle.find("bundle-index.md"))
        assertNotNull(bundle.find("manifest.md"))
        assertNotNull(bundle.find("diagnostics.md"))
        assertNotNull(bundle.find("diagnostics.properties"))
        assertNotNull(bundle.find("profile.md"))
        assertNotNull(bundle.find("profile.properties"))
        assertNotNull(bundle.find("commands.jsonl"))
        assertTrue(bundle.require("commands.jsonl").text.contains("AreaFill"))
        assertTrue(bundle.require("commands.jsonl").text.contains("Sounding"))
        assertTrue(bundle.toPropertiesIndex().contains("artifactCount="))
    }

    @Test
    fun compactExportKeepsDiagnosticsAndPreviewButCanSkipFullTranscript() {
        val bundle = S52.synthetic().artifactBundle(
            features = listOf(depthArea(), sounding()),
            profile = S52ProfileCatalog.safetyDay,
            name = "phase19-compact-test",
            options = S52ArtifactExportOptions.compact
        )

        assertFalse(bundle.hasErrors, bundle.toMarkdownIndex())
        assertNotNull(bundle.find("diagnostics.properties"))
        assertNotNull(bundle.find("commands-preview.txt"))
        assertEquals(null, bundle.find("commands.jsonl"))
        assertEquals(null, bundle.find("static-completeness.md"))
        assertEquals(null, bundle.find("command-validation.md"))
    }

    @Test
    fun artifactBundleRejectsUnsafePathsAndDuplicatePaths() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            S52Artifact("../bad.txt", "text/plain", "bad")
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            S52ArtifactBundle(
                name = "bad",
                artifacts = listOf(
                    S52Artifact("a.txt", "text/plain", "1"),
                    S52Artifact("a.txt", "text/plain", "2")
                )
            )
        }
    }

    @Test
    fun artifactPreviewIsBounded() {
        val artifact = S52Artifact(
            path = "preview.txt",
            mediaType = "text/plain",
            text = listOf("a", "b", "c").joinToString("\n")
        )

        assertEquals("a\nb\n... 1 more line(s) omitted ...", artifact.preview(maxLines = 2))
    }

    private fun depthArea(): EncFeature = EncFeature(
        id = 19_001,
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
        id = 19_002,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.2)),
        geometry = EncGeometry.Point(Coordinate(-73.995, 40.005, 3.2))
    )
}
