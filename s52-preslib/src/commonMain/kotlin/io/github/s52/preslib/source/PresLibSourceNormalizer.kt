package io.github.s52.preslib.source

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.catalog.S57ObjectClassKey
import io.github.s52.catalog.toKey
import io.github.s52.core.settings.DisplayCategory

internal fun String.s52Token(): String = trim().uppercase()

object PresLibSourceNormalizer {
    fun normalize(source: PresLibSourcePack): PresLibSourcePack {
        val normalizedLookupRecords = source.lookupRecords.map { record ->
            record.copy(instruction = normalizeInstructionText(record.instruction))
        }
        return source.copy(
            colorTables = source.colorTables.map { table ->
                table.copy(colors = table.colors.map { color -> color.copy(token = color.token.s52Token()) })
            },
            symbols = source.symbols.map { symbol -> symbol.copy(name = symbol.name.s52Token()) },
            lineStyles = source.lineStyles.map { style -> style.copy(name = style.name.s52Token()) },
            patterns = source.patterns.map { pattern -> pattern.copy(name = pattern.name.s52Token()) },
            lookupRecords = normalizedLookupRecords + openCpnCompatibilityLookupRecords(source, normalizedLookupRecords)
        )
    }

    private fun normalizeInstructionText(source: String): String =
        source.trim()
            .split(';')
            .joinToString(";") { it.trim() }
            .trim(';')

    /**
     * Small generated-pack compatibility layer for ENC metadata seen in real NOAA
     * client logs but absent from OpenCPN's raw lookup table. These rows are kept
     * outside the checked-in `chartsymbols.xml` payload so the generator can still
     * be rerun without mutating vendor source data.
     */
    private fun openCpnCompatibilityLookupRecords(
        source: PresLibSourcePack,
        normalizedLookupRecords: List<SourceLookupRecord>
    ): List<SourceLookupRecord> {
        if (!source.metadata.name.contains("OpenCPN", ignoreCase = true) &&
            !source.metadata.generatedBy.contains("OpenCpn", ignoreCase = true)
        ) {
            return emptyList()
        }

        val rows = mutableListOf<SourceLookupRecord>()
        if (!normalizedLookupRecords.hasLookup("ACHARE", PrimitiveType.Line)) {
            rows += normalizedLookupRecords
                .firstOrNull { it.objectClassKey.acronym == "ACHARE" && it.primitive == PrimitiveType.Area }
                ?.copy(
                    objectClass = S57ObjectClass.ACHARE,
                    objectClassKey = S57ObjectClass.ACHARE.toKey(),
                    primitive = PrimitiveType.Line,
                    instruction = "LC(ACHARE51)",
                    displayCategory = DisplayCategory.Standard,
                    sourceTableName = "Lines",
                    sourceDisplayPriorityLabel = "Line Symbol",
                    rawAttribCodes = emptyList()
                )
                ?: SourceLookupRecord(
                    objectClass = S57ObjectClass.ACHARE,
                    primitive = PrimitiveType.Line,
                    instruction = "LC(ACHARE51)",
                    displayCategory = DisplayCategory.Standard,
                    viewingGroup = 26220,
                    displayPriority = 4,
                    sourceTableName = "Lines",
                    sourceDisplayPriorityLabel = "Line Symbol",
                    sourceRadarPriority = "SUPPRESSED"
                )
        }

        if (!normalizedLookupRecords.hasLookup("OBJL_0", PrimitiveType.Line)) {
            rows += SourceLookupRecord(
                objectClass = S57ObjectClass.OBJL_0,
                primitive = PrimitiveType.Line,
                instruction = "",
                displayCategory = DisplayCategory.Other,
                viewingGroup = 0,
                displayPriority = 0,
                sourceTableName = "Lines",
                sourceDisplayPriorityLabel = "No data",
                sourceRadarPriority = "SUPPRESSED"
            )
        }

        return rows
    }

    private fun List<SourceLookupRecord>.hasLookup(acronym: String, primitive: PrimitiveType): Boolean =
        any { it.objectClassKey == S57ObjectClassKey.of(acronym) && it.primitive == primitive }
}
