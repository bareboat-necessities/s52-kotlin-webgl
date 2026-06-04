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
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.preslib.PresLibPack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Phase5CriticalCspTest {
    private val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)

    @Test
    fun registryContainsCriticalPhase5Procedures() {
        val registry = DefaultCspRegistry.phase5Critical()
        assertEquals(CspId.criticalPhase5Names(), registry.names())
        CspId.criticalPhase5Names().forEach { assertTrue(registry.has(it)) }
    }

    @Test
    fun generatedPackReferencesAllCriticalPhase5Procedures() {
        val pack = PresLibPack.phase2Synthetic()
        val report = CspCoverageValidator.validate(pack.lookupTable, DefaultCspRegistry.phase6Complete())
        assertFalse(report.hasErrors, report.toMarkdown())
        assertTrue(report.referenced.containsAll(CspId.criticalPhase5Names()))
    }

    @Test
    fun depthContourPromotesSafetyContour() {
        val instructions = DepthContourCsp().evaluate(
            lineFeature(S57ObjectClass.DEPCNT, S57Attributes.of(S57Attribute.VALDCO to S57Value.Decimal(10.0))),
            MarinerSettings(safetyContourMeters = 10.0),
            context
        )
        assertEquals(listOf(S52Instruction.SimpleLine("SOLD", 2.0, "DEPSC")), instructions)
    }

    @Test
    fun soundingHidesWhenSoundingsAreDisabled() {
        val feature = pointFeature(
            S57ObjectClass.SOUNDG,
            S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(4.2))
        )
        assertEquals(emptyList(), SoundingCsp().evaluate(feature, MarinerSettings(showSoundings = false), context))
    }

    @Test
    fun unsafeSoundingUsesUnsafeColorArgument() {
        val feature = pointFeature(
            S57ObjectClass.SOUNDG,
            S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(4.2))
        )
        val text = SoundingCsp().evaluate(feature, MarinerSettings(safetyDepthMeters = 5.0), context).single()
            as S52Instruction.Text
        assertEquals("4.2", text.textExpression)
        assertEquals("SNDG2", text.rawArgs[1])
    }

    @Test
    fun wrecksAndObstructionsSelectDangerSymbols() {
        val wreck = pointFeature(
            S57ObjectClass.WRECKS,
            S57Attributes.of(S57Attribute.CATWRK to S57Value.Integer(2))
        )
        val obstruction = pointFeature(
            S57ObjectClass.OBSTRN,
            S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(2.0))
        )

        assertEquals(listOf(S52Instruction.Symbol("WRECKS_DANGER01")), WrecksCsp().evaluate(wreck, MarinerSettings(), context))
        assertEquals(listOf(S52Instruction.Symbol("OBSTRN_DANGER01")), ObstructionCsp().evaluate(obstruction, MarinerSettings(safetyDepthMeters = 5.0), context))
    }

    @Test
    fun lightDescriptionsAreControlledByMarinerSetting() {
        val feature = pointFeature(
            S57ObjectClass.LIGHTS,
            S57Attributes.of(
                S57Attribute.OBJNAM to S57Value.Text("Main light"),
                S57Attribute.LITCHR to S57Value.Integer(2),
                S57Attribute.SECTR1 to S57Value.Decimal(20.0),
                S57Attribute.SECTR2 to S57Value.Decimal(110.0)
            )
        )
        val withText = LightsCsp().evaluate(feature, MarinerSettings(showLightDescriptions = true), context)
        val withoutText = LightsCsp().evaluate(feature, MarinerSettings(showLightDescriptions = false), context)

        assertTrue(withText.any { it is S52Instruction.Symbol && it.name == "LIGHTS11" })
        assertTrue(withText.any { it is S52Instruction.ComplexLine && it.name == "LIGHTSECTOR01" })
        assertTrue(withText.any { it is S52Instruction.Text })
        assertTrue(withoutText.none { it is S52Instruction.Text })
    }

    @Test
    fun topmarkSelectsShapeSymbol() {
        val feature = pointFeature(
            S57ObjectClass.TOPMAR,
            S57Attributes.of(S57Attribute.TOPSHP to S57Value.Integer(3))
        )
        assertEquals(listOf(S52Instruction.Symbol("TOPMAR_SPHERE01")), TopmarkCsp().evaluate(feature, MarinerSettings(), context))
    }

    @Test
    fun engineExpandsCriticalCspIntoDrawCommands() {
        val pack = PresLibPack.phase2Synthetic()
        val engine = S52PortrayalEngine(pack.lookupTable, DefaultCspRegistry.phase5Critical())
        val commands = engine.portray(
            listOf(
                pointFeature(S57ObjectClass.SOUNDG, S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.0))),
                pointFeature(S57ObjectClass.TOPMAR, S57Attributes.of(S57Attribute.TOPSHP to S57Value.Integer(1)))
            ),
            MarinerSettings(safetyDepthMeters = 5.0),
            context
        )

        assertTrue(commands.any { it is S52DrawCommand.Text && it.textExpression == "3" })
        assertTrue(commands.any { it is S52DrawCommand.PointSymbol && it.symbolName == "TOPMAR_CONE_UP01" })
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
}
