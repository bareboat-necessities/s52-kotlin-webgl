package io.github.s52.core

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.draw.DrawCommandKind
import io.github.s52.core.draw.DrawCommandValidator
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.draw.S52DrawCommandTranscript
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupTable
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DrawCommandPhase7Test {
    private val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)

    @Test
    fun commandKindsAreStableAndValidationAcceptsS52Tokens() {
        val commands = portrayPhase7Fixture()
        val kinds = commands.map { it.kind }

        assertEquals(
            listOf(
                DrawCommandKind.AreaFill,
                DrawCommandKind.LineSimple,
                DrawCommandKind.PointSymbol,
                DrawCommandKind.Text,
                DrawCommandKind.Sounding
            ),
            kinds
        )
        assertFalse(DrawCommandValidator.validate(commands).hasErrors)
    }

    @Test
    fun soundgTextInstructionBecomesDedicatedSoundingCommand() {
        val commands = portrayPhase7Fixture()
        val sounding = commands.filterIsInstance<S52DrawCommand.Sounding>().single()

        assertEquals("3.2", sounding.depthLabel)
        assertEquals("SNDG2", sounding.colorToken)
        assertEquals(DrawCommandKind.Sounding, sounding.kind)
    }

    @Test
    fun commandTranscriptIsDeterministicAndRendererIndependent() {
        val commands = portrayPhase7Fixture()
        val transcript = S52DrawCommandTranscript.serialize(commands)

        assertTrue(transcript.contains("\"kind\":\"area-fill\""), transcript)
        assertTrue(transcript.contains("\"kind\":\"sounding\""), transcript)
        assertTrue(transcript.contains("\"geometry\":\"POINT(-74,40)\""), transcript)
        assertEquals(transcript, S52DrawCommandTranscript.serialize(commands))
    }

    @Test
    fun validatorReportsStringlyRendererLeakage() {
        val invalid = listOf(
            S52DrawCommand.AreaFill(
                featureId = 99,
                geometry = EncGeometry.Polygon(listOf(Coordinate(0.0, 0.0), Coordinate(1.0, 0.0), Coordinate(0.0, 0.0))),
                colorToken = "#ffffff",
                priority = 1,
                viewingGroup = 1,
                category = DisplayCategory.Standard,
                overRadar = false
            )
        )

        val report = DrawCommandValidator.validate(invalid)

        assertTrue(report.hasErrors)
        assertTrue(report.toMarkdown().contains("not a stable S-52 token"))
    }

    @Test
    fun textCommandCarriesTextKindAndOptionalColorToken() {
        val text = portrayPhase7Fixture().filterIsInstance<S52DrawCommand.Text>().single()

        assertEquals(InstructionKind.TE, text.textKind)
        assertEquals("CHBLK", text.colorToken)
        assertIs<S52DrawCommand.Text>(text)
    }

    private fun portrayPhase7Fixture(): List<S52DrawCommand> {
        val features = listOf(
            areaFeature(),
            lineFeature(),
            pointFeature(S57ObjectClass.BOYLAT, 3),
            pointFeature(S57ObjectClass.LIGHTS, 4),
            pointFeature(S57ObjectClass.SOUNDG, 5)
        )
        val table = LookupTable(
            listOf(
                LookupRecord(
                    objectClass = S57ObjectClass.DEPARE,
                    primitive = PrimitiveType.Area,
                    instructions = listOf(S52Instruction.AreaColor("DEPVS")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 21010,
                    displayPriority = 1
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.DEPCNT,
                    primitive = PrimitiveType.Line,
                    instructions = listOf(S52Instruction.SimpleLine("SOLD", 2.0, "DEPSC")),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 22010,
                    displayPriority = 2
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.BOYLAT,
                    primitive = PrimitiveType.Point,
                    instructions = listOf(S52Instruction.Symbol("BOYLAT01", parameters = listOf("45"))),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 27010,
                    displayPriority = 3
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.LIGHTS,
                    primitive = PrimitiveType.Point,
                    instructions = listOf(S52Instruction.Text("Main", listOf("Main", "CHBLK"), InstructionKind.TE)),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 28010,
                    displayPriority = 4
                ),
                LookupRecord(
                    objectClass = S57ObjectClass.SOUNDG,
                    primitive = PrimitiveType.Point,
                    instructions = listOf(S52Instruction.Text("3.2", listOf("3.2", "SNDG2"), InstructionKind.TX)),
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 33010,
                    displayPriority = 5
                )
            )
        )

        return S52PortrayalEngine(table).portray(features, MarinerSettings(), context)
    }

    private fun pointFeature(objectClass: S57ObjectClass, id: Long): EncFeature = EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.Empty,
        geometry = EncGeometry.Point(Coordinate(-74.0, 40.0))
    )

    private fun lineFeature(): EncFeature = EncFeature(
        id = 2,
        objectClass = S57ObjectClass.DEPCNT,
        primitive = PrimitiveType.Line,
        attributes = S57Attributes.Empty,
        geometry = EncGeometry.LineString(listOf(Coordinate(-74.0, 40.0), Coordinate(-73.9, 40.1)))
    )

    private fun areaFeature(): EncFeature = EncFeature(
        id = 1,
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
}
