package io.github.s52.preslib.source

internal fun String.s52Token(): String = trim().uppercase()

object PresLibSourceNormalizer {
    fun normalize(source: PresLibSourcePack): PresLibSourcePack {
        return source.copy(
            colorTables = source.colorTables.map { table ->
                table.copy(colors = table.colors.map { color -> color.copy(token = color.token.s52Token()) })
            },
            symbols = source.symbols.map { symbol -> symbol.copy(name = symbol.name.s52Token()) },
            lineStyles = source.lineStyles.map { style -> style.copy(name = style.name.s52Token()) },
            patterns = source.patterns.map { pattern -> pattern.copy(name = pattern.name.s52Token()) },
            lookupRecords = source.lookupRecords.map { record ->
                record.copy(instruction = normalizeInstructionText(record.instruction))
            }
        )
    }

    private fun normalizeInstructionText(source: String): String =
        source.trim()
            .split(';')
            .joinToString(";") { it.trim() }
            .trim(';')
}
