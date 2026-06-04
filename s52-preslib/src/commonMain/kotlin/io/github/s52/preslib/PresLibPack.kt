package io.github.s52.preslib

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.instruction.InstructionParser
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupTable
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette

data class PresLibPack(
    val lookupTable: LookupTable,
    val colors: ColorTables,
    val symbols: SymbolRegistry,
    val lineStyles: LineStyleRegistry,
    val patterns: PatternRegistry
) {
    companion object {
        fun phase0Minimal(): PresLibPack {
            val dayColors = listOf(
                S52Color("DEPVS", 167, 206, 250),
                S52Color("DEPIT", 201, 226, 255),
                S52Color("DEPMS", 214, 236, 255),
                S52Color("DEPDW", 230, 245, 255),
                S52Color("LANDA", 201, 179, 123),
                S52Color("CHBLK", 0, 0, 0),
                S52Color("CHGRD", 110, 110, 110),
                S52Color("SNDG1", 0, 0, 0),
                S52Color("SNDG2", 30, 30, 30)
            ).associateBy { it.token }

            val allPalettes = S52Palette.entries.associateWith { dayColors }

            return PresLibPack(
                lookupTable = LookupTable(
                    listOf(
                        LookupRecord(
                            objectClass = S57ObjectClass.LNDARE,
                            primitive = PrimitiveType.Area,
                            instructions = InstructionParser.parseSequence("AC(LANDA)"),
                            displayCategory = DisplayCategory.DisplayBase,
                            viewingGroup = 11050,
                            displayPriority = 1
                        ),
                        LookupRecord(
                            objectClass = S57ObjectClass.DEPARE,
                            primitive = PrimitiveType.Area,
                            instructions = InstructionParser.parseSequence("CS(DEPARE)"),
                            displayCategory = DisplayCategory.Standard,
                            viewingGroup = 21010,
                            displayPriority = 2
                        ),
                        LookupRecord(
                            objectClass = S57ObjectClass.DEPCNT,
                            primitive = PrimitiveType.Line,
                            instructions = InstructionParser.parseSequence("LS(SOLD,1,CHGRD)"),
                            displayCategory = DisplayCategory.Standard,
                            viewingGroup = 21020,
                            displayPriority = 3
                        ),
                        LookupRecord(
                            objectClass = S57ObjectClass.BOYLAT,
                            primitive = PrimitiveType.Point,
                            instructions = InstructionParser.parseSequence("SY(BOYLAT01)"),
                            displayCategory = DisplayCategory.Standard,
                            viewingGroup = 27010,
                            displayPriority = 8,
                            overRadar = true
                        )
                    )
                ),
                colors = ColorTables(allPalettes),
                symbols = SymbolRegistry(
                    mapOf(
                        "BOYLAT01" to SymbolDefinition(
                            name = "BOYLAT01",
                            pivotX = 8.0,
                            pivotY = 8.0,
                            width = 16.0,
                            height = 16.0,
                            commands = listOf(
                                VectorCommand.MoveTo(8.0, 0.0),
                                VectorCommand.LineTo(16.0, 16.0),
                                VectorCommand.LineTo(0.0, 16.0),
                                VectorCommand.ClosePath
                            )
                        )
                    )
                ),
                lineStyles = LineStyleRegistry(
                    mapOf("SOLD" to LineStyleDefinition("SOLD", "solid line"))
                ),
                patterns = PatternRegistry(emptyMap())
            )
        }
    }
}
