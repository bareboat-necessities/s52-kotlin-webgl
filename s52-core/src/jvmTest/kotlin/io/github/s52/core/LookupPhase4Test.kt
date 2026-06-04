package io.github.s52.core

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.lookup.AttributeFilter
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupRejectionReason
import io.github.s52.core.lookup.LookupTable
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LookupPhase4Test {
    private val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)

    @Test
    fun lookupTableIndexesByObjectAndPrimitiveAndRanksSpecificFilters() {
        val feature = pointFeature(
            objectClass = S57ObjectClass.WRECKS,
            attributes = S57Attributes.of(S57Attribute.CATWRK to S57Value.Integer(1))
        )
        val table = LookupTable(
            listOf(
                LookupRecord(
                    objectClass = S57ObjectClass.WRECKS,
                    primitive = PrimitiveType.Point,
                    instructions = listOf(S52Instruction.Symbol("WRECKS_GENERIC")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 34050,
                    displayPriority = 8
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.WRECKS,
                    primitive = PrimitiveType.Point,
                    attributeFilter = AttributeFilter.EqualsInt(S57Attribute.CATWRK, 1),
                    instructions = listOf(S52Instruction.Symbol("WRECKS_SPECIFIC")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 34050,
                    displayPriority = 8
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.WRECKS,
                    primitive = PrimitiveType.Area,
                    instructions = listOf(S52Instruction.AreaColor("CHBLK")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 34050,
                    displayPriority = 8
                )
            )
        )

        val matches = table.matchDetailed(feature, MarinerSettings(), context)

        assertEquals(2, matches.size)
        assertEquals("WRECKS_SPECIFIC", (matches[0].record.instructions.single() as S52Instruction.Symbol).name)
        assertEquals("WRECKS_GENERIC", (matches[1].record.instructions.single() as S52Instruction.Symbol).name)
        assertEquals(2, table.candidates(S57ObjectClass.WRECKS, PrimitiveType.Point).size)
    }

    @Test
    fun lookupExplanationReportsScaleAndAttributeRejections() {
        val feature = pointFeature(
            objectClass = S57ObjectClass.BOYLAT,
            attributes = S57Attributes.Empty,
            scaleMin = 20_000
        )
        val table = LookupTable(
            listOf(
                LookupRecord(
                    objectClass = S57ObjectClass.BOYLAT,
                    primitive = PrimitiveType.Point,
                    instructions = listOf(S52Instruction.Symbol("BOYLAT01")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 27010,
                    displayPriority = 8
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.BOYLAT,
                    primitive = PrimitiveType.Point,
                    attributeFilter = AttributeFilter.Exists(S57Attribute.COLOUR),
                    instructions = listOf(S52Instruction.Symbol("BOYLAT_COLOURED")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 27010,
                    displayPriority = 8
                )
            )
        )

        val explanation = table.explain(
            feature,
            MarinerSettings(),
            PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        )

        assertEquals(2, explanation.candidateCount)
        assertEquals(0, explanation.matches.size)
        assertTrue(explanation.rejected.all { it.reason == LookupRejectionReason.Scale })
    }

    @Test
    fun displayCategoryViewingGroupAndPriorityFiltersAreAppliedAfterPortrayal() {
        val features = listOf(
            pointFeature(S57ObjectClass.BOYLAT, id = 1),
            pointFeature(S57ObjectClass.LIGHTS, id = 2),
            areaFeature(S57ObjectClass.LNDARE, id = 3)
        )
        val table = LookupTable(
            listOf(
                LookupRecord(
                    objectClass = S57ObjectClass.BOYLAT,
                    primitive = PrimitiveType.Point,
                    instructions = listOf(S52Instruction.Symbol("BOYLAT01")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 27010,
                    displayPriority = 8
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.LIGHTS,
                    primitive = PrimitiveType.Point,
                    instructions = listOf(S52Instruction.Symbol("LIGHTS11")),
                    displayCategory = DisplayCategory.Other,
                    viewingGroup = 28010,
                    displayPriority = 9
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.LNDARE,
                    primitive = PrimitiveType.Area,
                    instructions = listOf(S52Instruction.AreaColor("LANDA")),
                    displayCategory = DisplayCategory.DisplayBase,
                    viewingGroup = 11050,
                    displayPriority = 1
                )
            )
        )

        val commands = S52PortrayalEngine(table).portray(
            features = features,
            settings = MarinerSettings(
                displayCategory = DisplayCategory.Other,
                disabledViewingGroups = setOf(28010)
            ),
            context = context
        )

        assertEquals(listOf(3L, 1L), commands.map { it.featureId })
        assertIs<io.github.s52.core.draw.S52DrawCommand.AreaFill>(commands[0])
        assertIs<io.github.s52.core.draw.S52DrawCommand.PointSymbol>(commands[1])
    }

    @Test
    fun listAndCompositeAttributeFiltersMatchTypedValues() {
        val feature = pointFeature(
            objectClass = S57ObjectClass.BOYLAT,
            attributes = S57Attributes.of(
                S57Attribute.COLOUR to S57Value.ListValue(
                    listOf(S57Value.Integer(1), S57Value.Integer(3))
                ),
                S57Attribute.OBJNAM to S57Value.Text("Preferred Channel")
            )
        )
        val filter = AttributeFilter.All(
            listOf(
                AttributeFilter.IntIn(S57Attribute.COLOUR, setOf(3, 4)),
                AttributeFilter.TextEquals(S57Attribute.OBJNAM, "preferred channel", ignoreCase = true)
            )
        )

        assertTrue(filter.matches(feature))
    }

    private fun pointFeature(
        objectClass: S57ObjectClass,
        id: Long = 1,
        attributes: S57Attributes = S57Attributes.Empty,
        scaleMin: Int? = null,
        scaleMax: Int? = null
    ): EncFeature = EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = PrimitiveType.Point,
        attributes = attributes,
        geometry = EncGeometry.Point(Coordinate(-74.0, 40.0)),
        scaleMin = scaleMin,
        scaleMax = scaleMax
    )

    private fun areaFeature(
        objectClass: S57ObjectClass,
        id: Long = 1,
        attributes: S57Attributes = S57Attributes.Empty
    ): EncFeature = EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = PrimitiveType.Area,
        attributes = attributes,
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.0, 40.0),
                Coordinate(-73.9, 40.0),
                Coordinate(-73.9, 40.1),
                Coordinate(-74.0, 40.0)
            )
        )
    )
}
