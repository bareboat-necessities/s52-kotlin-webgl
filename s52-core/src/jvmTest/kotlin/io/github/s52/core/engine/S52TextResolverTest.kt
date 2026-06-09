package io.github.s52.core.engine

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.MapCspRegistry
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.InstructionKind
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

class S52TextResolverTest {
    @Test
    fun resolvesObjectNameExpressionToFeatureLabel() {
        val command = portraySingleText(
            feature = feature(
                id = 1,
                objectClass = S57ObjectClass.BOYLAT,
                primitive = PrimitiveType.Point,
                geometry = EncGeometry.Point(Coordinate(-73.0, 40.0)),
                attributes = S57Attributes.of(S57Attribute.OBJNAM to S57Value.Text("North Channel Buoy"))
            ),
            expression = "OBJNAM",
            args = listOf("OBJNAM", "CHBLK")
        ) as S52DrawCommand.Text

        assertEquals("North Channel Buoy", command.textExpression)
        assertEquals("CHBLK", command.colorToken)
    }

    @Test
    fun resolvesNationalNameBeforeLeavingRawExpression() {
        val command = portraySingleText(
            feature = feature(
                id = 2,
                objectClass = S57ObjectClass.BOYCAR,
                primitive = PrimitiveType.Point,
                geometry = EncGeometry.Point(Coordinate(-73.0, 40.0)),
                attributes = S57Attributes.of(S57Attribute.NOBJNM to S57Value.Text("Boya Norte"))
            ),
            expression = "NOBJNM",
            args = listOf("NOBJNM", "CHBLK")
        ) as S52DrawCommand.Text

        assertEquals("Boya Norte", command.textExpression)
    }

    @Test
    fun resolvesSoundingDepthFromValsouInsteadOfExpressionToken() {
        val command = portraySingleText(
            feature = feature(
                id = 3,
                objectClass = S57ObjectClass.SOUNDG,
                primitive = PrimitiveType.Point,
                geometry = EncGeometry.Point(Coordinate(-73.0, 40.0, 12.74)),
                attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(12.74))
            ),
            expression = "VALSOU",
            args = listOf("VALSOU", "SNDG2")
        ) as S52DrawCommand.Sounding

        assertEquals("12.7", command.depthLabel)
        assertEquals("SNDG2", command.colorToken)
    }

    @Test
    fun resolvesLightDescriptionFromLightAttributes() {
        val command = portraySingleText(
            feature = feature(
                id = 4,
                objectClass = S57ObjectClass.LIGHTS,
                primitive = PrimitiveType.Point,
                geometry = EncGeometry.Point(Coordinate(-73.0, 40.0)),
                attributes = S57Attributes.of(
                    S57Attribute.OBJNAM to S57Value.Text("Fl R 4s"),
                    S57Attribute.LITCHR to S57Value.Integer(2),
                    S57Attribute.SIGGRP to S57Value.Text("(2)"),
                    S57Attribute.SIGPER to S57Value.Decimal(4.0),
                    S57Attribute.SECTR1 to S57Value.Decimal(15.0),
                    S57Attribute.SECTR2 to S57Value.Decimal(120.0)
                )
            ),
            expression = "LIGHTS",
            args = listOf("LIGHTS", "LITYW")
        ) as S52DrawCommand.Text

        assertEquals("Fl R 4s LITCHR=2 SIGGRP=(2) 4s 15-120°", command.textExpression)
        assertEquals("LITYW", command.colorToken)
    }

    private fun portraySingleText(
        feature: EncFeature,
        expression: String,
        args: List<String>
    ): S52DrawCommand {
        val engine = S52PortrayalEngine(
            LookupTable(
                listOf(
                    LookupRecord(
                        objectClass = feature.objectClass,
                        primitive = feature.primitive,
                        instructions = listOf(S52Instruction.Text(expression, args, InstructionKind.TX)),
                        displayCategory = DisplayCategory.Standard,
                        viewingGroup = 1,
                        displayPriority = 1
                    )
                )
            ),
            MapCspRegistry(emptyList())
        )
        return engine.portray(
            features = listOf(feature),
            settings = MarinerSettings(),
            context = PortrayalContext()
        ).single()
    }

    private fun feature(
        id: Long,
        objectClass: S57ObjectClass,
        primitive: PrimitiveType,
        geometry: EncGeometry,
        attributes: S57Attributes
    ): EncFeature = EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = primitive,
        attributes = attributes,
        geometry = geometry
    )
}
