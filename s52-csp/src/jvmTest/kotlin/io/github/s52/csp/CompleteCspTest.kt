package io.github.s52.csp

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.preslib.PresLibPack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompleteCspTest {
    private val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)

    @Test
    fun generatedPackHasNoMissingCspReferences() {
        val report = CspCoverageValidator.validate(
            PresLibPack.synthetic().lookupTable,
            DefaultCspRegistry.complete()
        )

        assertFalse(report.hasErrors, report.toMarkdown())
        assertEquals(CspId.completeNames(), report.implemented)
        assertTrue(report.implemented.containsAll(report.referenced))
    }

    @Test
    fun everyPhase6CspHasAGoldenInstructionTranscript() {
        val settings = MarinerSettings(
            displayCategory = DisplayCategory.Other,
            safetyDepthMeters = 5.0,
            safetyContourMeters = 10.0,
            showText = true,
            showSoundings = true,
            showLightDescriptions = true
        )

        val transcripts = CspId.entries.associate { id ->
            id.s52Name to id.procedure.evaluate(featureFor(id), settings, context).map(::instructionTranscript)
        }

        assertEquals(listOf("AC(DEPVS)"), transcripts.getValue("DEPARE"))
        assertEquals(listOf("LS(SOLD,2.0,DEPSC)"), transcripts.getValue("DEPCNT"))
        assertEquals(listOf("TX(3.2,3.2,SNDG2)"), transcripts.getValue("SOUNDG"))
        assertEquals(listOf("SY(WRECKS_DANGER01)"), transcripts.getValue("WRECKS"))
        assertEquals(listOf("SY(OBSTRN_DANGER01)"), transcripts.getValue("OBSTRN"))
        assertTrue(transcripts.getValue("LIGHTS").contains("SY(LIGHTS11)"))
        assertEquals(listOf("SY(TOPMAR_CONE_UP01)"), transcripts.getValue("TOPMAR"))
        assertTrue(transcripts.getValue("RESARE").contains("AP(RESTRN01)"))
        assertTrue(transcripts.getValue("M_QUAL").contains("AP(MQUAL_LOW01)"))
        assertTrue(transcripts.getValue("DATCVR").contains("AP(NODATA01)"))
        assertTrue(transcripts.getValue("DRGARE").contains("AP(DRGARE01)"))
        assertTrue(transcripts.getValue("SBDARE").contains("AP(SBDARE01)"))
        assertTrue(transcripts.getValue("ACHARE").contains("AP(APACHR01)"))
        assertTrue(transcripts.getValue("FAIRWY").contains("AP(FAIRWY01)"))
    }

    @Test
    fun engineExpandsNewCspRowsIntoDrawCommands() {
        val pack = PresLibPack.synthetic()
        val engine = S52PortrayalEngine(pack.lookupTable, DefaultCspRegistry.complete())
        val commands = engine.portray(
            listOf(
                areaFeature(S57ObjectClass.RESARE, S57Attributes.of(S57Attribute.OBJNAM to S57Value.Text("No anchoring"))),
                areaFeature(S57ObjectClass.M_QUAL, S57Attributes.of(S57Attribute.CATZOC to S57Value.Integer(5))),
                areaFeature(S57ObjectClass.M_COVR, S57Attributes.of(S57Attribute.CATCOV to S57Value.Integer(2)))
            ),
            MarinerSettings(displayCategory = DisplayCategory.Other, showText = true),
            context
        )

        assertTrue(commands.any { it is S52DrawCommand.AreaPattern && it.patternName == "RESTRN01" })
        assertTrue(commands.any { it is S52DrawCommand.AreaPattern && it.patternName == "MQUAL_LOW01" })
        assertTrue(commands.any { it is S52DrawCommand.AreaFill && it.colorToken == "NODTA" })
        assertTrue(commands.any { it is S52DrawCommand.Text && it.textExpression == "No anchoring" })
    }

    private fun featureFor(id: CspId): EncFeature = when (id.s52Name) {
        "DEPARE" -> areaFeature(S57ObjectClass.DEPARE, S57Attributes.of(S57Attribute.DRVAL1 to S57Value.Decimal(0.0), S57Attribute.DRVAL2 to S57Value.Decimal(4.0)))
        "DEPCNT" -> lineFeature(S57ObjectClass.DEPCNT, S57Attributes.of(S57Attribute.VALDCO to S57Value.Decimal(10.0)))
        "SOUNDG" -> pointFeature(S57ObjectClass.SOUNDG, S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.2)))
        "WRECKS" -> pointFeature(S57ObjectClass.WRECKS, S57Attributes.of(S57Attribute.CATWRK to S57Value.Integer(2)))
        "OBSTRN" -> pointFeature(S57ObjectClass.OBSTRN, S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(2.0)))
        "LIGHTS" -> pointFeature(S57ObjectClass.LIGHTS, S57Attributes.of(S57Attribute.OBJNAM to S57Value.Text("Main"), S57Attribute.SECTR1 to S57Value.Decimal(20.0), S57Attribute.SECTR2 to S57Value.Decimal(110.0)))
        "TOPMAR" -> pointFeature(S57ObjectClass.TOPMAR, S57Attributes.of(S57Attribute.TOPSHP to S57Value.Integer(1)))
        "ACHARE" -> areaFeature(S57ObjectClass.ACHARE, S57Attributes.of(S57Attribute.OBJNAM to S57Value.Text("Anchorage")))
        "RESARE" -> areaFeature(S57ObjectClass.RESARE, S57Attributes.of(S57Attribute.RESTRN to S57Value.ListValue(listOf(S57Value.Integer(1), S57Value.Integer(7)))))
        "PRCARE" -> areaFeature(S57ObjectClass.PRCARE, S57Attributes.of(S57Attribute.OBJNAM to S57Value.Text("Precaution")))
        "TESARE" -> areaFeature(S57ObjectClass.TESARE, S57Attributes.of(S57Attribute.OBJNAM to S57Value.Text("Tidal stream")))
        "FAIRWY" -> areaFeature(S57ObjectClass.FAIRWY, S57Attributes.of(S57Attribute.OBJNAM to S57Value.Text("Fairway")))
        "DRGARE" -> areaFeature(S57ObjectClass.DRGARE, S57Attributes.of(S57Attribute.DRVAL1 to S57Value.Decimal(8.0), S57Attribute.DRVAL2 to S57Value.Decimal(12.0)))
        "SBDARE" -> areaFeature(S57ObjectClass.SBDARE, S57Attributes.of(S57Attribute.NATCON to S57Value.ListValue(listOf(S57Value.Integer(4), S57Value.Integer(5)))))
        "M_QUAL" -> areaFeature(S57ObjectClass.M_QUAL, S57Attributes.of(S57Attribute.CATZOC to S57Value.Integer(5)))
        "DATCVR" -> areaFeature(S57ObjectClass.M_COVR, S57Attributes.of(S57Attribute.CATCOV to S57Value.Integer(2)))
        else -> error("Unhandled test CSP ${id.s52Name}")
    }

    private fun instructionTranscript(instruction: S52Instruction): String = when (instruction) {
        is S52Instruction.AreaColor -> "AC(${instruction.colorToken})"
        is S52Instruction.AreaPattern -> "AP(${instruction.name})"
        is S52Instruction.ComplexLine -> "LC(${instruction.name})"
        is S52Instruction.SimpleLine -> "LS(${instruction.style},${instruction.width},${instruction.colorToken})"
        is S52Instruction.Symbol -> "SY(${instruction.name})"
        is S52Instruction.Text -> "${instruction.kind.token}(${listOf(instruction.textExpression).plus(instruction.rawArgs).joinToString(",")})"
        is S52Instruction.Conditional -> "CS(${instruction.cspName})"
    }

    private fun pointFeature(objectClass: S57ObjectClass, attributes: S57Attributes = S57Attributes.Empty): EncFeature =
        EncFeature(
            id = objectClass.ordinal.toLong() + 1L,
            objectClass = objectClass,
            primitive = PrimitiveType.Point,
            attributes = attributes,
            geometry = EncGeometry.Point(Coordinate(0.0, 0.0))
        )

    private fun lineFeature(objectClass: S57ObjectClass, attributes: S57Attributes = S57Attributes.Empty): EncFeature =
        EncFeature(
            id = objectClass.ordinal.toLong() + 10L,
            objectClass = objectClass,
            primitive = PrimitiveType.Line,
            attributes = attributes,
            geometry = EncGeometry.LineString(listOf(Coordinate(0.0, 0.0), Coordinate(1.0, 1.0)))
        )

    private fun areaFeature(objectClass: S57ObjectClass, attributes: S57Attributes = S57Attributes.Empty): EncFeature =
        EncFeature(
            id = objectClass.ordinal.toLong() + 20L,
            objectClass = objectClass,
            primitive = PrimitiveType.Area,
            attributes = attributes,
            geometry = EncGeometry.Polygon(listOf(Coordinate(0.0, 0.0), Coordinate(1.0, 0.0), Coordinate(1.0, 1.0), Coordinate(0.0, 0.0)))
        )
}
