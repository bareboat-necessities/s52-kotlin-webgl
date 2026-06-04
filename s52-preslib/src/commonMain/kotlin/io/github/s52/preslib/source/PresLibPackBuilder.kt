package io.github.s52.preslib.source

import io.github.s52.core.instruction.InstructionParser
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupTable
import io.github.s52.preslib.ColorTables
import io.github.s52.preslib.LineStyleDefinition
import io.github.s52.preslib.LineStyleRegistry
import io.github.s52.preslib.PatternDefinition
import io.github.s52.preslib.PatternRegistry
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.S52Color
import io.github.s52.preslib.SymbolDefinition
import io.github.s52.preslib.SymbolRegistry
import io.github.s52.preslib.VectorCommand

object PresLibPackBuilder {
    fun build(source: PresLibSourcePack): PresLibPack {
        val normalized = PresLibSourceNormalizer.normalize(source)
        return PresLibPack(
            lookupTable = LookupTable(normalized.lookupRecords.map { it.toLookupRecord() }),
            colors = ColorTables(
                normalized.colorTables.associate { table ->
                    table.palette to table.colors.associate { color ->
                        color.token to S52Color(color.token, color.r, color.g, color.b)
                    }
                }
            ),
            symbols = SymbolRegistry(
                normalized.symbols.associate { symbol -> symbol.name to symbol.toSymbolDefinition() }
            ),
            lineStyles = LineStyleRegistry(
                normalized.lineStyles.associate { style -> style.name to LineStyleDefinition(style.name, style.description) }
            ),
            patterns = PatternRegistry(
                normalized.patterns.associate { pattern -> pattern.name to PatternDefinition(pattern.name, pattern.description) }
            )
        )
    }

    private fun SourceLookupRecord.toLookupRecord(): LookupRecord = LookupRecord(
        objectClass = objectClass,
        primitive = primitive,
        instructions = InstructionParser.parseSequence(instruction),
        displayCategory = displayCategory,
        viewingGroup = viewingGroup,
        displayPriority = displayPriority,
        overRadar = overRadar
    )

    private fun SourceSymbol.toSymbolDefinition(): SymbolDefinition = SymbolDefinition(
        name = name,
        pivotX = pivotX,
        pivotY = pivotY,
        width = width,
        height = height,
        commands = commands.map { it.toVectorCommand() }
    )

    private fun SourceVectorCommand.toVectorCommand(): VectorCommand = when (this) {
        is SourceVectorCommand.MoveTo -> VectorCommand.MoveTo(x, y)
        is SourceVectorCommand.LineTo -> VectorCommand.LineTo(x, y)
        SourceVectorCommand.ClosePath -> VectorCommand.ClosePath
    }
}
