package io.github.s52.preslib.opencpn.generator

import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceBitmapRef
import io.github.s52.preslib.source.SourceLineStyle
import io.github.s52.preslib.source.SourceLookupRecord
import io.github.s52.preslib.source.SourcePattern
import io.github.s52.preslib.source.SourceSymbol
import java.util.Locale

/** JVM generator for the checked-in commonMain OpenCPN source pack. */
object OpenCpnGeneratedKotlinWriter {
    fun generate(source: PresLibSourcePack): String = buildString {
        appendLine("package io.github.s52.preslib.opencpn.generated")
        appendLine()
        appendLine("import io.github.s52.catalog.PrimitiveType")
        appendLine("import io.github.s52.catalog.S57ObjectClass")
        appendLine("import io.github.s52.catalog.S57ObjectClassKey")
        appendLine("import io.github.s52.core.settings.DisplayCategory")
        appendLine("import io.github.s52.core.settings.S52Palette")
        appendLine("import io.github.s52.preslib.PresLibPack")
        appendLine("import io.github.s52.preslib.source.PresLibMetadata")
        appendLine("import io.github.s52.preslib.source.PresLibPackBuilder")
        appendLine("import io.github.s52.preslib.source.PresLibSourcePack")
        appendLine("import io.github.s52.preslib.source.SourceBitmapRef")
        appendLine("import io.github.s52.preslib.source.SourceColor")
        appendLine("import io.github.s52.preslib.source.SourceColorTable")
        appendLine("import io.github.s52.preslib.source.SourceLineStyle")
        appendLine("import io.github.s52.preslib.source.SourceLookupRecord")
        appendLine("import io.github.s52.preslib.source.SourcePattern")
        appendLine("import io.github.s52.preslib.source.SourceSymbol")
        appendLine()
        appendLine("/** Generated from s52/opencpn/chartsymbols.xml by Phase 28C. */")
        appendLine("object OpenCpnGeneratedPresLib {")
        appendLine("    const val LOOKUP_COUNT: Int = ${source.lookupRecords.size}")
        appendLine("    const val SYMBOL_COUNT: Int = ${source.symbols.size}")
        appendLine("    const val LINE_STYLE_COUNT: Int = ${source.lineStyles.size}")
        appendLine("    const val PATTERN_COUNT: Int = ${source.patterns.size}")
        appendLine("    const val COLOR_TABLE_COUNT: Int = ${source.colorTables.size}")
        appendLine()
        appendLine("    fun pack(): PresLibPack = PresLibPackBuilder.build(sourcePack())")
        appendLine()
        appendLine("    fun sourcePack(): PresLibSourcePack = PresLibSourcePack(")
        appendLine("        metadata = PresLibMetadata(${source.metadata.name.kt()}, ${source.metadata.edition.kt()}, ${source.metadata.sourceDescription.kt()}, ${source.metadata.generatedBy.kt()}),")
        appendLine("        colorTables = colorTables(),")
        appendLine("        symbols = symbols(),")
        appendLine("        lineStyles = lineStyles(),")
        appendLine("        patterns = patterns(),")
        appendLine("        lookupRecords = lookupRecords()")
        appendLine("    )")
        appendLine()
        appendLine("    private fun colorTables() = listOf(")
        source.colorTables.forEachIndexed { index, table ->
            appendLine("        SourceColorTable(S52Palette.${table.palette.name}, listOf(")
            table.colors.forEachIndexed { colorIndex, color ->
                appendLine("            SourceColor(${color.token.kt()}, ${color.r}, ${color.g}, ${color.b})${comma(colorIndex, table.colors.lastIndex)}")
            }
            appendLine("        ))${comma(index, source.colorTables.lastIndex)}")
        }
        appendLine("    )")
        appendLine()
        appendAssetChunks("symbols", "SourceSymbol", source.symbols, 50) { it.symbolExpr() }
        appendAssetChunks("lineStyles", "SourceLineStyle", source.lineStyles, 60) { it.lineStyleExpr() }
        appendAssetChunks("patterns", "SourcePattern", source.patterns, 60) { it.patternExpr() }
        appendAssetChunks("lookupRecords", "SourceLookupRecord", source.lookupRecords, 150) { it.lookupExpr() }
        appendLine("}")
    }

