package io.github.s52.preslib.validation

import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.PresLibSourceNormalizer
import io.github.s52.preslib.source.SourceColor
import io.github.s52.preslib.source.s52Token

sealed interface PresLibValidationDiagnostic {
    val message: String

    data class MissingPalette(val palette: S52Palette) : PresLibValidationDiagnostic {
        override val message: String = "Missing color table for palette $palette"
    }

    data class MissingColor(val token: String, val palette: S52Palette) : PresLibValidationDiagnostic {
        override val message: String = "Missing color token $token in palette $palette"
    }

    data class MissingSymbol(val name: String) : PresLibValidationDiagnostic {
        override val message: String = "Missing symbol $name"
    }

    data class MissingLineStyle(val name: String) : PresLibValidationDiagnostic {
        override val message: String = "Missing line style $name"
    }

    data class MissingPattern(val name: String) : PresLibValidationDiagnostic {
        override val message: String = "Missing pattern $name"
    }

    data class DuplicateSourceName(val kind: String, val name: String) : PresLibValidationDiagnostic {
        override val message: String = "Duplicate $kind name $name in source pack"
    }

    data class InvalidColorRange(val token: String, val r: Int, val g: Int, val b: Int) : PresLibValidationDiagnostic {
        override val message: String = "Color $token has RGB values outside 0..255: $r,$g,$b"
    }
}

data class PresLibValidationReport(
    val diagnostics: List<PresLibValidationDiagnostic>,
    val lookupRecordCount: Int,
    val symbolCount: Int,
    val lineStyleCount: Int,
    val patternCount: Int,
    val palettes: Set<S52Palette>,
    val referencedSymbols: Set<String>,
    val referencedLineStyles: Set<String>,
    val referencedPatterns: Set<String>,
    val referencedColors: Set<String>,
    val referencedCsps: Set<String>
) {
    val hasErrors: Boolean get() = diagnostics.isNotEmpty()

    fun toMarkdown(): String = buildString {
        appendLine("# Presentation Library Validation Report")
        appendLine()
        appendLine("- Lookup records: $lookupRecordCount")
        appendLine("- Symbols: $symbolCount")
        appendLine("- Line styles: $lineStyleCount")
        appendLine("- Patterns: $patternCount")
        appendLine("- Palettes: ${palettes.size}")
        appendLine("- Referenced symbols: ${referencedSymbols.size}")
        appendLine("- Referenced line styles: ${referencedLineStyles.size}")
        appendLine("- Referenced patterns: ${referencedPatterns.size}")
        appendLine("- Referenced colors: ${referencedColors.size}")
        appendLine("- Referenced CSPs: ${referencedCsps.size}")
        appendLine("- Diagnostics: ${diagnostics.size}")
        if (diagnostics.isNotEmpty()) {
            appendLine()
            appendLine("## Diagnostics")
            diagnostics.forEach { appendLine("- ${it.message}") }
        }
    }
}

object PresLibValidator {
    fun validate(pack: PresLibPack): PresLibValidationReport {
        val records = pack.lookupTable.records()
        val references = PresLibReferences.from(pack)
        val diagnostics = mutableListOf<PresLibValidationDiagnostic>()

        for (palette in S52Palette.entries) {
            val tokens = pack.colors.tokens(palette)
            if (tokens.isEmpty()) {
                diagnostics += PresLibValidationDiagnostic.MissingPalette(palette)
            }
            references.colors.forEach { token ->
                if (token !in tokens) {
                    diagnostics += PresLibValidationDiagnostic.MissingColor(token, palette)
                }
            }
        }

        references.symbols.forEach { symbol ->
            if (pack.symbols.find(symbol) == null) diagnostics += PresLibValidationDiagnostic.MissingSymbol(symbol)
        }
        references.lineStyles.forEach { style ->
            if (pack.lineStyles.find(style) == null) diagnostics += PresLibValidationDiagnostic.MissingLineStyle(style)
        }
        references.patterns.forEach { pattern ->
            if (pack.patterns.find(pattern) == null) diagnostics += PresLibValidationDiagnostic.MissingPattern(pattern)
        }

        return PresLibValidationReport(
            diagnostics = diagnostics,
            lookupRecordCount = records.size,
            symbolCount = pack.symbols.names().size,
            lineStyleCount = pack.lineStyles.names().size,
            patternCount = pack.patterns.names().size,
            palettes = S52Palette.entries.filter { pack.colors.tokens(it).isNotEmpty() }.toSet(),
            referencedSymbols = references.symbols,
            referencedLineStyles = references.lineStyles,
            referencedPatterns = references.patterns,
            referencedColors = references.colors,
            referencedCsps = references.csps
        )
    }

    fun validateSource(source: PresLibSourcePack): List<PresLibValidationDiagnostic> {
        val normalized = PresLibSourceNormalizer.normalize(source)
        return buildList {
            normalized.colorTables.forEach { table ->
                addDuplicateDiagnostics("color in ${table.palette}", table.colors.map(SourceColor::token))
            }
            addDuplicateDiagnostics("symbol", normalized.symbols.map { it.name })
            addDuplicateDiagnostics("line style", normalized.lineStyles.map { it.name })
            addDuplicateDiagnostics("pattern", normalized.patterns.map { it.name })
            for (table in normalized.colorTables) {
                table.colors.forEach { color ->
                    if (color.r !in 0..255 || color.g !in 0..255 || color.b !in 0..255) {
                        add(PresLibValidationDiagnostic.InvalidColorRange(color.token, color.r, color.g, color.b))
                    }
                }
            }
        }
    }

    private fun MutableList<PresLibValidationDiagnostic>.addDuplicateDiagnostics(kind: String, names: List<String>) {
        names.groupingBy { it.s52Token() }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { add(PresLibValidationDiagnostic.DuplicateSourceName(kind, it)) }
    }
}

internal data class PresLibReferences(
    val symbols: Set<String>,
    val lineStyles: Set<String>,
    val patterns: Set<String>,
    val colors: Set<String>,
    val csps: Set<String>
) {
    companion object {
        fun from(pack: PresLibPack): PresLibReferences {
            val symbols = mutableSetOf<String>()
            val lineStyles = mutableSetOf<String>()
            val patterns = mutableSetOf<String>()
            val colors = mutableSetOf<String>()
            val csps = mutableSetOf<String>()

            pack.lookupTable.records().flatMap { it.instructions }.forEach { instruction ->
                when (instruction) {
                    is S52Instruction.Symbol -> symbols += instruction.name.s52Token()
                    is S52Instruction.SimpleLine -> {
                        lineStyles += instruction.style.s52Token()
                        colors += instruction.colorToken.s52Token()
                    }
                    is S52Instruction.ComplexLine -> lineStyles += instruction.name.s52Token()
                    is S52Instruction.AreaColor -> colors += instruction.colorToken.s52Token()
                    is S52Instruction.AreaPattern -> patterns += instruction.name.s52Token()
                    is S52Instruction.Text -> Unit
                    is S52Instruction.Conditional -> csps += instruction.cspName.s52Token()
                }
            }
            return PresLibReferences(symbols, lineStyles, patterns, colors, csps)
        }
    }
}
