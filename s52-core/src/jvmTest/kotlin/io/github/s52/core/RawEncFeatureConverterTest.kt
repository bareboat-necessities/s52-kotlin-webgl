package io.github.s52.core

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57EnumeratedValue
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.FeatureConversionResult
import io.github.s52.core.model.RawEncFeature
import io.github.s52.core.model.RawEncFeatureConverter
import io.github.s52.core.model.S57Value
import io.github.s52.core.model.toTypedFeature
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RawEncFeatureConverterTest {
    @Test
    fun convertsRawFeatureToTypedFeatureWithAttributeEnums() {
        val raw = RawEncFeature(
            id = 10,
            objectClassAcronym = "DEPARE",
            objectClassCode = 42,
            primitive = PrimitiveType.Area,
            rawAttributes = mapOf(
                "DRVAL1" to S57Value.Decimal(0.0),
                "DRVAL2" to S57Value.Decimal(5.0)
            ),
            geometry = samplePolygon()
        )

        val feature = raw.toTypedFeature()

        assertEquals(S57ObjectClass.DEPARE, feature.objectClass)
        assertEquals(5.0, feature.attributes.double(S57Attribute.DRVAL2))
    }

    @Test
    fun rejectsUnsupportedPrimitiveForObjectClass() {
        val raw = RawEncFeature(
            id = 11,
            objectClassAcronym = "DEPARE",
            primitive = PrimitiveType.Point,
            rawAttributes = emptyMap(),
            geometry = EncGeometry.Point(Coordinate(-74.0, 40.0))
        )

        val result = RawEncFeatureConverter.convert(raw)

        assertIs<FeatureConversionResult.Failure>(result)
        assertTrue(result.message.contains("does not support Point"))
    }

    @Test
    fun rejectsUnknownAttributesWithDiagnostics() {
        val raw = RawEncFeature(
            id = 12,
            objectClassAcronym = "LIGHTS",
            primitive = PrimitiveType.Point,
            rawAttributes = mapOf("NO_SUCH_ATTR" to S57Value.Integer(1)),
            geometry = EncGeometry.Point(Coordinate(-74.0, 40.0))
        )

        val result = RawEncFeatureConverter.convert(raw)

        assertIs<FeatureConversionResult.Failure>(result)
        assertTrue(result.message.contains("Unknown S-57 attribute"))
    }

    @Test
    fun exposesEnumeratedAttributeValuesFromTypedAttributes() {
        val raw = RawEncFeature(
            id = 13,
            objectClassAcronym = "WRECKS",
            primitive = PrimitiveType.Point,
            rawAttributes = mapOf("CATWRK" to S57Value.Integer(2)),
            geometry = EncGeometry.Point(Coordinate(-74.0, 40.0))
        )

        val feature = raw.toTypedFeature()

        assertEquals(
            S57EnumeratedValue.CATWRK_DANGEROUS,
            feature.attributes.enum(S57Attribute.CATWRK)
        )
    }

    private fun samplePolygon(): EncGeometry.Polygon = EncGeometry.Polygon(
        outer = listOf(
            Coordinate(-74.0, 40.0),
            Coordinate(-73.9, 40.0),
            Coordinate(-73.9, 40.1),
            Coordinate(-74.0, 40.0)
        )
    )
}
