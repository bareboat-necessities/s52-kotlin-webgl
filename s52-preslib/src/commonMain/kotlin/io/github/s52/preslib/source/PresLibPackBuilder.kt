package io.github.s52.preslib.source

import io.github.s52.core.instruction.InstructionParser
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.lookup.AttributeFilter
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupTable
import io.github.s52.core.lookup.OpenCpnAttribCodeRuntimeParser
import io.github.s52.preslib.ColorTables
import io.github.s52.preslib.LineStyleDefinition
import io.github.s52.preslib.LineStyleRegistry
import io.github.s52.preslib.PatternDefinition
import io.github.s52.preslib.PatternRegistry
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.RasterBitmapDefinition
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
            symbols = SymbolRegistry.fromDefinitions(
                normalized.symbols.map { symbol -> symbol.toSymbolDefinition() }
            ),
            lineStyles = LineStyleRegistry(
                normalized.lineStyles.associate { style ->
                    style.name to LineStyleDefinition(
                        name = style.name,
                        description = style.description,
                        pivotX = style.pivotX,
                        pivotY = style.pivotY,
                        width = style.width,
                        height = style.height,
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
                        pivotX = pattern.pivotX,
                        pivotY = pattern.pivotY,
                        width = pattern.width,
                        height = pattern.height,
                        colorRefs = pattern.colorRefs,
                        bitmap = pattern.bitmap?.toRasterBitmapDefinition(),
                        vectorHpgl = pattern.vectorHpgl
                    )
                }
            )
        )
    }

    private fun SourceLookupRecord.toLookupRecord(): LookupRecord = LookupRecord(
        /*
         * The starter S57ObjectClass enum only covers a subset of the full
         * OpenCPN lookup table semantics. Some OpenCPN rows use a valid object
         * acronym with a primitive not listed on the enum entry. In those cases
         * objectClassKey is still the authoritative OpenCPN class key, so avoid
         * tripping LookupRecord's enum compatibility guard.
         */
        objectClass = objectClass?.takeIf { it.supports(primitive) },
        objectClassKey = objectClassKey,
        primitive = primitive,
        attributeFilter = combinedRuntimeFilter(),
        instructions = parseInstructionsLenient(instruction),
        displayCategory = displayCategory,
        viewingGroup = viewingGroup,
        displayPriority = displayPriority,
        overRadar = overRadar,
        minimumDisplayScale = minimumDisplayScale,
        maximumDisplayScale = maximumDisplayScale,
        sourceTableName = sourceTableName,
        sourceDisplayPriorityLabel = sourceDisplayPriorityLabel,
        sourceRadarPriority = sourceRadarPriority
    )

    private fun SourceLookupRecord.combinedRuntimeFilter(): AttributeFilter {
        val explicit = attributeFilter.toRuntime()
        val openCpn = OpenCpnAttribCodeRuntimeParser.parseAll(rawAttribCodes)
        return when {
            explicit === AttributeFilter.Any -> openCpn
            openCpn === AttributeFilter.Any -> explicit
            else -> AttributeFilter.All(listOf(explicit, openCpn))
        }
    }

    private fun parseInstructionsLenient(source: String): List<S52Instruction> {
        if (source.isBlank()) return emptyList()
        return try {
            InstructionParser.parseSequence(source)
        } catch (_: IllegalArgumentException) {
            source.split(';')
                .mapNotNull { segment ->
                    val trimmed = segment.trim()
                    if (trimmed.isEmpty()) {
                        null
                    } else {
                        try {
                            InstructionParser.parseOne(trimmed)
                        } catch (_: IllegalArgumentException) {
                            null
                        }
                    }
                }
        }
    }

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
