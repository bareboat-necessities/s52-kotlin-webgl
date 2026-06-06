package io.github.s52.core

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57AttributeKey
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.lookup.AttributeFilter
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupRejectionReason
import io.github.s52.core.lookup.LookupTable
import io.github.s52.core.lookup.OpenCpnAttribCodeRuntimeParser
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.BoundaryStyle
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.core.settings.SymbolStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LookupPhase29OpenCpnTest {
    @Test
    fun filtersPointRowsByOpenCpnSymbolStyleTableName() {
        val table = LookupTable(
            listOf(
                pointLookup("Simplified", "SIMPLIFIED_SYMBOL"),
                pointLookup("Paper", "PAPER_SYMBOL")
            )
        )
        val feature = pointFeature()

        val simplified = table.match(feature, settings(symbolStyle = SymbolStyle.Simplified), context())
        val paper = table.match(feature, settings(symbolStyle = SymbolStyle.PaperChart), context())

        assertEquals(listOf("SIMPLIFIED_SYMBOL"), simplified.map { (it.instructions.single() as S52Instruction.Symbol).name })
        assertEquals(listOf("PAPER_SYMBOL"), paper.map { (it.instructions.single() as S52Instruction.Symbol).name })
    }

    @Test
    fun filtersAreaRowsByOpenCpnBoundaryStyleTableName() {
        val table = LookupTable(
            listOf(
                areaLookup("Plain", "PLAIN_AREA"),
                areaLookup("Symbolized", "SYMBOLIZED_AREA")
            )
        )
        val feature = areaFeature()

        val plain = table.match(feature, settings(boundaryStyle = BoundaryStyle.Plain), context())
        val symbolized = table.match(feature, settings(boundaryStyle = BoundaryStyle.Symbolized), context())

        assertEquals(listOf("PLAIN_AREA"), plain.map { (it.instructions.single() as S52Instruction.AreaColor).colorToken })
        assertEquals(listOf("SYMBOLIZED_AREA"), symbolized.map { (it.instructions.single() as S52Instruction.AreaColor).colorToken })
    }

    @Test
    fun parsesOpenCpnAttribCodesAgainstDynamicAttributeKeys() {
        val filter = OpenCpnAttribCodeRuntimeParser.parseAll(listOf("COLOUR3,1", "fnctnm5", "DRVAL1?"))
        val attrs = S57Attributes.ofKeyMap(
            mapOf(
                S57AttributeKey.of("COLOUR") to S57Value.ListValue(listOf(S57Value.Integer(3))),
                S57AttributeKey.of("FUNCTN") to S57Value.Integer(5),
                S57AttributeKey.of("DRVAL1") to S57Value.Decimal(0.0)
            )
        )

        assertTrue(filter.matches(pointFeature(attrs)))
        assertFalse(filter.matches(pointFeature(S57Attributes.ofKeyMap(mapOf(S57AttributeKey.of("COLOUR") to S57Value.Integer(2))))))
    }

    @Test
    fun explainReportsPresentationTableRejectionsSeparately() {
        val table = LookupTable(listOf(pointLookup("Paper", "PAPER_SYMBOL")))
        val explanation = table.explain(pointFeature(), settings(symbolStyle = SymbolStyle.Simplified), context())

        assertEquals(1, explanation.candidateCount)
        assertEquals(emptyList(), explanation.matches)
        assertEquals(LookupRejectionReason.PresentationTable, explanation.rejected.single().reason)
    }

    private fun pointLookup(tableName: String, symbolName: String): LookupRecord = LookupRecord(
        objectClass = S57ObjectClass.BOYLAT,
        primitive = PrimitiveType.Point,
        instructions = listOf(S52Instruction.Symbol(symbolName)),
        displayCategory = DisplayCategory.Standard,
        viewingGroup = 17010,
        displayPriority = 9,
        sourceTableName = tableName
    )

    private fun areaLookup(tableName: String, colorToken: String): LookupRecord = LookupRecord(
        objectClass = S57ObjectClass.DEPARE,
        primitive = PrimitiveType.Area,
        instructions = listOf(S52Instruction.AreaColor(colorToken)),
        displayCategory = DisplayCategory.Standard,
        viewingGroup = 11000,
        displayPriority = 3,
        sourceTableName = tableName
    )

    private fun pointFeature(attrs: S57Attributes = S57Attributes.Empty): EncFeature = EncFeature(
        id = 1L,
        objectClass = S57ObjectClass.BOYLAT,
        primitive = PrimitiveType.Point,
        attributes = attrs,
        geometry = EncGeometry.Point(Coordinate(-74.0, 40.0))
    )

    private fun areaFeature(): EncFeature = EncFeature(
        id = 2L,
        objectClass = S57ObjectClass.DEPARE,
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

    private fun settings(
        symbolStyle: SymbolStyle = SymbolStyle.Simplified,
        boundaryStyle: BoundaryStyle = BoundaryStyle.Plain
    ): MarinerSettings = MarinerSettings(symbolStyle = symbolStyle, boundaryStyle = boundaryStyle)

    private fun context(): PortrayalContext = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
}
