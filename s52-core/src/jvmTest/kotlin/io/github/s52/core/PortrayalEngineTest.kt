package io.github.s52.core

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.csp.MapCspRegistry
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupTable
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PortrayalEngineTest {
    @Test
    fun convertsLookupInstructionsToDrawCommands() {
        val feature = EncFeature(
            id = 1,
            objectClass = S57ObjectClass.LNDARE,
            primitive = PrimitiveType.Area,
            attributes = S57Attributes.Empty,
            geometry = EncGeometry.Polygon(
                outer = listOf(
                    Coordinate(-74.0, 40.0),
                    Coordinate(-73.9, 40.0),
                    Coordinate(-73.9, 40.1),
                    Coordinate(-74.0, 40.0)
                )
            )
        )
        val table = LookupTable(
            listOf(
                LookupRecord(
                    objectClass = S57ObjectClass.LNDARE,
                    primitive = PrimitiveType.Area,
                    instructions = listOf(S52Instruction.AreaColor("LANDA")),
                    displayCategory = DisplayCategory.DisplayBase,
                    viewingGroup = 11050,
                    displayPriority = 2
                )
            )
        )

        val commands = S52PortrayalEngine(table).portray(
            features = listOf(feature),
            settings = MarinerSettings(),
            context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        )

        assertEquals(1, commands.size)
        assertEquals(1, commands.first().featureId)
    }

    @Test
    fun expandsConditionalSymbology() {
        val feature = EncFeature(
            id = 2,
            objectClass = S57ObjectClass.DEPARE,
            primitive = PrimitiveType.Area,
            attributes = S57Attributes.of(S57Attribute.DRVAL2 to S57Value.Decimal(1.0)),
            geometry = EncGeometry.Polygon(
                outer = listOf(
                    Coordinate(-74.0, 40.0),
                    Coordinate(-73.9, 40.0),
                    Coordinate(-73.9, 40.1),
                    Coordinate(-74.0, 40.0)
                )
            )
        )
        val table = LookupTable(
            listOf(
                LookupRecord(
                    objectClass = S57ObjectClass.DEPARE,
                    primitive = PrimitiveType.Area,
                    instructions = listOf(S52Instruction.Conditional("DEPARE")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 21010,
                    displayPriority = 1
                )
            )
        )
        val csp = object : ConditionalSymbologyProcedure {
            override val name: String = "DEPARE"

            override fun evaluate(
                feature: EncFeature,
                settings: MarinerSettings,
                context: PortrayalContext
            ): List<S52Instruction> = listOf(S52Instruction.AreaColor("DEPVS"))
        }

        val commands = S52PortrayalEngine(table, MapCspRegistry(listOf(csp))).portray(
            features = listOf(feature),
            settings = MarinerSettings(),
            context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        )

        assertEquals(1, commands.size)
        assertTrue(commands.first().toString().contains("DEPVS"))
    }
}
