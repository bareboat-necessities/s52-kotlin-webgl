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
import io.github.s52.preslib.RasterBitmapDefinition

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
                normalized.lineStyles.associate { style ->
                    style.name to LineStyleDefinition(
                        name = style.name,
                        description = style.description,
                        colorRefs = style.colorRefs,
                        bitmap = style.bitmap?.toRasterBitmapDefinition(),
                        vectorHpgl = style.vectorHpgl
                    )
                }
            ),
            patterns = PatternRegistry(
                normalized.patterns.associate { pattern ->
                    pattern.name to PatternDefinition(
                        name = pattern.name,
                        description = pattern.description,
                        colorRefs = pattern.colorRefs,
                        bitmap = pattern.bitmap?.toRasterBitmapDefinition(),
                        vectorHpgl = pattern.vectorHpgl
                    )
                }
            )
        )
    }

    private fun SourceLookupRecord.toLookupRecord(): LookupRecord = LookupRecord(
        objectClass = objectClass,
        objectClassKey = objectClassKey,
        primitive = primitive,
        attributeFilter = attributeFilter.toRuntime(),
        instructions = InstructionParser.parseSequence(instruction),
        displayCategory = displayCategory,
        viewingGroup = viewingGroup,
        displayPriority = displayPriority,
        overRadar = overRadar,
        minimumDisplayScale = minimumDisplayScale,
        maximumDisplayScale = maximumDisplayScale
    )

    private fun SourceSymbol.toSymbolDefinition(): SymbolDefinition = SymbolDefinition(
        name = name,
        pivotX = pivotX,
        pivotY = pivotY,
        width = width,
        height = height,
        commands = commands.map { it.toVectorCommand() },
        colorRefs = colorRefs,
        bitmap = bitmap?.toRasterBitmapDefinition(),
        vectorHpgl = vectorHpgl
    )

    private fun SourceBitmapRef.toRasterBitmapDefinition(): RasterBitmapDefinition = RasterBitmapDefinition(
        atlasFileName = atlasFileName,
        x = x,
        y = y,
        width = width,
        height = height,
        pivotX = pivotX,
        pivotY = pivotY,
        originX = originX,
        originY = originY
    )

    private fun SourceVectorCommand.toVectorCommand(): VectorCommand = when (this) {
        is SourceVectorCommand.MoveTo -> VectorCommand.MoveTo(x, y)
        is SourceVectorCommand.LineTo -> VectorCommand.LineTo(x, y)
        SourceVectorCommand.ClosePath -> VectorCommand.ClosePath
    }
}
