package io.github.s52.tests.golden

import io.github.s52.api.S52
import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.BoundaryStyle
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.core.settings.SymbolStyle
import kotlin.test.Test
import kotlin.test.assertTrue

class OpenCpnGoldenPhase32Test {
    @Test
    fun representativeOpenCpnFeaturesProduceStableCommands() {
        val runtime = S52.openCpnRuntime()
        val settings = MarinerSettings(
            displayCategory = DisplayCategory.Other,
            symbolStyle = SymbolStyle.Simplified,
            boundaryStyle = BoundaryStyle.Plain
        )
        val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        val commands = runtime.portray(representativeFeatures(), settings, context)

        assertTrue(commands.isNotEmpty())
        assertTrue(commands.any { it is S52DrawCommand.AreaFill })
        assertTrue(commands.any { it is S52DrawCommand.LineSimple || it is S52DrawCommand.LineComplex })
        assertTrue(commands.any { it is S52DrawCommand.PointSymbol || it is S52DrawCommand.Text || it is S52DrawCommand.Sounding })
    }

    private fun representativeFeatures(): List<EncFeature> = listOf(
        EncFeature(
            id = 1L,
            objectClass = S57ObjectClass.LNDARE,
            primitive = PrimitiveType.Area,
            attributes = S57Attributes.Empty,
            geometry = EncGeometry.Polygon(
                listOf(
                    Coordinate(-74.10, 40.12),
                    Coordinate(-73.78, 40.12),
                    Coordinate(-73.78, 40.20),
                    Coordinate(-74.10, 40.20),
                    Coordinate(-74.10, 40.12)
                )
            )
        ),
        EncFeature(
            id = 2L,
            objectClass = S57ObjectClass.DEPCNT,
            primitive = PrimitiveType.Line,
            attributes = S57Attributes.of(S57Attribute.VALDCO to S57Value.Decimal(10.0)),
            geometry = EncGeometry.LineString(
                listOf(Coordinate(-74.08, 40.02), Coordinate(-73.96, 40.06), Coordinate(-73.82, 40.04))
            )
        ),
        EncFeature(
            id = 3L,
            objectClass = S57ObjectClass.SOUNDG,
            primitive = PrimitiveType.Point,
            attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(6.4)),
            geometry = EncGeometry.Point(Coordinate(-74.02, 40.00))
        )
    )
}
