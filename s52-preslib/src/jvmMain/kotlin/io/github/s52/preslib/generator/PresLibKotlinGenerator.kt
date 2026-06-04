package io.github.s52.preslib.generator

import io.github.s52.preslib.source.PresLibSourceNormalizer
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceAttributeFilter
import io.github.s52.preslib.source.SourceVectorCommand
import java.util.Locale

/** JVM-only deterministic Kotlin source generator for Phase 2 source packs. */
object PresLibKotlinGenerator {
    fun generate(
        source: PresLibSourcePack,
        packageName: String,
        objectName: String
    ): String {
        require(packageName.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*"))) {
            "Invalid Kotlin package name: $packageName"
        }
        require(objectName.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) {
            "Invalid Kotlin object name: $objectName"
        }
        val normalized = PresLibSourceNormalizer.normalize(source)
        return buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("import io.github.s52.catalog.PrimitiveType")
            appendLine("import io.github.s52.catalog.S57Attribute")
            appendLine("import io.github.s52.catalog.S57ObjectClass")
            appendLine("import io.github.s52.core.settings.DisplayCategory")
            appendLine("import io.github.s52.core.settings.S52Palette")
            appendLine("import io.github.s52.preslib.source.PresLibMetadata")
            appendLine("import io.github.s52.preslib.source.PresLibSourcePack")
            appendLine("import io.github.s52.preslib.source.SourceColor")
            appendLine("import io.github.s52.preslib.source.SourceColorTable")
            appendLine("import io.github.s52.preslib.source.SourceLineStyle")
            appendLine("import io.github.s52.preslib.source.SourceAttributeFilter")
            appendLine("import io.github.s52.preslib.source.SourceLookupRecord")
            appendLine("import io.github.s52.preslib.source.SourcePattern")
            appendLine("import io.github.s52.preslib.source.SourceSymbol")
            appendLine("import io.github.s52.preslib.source.SourceVectorCommand")
            appendLine()
            appendLine("object $objectName {")
            appendLine("    fun sourcePack(): PresLibSourcePack = PresLibSourcePack(")
            appendLine("        metadata = PresLibMetadata(")
            appendLine("            name = ${normalized.metadata.name.kt()},")
            appendLine("            edition = ${normalized.metadata.edition.kt()},")
            appendLine("            sourceDescription = ${normalized.metadata.sourceDescription.kt()},")
            appendLine("            generatedBy = ${normalized.metadata.generatedBy.kt()}")
            appendLine("        ),")
            appendLine("        colorTables = listOf(")
            normalized.colorTables.sortedBy { it.palette.name }.forEachIndexed { index, table ->
                appendLine("            SourceColorTable(")
                appendLine("                palette = S52Palette.${table.palette.name},")
                appendLine("                colors = listOf(")
                table.colors.sortedBy { it.token }.forEachIndexed { colorIndex, color ->
                    append("                    SourceColor(${color.token.kt()}, ${color.r}, ${color.g}, ${color.b})")
                    appendLine(if (colorIndex == table.colors.lastIndex) "" else ",")
                }
                appendLine("                )")
                append("            )")
                appendLine(if (index == normalized.colorTables.lastIndex) "" else ",")
            }
            appendLine("        ),")
            appendLine("        symbols = listOf(")
            normalized.symbols.sortedBy { it.name }.forEachIndexed { index, symbol ->
                appendLine("            SourceSymbol(")
                appendLine("                name = ${symbol.name.kt()},")
                appendLine("                pivotX = ${symbol.pivotX.d()},")
                appendLine("                pivotY = ${symbol.pivotY.d()},")
                appendLine("                width = ${symbol.width.d()},")
                appendLine("                height = ${symbol.height.d()},")
                appendLine("                commands = listOf(")
                symbol.commands.forEachIndexed { commandIndex, command ->
                    append("                    ${command.kt()}")
                    appendLine(if (commandIndex == symbol.commands.lastIndex) "" else ",")
                }
                appendLine("                )")
                append("            )")
                appendLine(if (index == normalized.symbols.lastIndex) "" else ",")
            }
            appendLine("        ),")
            appendLine("        lineStyles = listOf(")
            normalized.lineStyles.sortedBy { it.name }.forEachIndexed { index, style ->
                append("            SourceLineStyle(${style.name.kt()}, ${style.description.kt()})")
                appendLine(if (index == normalized.lineStyles.lastIndex) "" else ",")
            }
            appendLine("        ),")
            appendLine("        patterns = listOf(")
            normalized.patterns.sortedBy { it.name }.forEachIndexed { index, pattern ->
                append("            SourcePattern(${pattern.name.kt()}, ${pattern.description.kt()})")
                appendLine(if (index == normalized.patterns.lastIndex) "" else ",")
            }
            appendLine("        ),")
            appendLine("        lookupRecords = listOf(")
            normalized.lookupRecords.sortedWith(compareBy({ it.objectClass.name }, { it.primitive.name }, { it.viewingGroup })).forEachIndexed { index, record ->
                appendLine("            SourceLookupRecord(")
                appendLine("                objectClass = S57ObjectClass.${record.objectClass.name},")
                appendLine("                primitive = PrimitiveType.${record.primitive.name},")
                appendLine("                instruction = ${record.instruction.kt()},")
                appendLine("                displayCategory = DisplayCategory.${record.displayCategory.name},")
                appendLine("                viewingGroup = ${record.viewingGroup},")
                appendLine("                displayPriority = ${record.displayPriority},")
                val minimumDisplayScale = record.minimumDisplayScale?.d() ?: "null"
                val maximumDisplayScale = record.maximumDisplayScale?.d() ?: "null"
                appendLine("                overRadar = ${record.overRadar},")
                appendLine("                attributeFilter = ${record.attributeFilter.kt()},")
                appendLine("                minimumDisplayScale = $minimumDisplayScale,")
                appendLine("                maximumDisplayScale = $maximumDisplayScale")
                append("            )")
                appendLine(if (index == normalized.lookupRecords.lastIndex) "" else ",")
            }
            appendLine("        )")
            appendLine("    )")
            appendLine("}")
        }
    }

    private fun String.kt(): String = buildString {
        append('"')
        for (ch in this@kt) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun Double.d(): String = String.format(Locale.US, "%.6f", this).trimEnd('0').trimEnd('.') + ".0".takeIf { this % 1.0 == 0.0 }.orEmpty()

    private fun SourceAttributeFilter.kt(): String = when (this) {
        SourceAttributeFilter.Any -> "SourceAttributeFilter.Any"
        is SourceAttributeFilter.Exists -> "SourceAttributeFilter.Exists(S57Attribute.${attribute.name})"
        is SourceAttributeFilter.Missing -> "SourceAttributeFilter.Missing(S57Attribute.${attribute.name})"
        is SourceAttributeFilter.EqualsInt -> "SourceAttributeFilter.EqualsInt(S57Attribute.${attribute.name}, $expected)"
        is SourceAttributeFilter.IntIn -> "SourceAttributeFilter.IntIn(S57Attribute.${attribute.name}, setOf(${expected.sorted().joinToString()}))"
        is SourceAttributeFilter.EqualsDecimal -> "SourceAttributeFilter.EqualsDecimal(S57Attribute.${attribute.name}, ${expected.d()}, ${tolerance.d()})"
        is SourceAttributeFilter.DecimalRange -> {
            val min = minInclusive?.d() ?: "null"
            val max = maxInclusive?.d() ?: "null"
            "SourceAttributeFilter.DecimalRange(S57Attribute.${attribute.name}, $min, $max)"
        }
        is SourceAttributeFilter.TextEquals -> "SourceAttributeFilter.TextEquals(S57Attribute.${attribute.name}, ${expected.kt()}, $ignoreCase)"
        is SourceAttributeFilter.TextIn -> "SourceAttributeFilter.TextIn(S57Attribute.${attribute.name}, setOf(${expected.sorted().joinToString { it.kt() }}), $ignoreCase)"
        is SourceAttributeFilter.All -> "SourceAttributeFilter.All(listOf(${filters.joinToString { it.kt() }}))"
        is SourceAttributeFilter.AnyOf -> "SourceAttributeFilter.AnyOf(listOf(${filters.joinToString { it.kt() }}))"
        is SourceAttributeFilter.Not -> "SourceAttributeFilter.Not(${filter.kt()})"
    }

    private fun SourceVectorCommand.kt(): String = when (this) {
        is SourceVectorCommand.MoveTo -> "SourceVectorCommand.MoveTo(${x.d()}, ${y.d()})"
        is SourceVectorCommand.LineTo -> "SourceVectorCommand.LineTo(${x.d()}, ${y.d()})"
        SourceVectorCommand.ClosePath -> "SourceVectorCommand.ClosePath"
    }
}
