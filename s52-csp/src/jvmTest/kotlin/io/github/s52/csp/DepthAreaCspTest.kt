package io.github.s52.csp

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import kotlin.test.Test
import kotlin.test.assertEquals

class DepthAreaCspTest {
    @Test
    fun shallowAreaUsesVeryShallowColor() {
        val feature = EncFeature(
            id = 10,
            objectClass = S57ObjectClass.DEPARE,
            primitive = PrimitiveType.Area,
            attributes = S57Attributes.of(
                S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
                S57Attribute.DRVAL2 to S57Value.Decimal(1.5)
            ),
            geometry = EncGeometry.Polygon(
                listOf(
                    Coordinate(0.0, 0.0),
                    Coordinate(1.0, 0.0),
                    Coordinate(1.0, 1.0),
                    Coordinate(0.0, 0.0)
                )
            )
        )

        val result = DepthAreaCsp().evaluate(
            feature,
            MarinerSettings(safetyContourMeters = 5.0),
            PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        )

        assertEquals(listOf(S52Instruction.AreaColor("DEPVS")), result)
    }
}
