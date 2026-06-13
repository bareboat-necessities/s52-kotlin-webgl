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
import kotlin.test.assertTrue

class S52DocumentationTest {


    @Test
    fun minimalDocumentedApiPathProducesValidatedCommands() {
        val feature = EncFeature(
            id = 1401,
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

        val runtime = S52.defaultRuntime()
        val settings = S52.defaultSettings(safetyContourMeters = 6.0)
        val result = runtime.portrayValidated(listOf(feature), settings, S52.defaultContext(settings))

        assertTrue(result.isValid)
        assertTrue(result.commands.isNotEmpty())
        assertTrue(runtime.transcript(listOf(feature), settings, S52.defaultContext(settings)).contains("area-fill"))
    }
}