    private fun <T> StringBuilder.appendAssetChunks(
        baseName: String,
        typeName: String,
        values: List<T>,
        chunkSize: Int,
        expr: (T) -> String
    ) {
        val chunkCount = (values.size + chunkSize - 1) / chunkSize
        appendLine("    private fun $baseName(): List<$typeName> = listOf(${(0 until chunkCount).joinToString { "$baseName${it}()" }}).flatten()")
        appendLine()
        values.chunked(chunkSize).forEachIndexed { chunkIndex, chunk ->
            appendLine("    private fun $baseName$chunkIndex(): List<$typeName> = listOf(")
            chunk.forEachIndexed { index, value -> appendLine("        ${expr(value)}${comma(index, chunk.lastIndex)}") }
            appendLine("    )")
            appendLine()
        }
    }

    private fun SourceSymbol.symbolExpr(): String = "SourceSymbol(name = ${name.kt()}, pivotX = ${pivotX.d()}, pivotY = ${pivotY.d()}, width = ${width.d()}, height = ${height.d()}, colorRefs = ${colorRefs.ktList()}, bitmap = ${bitmap.kt()}, vectorHpgl = ${vectorHpgl.ktNullable()})"
    private fun SourceLineStyle.lineStyleExpr(): String = "SourceLineStyle(name = ${name.kt()}, description = ${description.kt()}, pivotX = ${pivotX.d()}, pivotY = ${pivotY.d()}, width = ${width.d()}, height = ${height.d()}, colorRefs = ${colorRefs.ktList()}, bitmap = ${bitmap.kt()}, vectorHpgl = ${vectorHpgl.ktNullable()})"
    private fun SourcePattern.patternExpr(): String = "SourcePattern(name = ${name.kt()}, description = ${description.kt()}, pivotX = ${pivotX.d()}, pivotY = ${pivotY.d()}, width = ${width.d()}, height = ${height.d()}, colorRefs = ${colorRefs.ktList()}, bitmap = ${bitmap.kt()}, vectorHpgl = ${vectorHpgl.ktNullable()})"

    private fun SourceLookupRecord.lookupExpr(): String {
        val classArg = if (objectClass != null) "objectClass = S57ObjectClass.${objectClass.name}" else "objectClass = null, objectClassKey = S57ObjectClassKey.of(${objectClassKey.acronym.kt()})"
        return "SourceLookupRecord($classArg, primitive = PrimitiveType.${primitive.name}, instruction = ${instruction.kt()}, displayCategory = DisplayCategory.${displayCategory.name}, viewingGroup = $viewingGroup, displayPriority = $displayPriority, overRadar = $overRadar, sourceTableName = ${sourceTableName.ktNullable()}, sourceDisplayPriorityLabel = ${sourceDisplayPriorityLabel.ktNullable()}, sourceRadarPriority = ${sourceRadarPriority.ktNullable()}, rawAttribCodes = ${rawAttribCodes.ktList()})"
    }

    private fun SourceBitmapRef?.kt(): String = this?.let {
        "SourceBitmapRef(atlasFileName = ${atlasFileName.kt()}, x = ${x.d()}, y = ${y.d()}, width = ${width.d()}, height = ${height.d()}, pivotX = ${pivotX.d()}, pivotY = ${pivotY.d()}, originX = ${originX.d()}, originY = ${originY.d()})"
    } ?: "null"

    private fun List<String>.ktList(): String = if (isEmpty()) "emptyList()" else joinToString(prefix = "listOf(", postfix = ")") { it.kt() }
    private fun String?.ktNullable(): String = this?.kt() ?: "null"
    private fun String.kt(): String = buildString {
        append('"')
        for (ch in this@kt) when (ch) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '$' -> { append('\\'); append('$') }
            else -> append(ch)
        }
        append('"')
    }
    private fun Double.d(): String = String.format(Locale.US, "%.6f", this).trimEnd('0').trimEnd('.') + if (this % 1.0 == 0.0) ".0" else ""
    private fun comma(index: Int, lastIndex: Int): String = if (index == lastIndex) "" else ","
}
