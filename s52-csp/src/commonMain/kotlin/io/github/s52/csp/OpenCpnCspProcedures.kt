package io.github.s52.csp

import io.github.s52.catalog.PrimitiveType
import io.github.s52.core.csp.ConditionalSymbologyProcedure
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/** OpenCPN-numbered CSP registrations referenced by `chartsymbols.xml`. */
object OpenCpnCspProcedures {
    fun procedures(): List<ConditionalSymbologyProcedure> = listOf(
        alias("DATCVR01", DataCoverageCsp()),
        alias("DEPARE01", DepthAreaCsp()),
        alias("DEPARE02", DepthAreaCsp()),
        alias("DEPCNT02", DepthContourCsp()),
        alias("LIGHTS05", LightsCsp()),
        alias("OBSTRN04", ObstructionCsp()),
        alias("QUAPOS01", QualityOfDataCsp()),
        RestrictedAreaCsp("RESARE01", "RESTRN01"),
        RestrictedAreaCsp("RESARE02", "RESTRN01"),
        RestrictedAreaCsp("RESTRN01", "RESTRN01"),
        alias("SOUNDG02", SoundingCsp()),
        alias("TOPMAR01", TopmarkCsp()),
        alias("TOPMARI1", TopmarkCsp()),
        alias("WRECKS02", WrecksCsp()),
        simple("CLRLIN01") { listOf(S52Instruction.SimpleLine("DASH", 1.0, "CHMGD")) },
        simple("LEGLIN02") { listOf(S52Instruction.SimpleLine("DASH", 1.0, "CHMGD")) },
        simple("PASTRK01") { listOf(S52Instruction.SimpleLine("DASH", 1.0, "CHMGD")) },
        simple("VRMEBL01") { listOf(S52Instruction.SimpleLine("DASH", 1.0, "CHMGD")) },
        simple("OWNSHP02") { listOf(S52Instruction.Symbol("OWNSHP01")) },
        simple("VESSEL01") { listOf(S52Instruction.Symbol("VESSL001")) },
        simple("SLCONS03") { feature ->
            when (feature.primitive) {
                PrimitiveType.Point -> listOf(S52Instruction.Symbol("MORFAC03"))
                PrimitiveType.Line -> listOf(S52Instruction.SimpleLine("SOLID", 1.0, "CSTLN"))
                PrimitiveType.Area -> listOf(S52Instruction.AreaColor("CHBRN"), S52Instruction.SimpleLine("SOLID", 1.0, "CSTLN"))
                PrimitiveType.Collection -> emptyList()
            }
        },
        simple("SYMINS01") { listOf(S52Instruction.Symbol("QUESMRK1")) }
    )

    fun names(): Set<String> = procedures().mapTo(mutableSetOf()) { it.name.uppercase() }

    private fun alias(name: String, delegate: ConditionalSymbologyProcedure): ConditionalSymbologyProcedure =
        object : ConditionalSymbologyProcedure {
            override val name: String = name

            override fun evaluate(
                feature: EncFeature,
                settings: MarinerSettings,
                context: PortrayalContext
            ): List<S52Instruction> = delegate.evaluate(feature, settings, context)
        }

    private fun simple(
        name: String,
        body: (EncFeature) -> List<S52Instruction>
    ): ConditionalSymbologyProcedure = object : ConditionalSymbologyProcedure {
        override val name: String = name

        override fun evaluate(
            feature: EncFeature,
            settings: MarinerSettings,
            context: PortrayalContext
        ): List<S52Instruction> = body(feature)
    }
}
