package io.github.s52.api

import io.github.s52.core.csp.CspRegistry
import io.github.s52.core.instruction.InstructionReferenceCollector
import io.github.s52.core.settings.S52Palette
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack

/**
 * Runtime diagnostics for the generated OpenCPN Presentation Library pack.
 *
 * This is intentionally renderer-independent. Browser demos, CLI tools, and
 * tests can all use the same report to verify that the generated pack still has
 * the expected OpenCPN payload size and to find unresolved presentation
 * references before WebGL rendering is involved.
 */
data class OpenCpnDiagnosticsReport(
    val lookupCount: Int,
    val symbolCount: Int,
    val rasterSymbolCount: Int,
    val vectorSymbolCount: Int,
    val rasterOnlySymbolCount: Int,
    val vectorOnlySymbolCount: Int,
    val lineStyleCount: Int,
    val vectorLineStyleCount: Int,
    val patternCount: Int,
    val rasterPatternCount: Int,
    val vectorPatternCount: Int,
    val colorTableCount: Int,
    val colorsPerPalette: Map<S52Palette, Int>,
    val displayCategoryCounts: Map<String, Int>,
    val primitiveCounts: Map<String, Int>,
    val presentationTableCounts: Map<String, Int>,
    val referencedSymbols: Set<String>,
    val referencedLineStyles: Set<String>,
    val referencedPatterns: Set<String>,
    val referencedColors: Set<String>,
    val referencedCsps: Set<String>,
    val unresolvedSymbols: Set<String>,
    val unresolvedLineStyles: Set<String>,
    val unresolvedPatterns: Set<String>,
    val unresolvedColors: Set<String>,
    val unresolvedCsps: Set<String>,
    val unsupportedHpglCommands: Set<String>,
    val knownRasterAtlases: Set<String>
) {
    val hasErrors: Boolean
        get() = unresolvedSymbols.isNotEmpty() ||
            unresolvedLineStyles.isNotEmpty() ||
            unresolvedPatterns.isNotEmpty() ||
            unresolvedColors.isNotEmpty() ||
            unresolvedCsps.isNotEmpty()

    fun toPlainText(maxItems: Int = 16): String = buildString {
        appendLine("OpenCPN Presentation Library diagnostics")
        appendLine("lookups=$lookupCount symbols=$symbolCount lines=$lineStyleCount patterns=$patternCount colorTables=$colorTableCount")
        appendLine("symbols: raster=$rasterSymbolCount vector=$vectorSymbolCount rasterOnly=$rasterOnlySymbolCount vectorOnly=$vectorOnlySymbolCount")
        appendLine("lineStyles: vector=$vectorLineStyleCount")
        appendLine("patterns: raster=$rasterPatternCount vector=$vectorPatternCount")
        appendLine("atlases=${knownRasterAtlases.sorted().joinToString(",")}")
        appendLine("colorsPerPalette=${colorsPerPalette.entries.sortedBy { it.key.name }.joinToString { it.key.name + "=" + it.value }}")
        appendLine("presentationTables=${presentationTableCounts.toSortedMap().entries.joinToString { it.key + "=" + it.value }}")
        appendLine("primitives=${primitiveCounts.toSortedMap().entries.joinToString { it.key + "=" + it.value }}")
        appendLine("displayCategories=${displayCategoryCounts.toSortedMap().entries.joinToString { it.key + "=" + it.value }}")
        appendLine("unresolvedSymbols=${unresolvedSymbols.describe(maxItems)}")
        appendLine("unresolvedLineStyles=${unresolvedLineStyles.describe(maxItems)}")
        appendLine("unresolvedPatterns=${unresolvedPatterns.describe(maxItems)}")
        appendLine("unresolvedColors=${unresolvedColors.describe(maxItems)}")
        appendLine("unresolvedCsps=${unresolvedCsps.describe(maxItems)}")
        appendLine("unsupportedHpglCommands=${unsupportedHpglCommands.describe(maxItems)}")
    }

    private fun Set<String>.describe(maxItems: Int): String {
        if (isEmpty()) return "none"
        val sorted = sorted()
        val suffix = if (sorted.size > maxItems) " ... +${sorted.size - maxItems}" else ""
        return sorted.take(maxItems).joinToString(",") + suffix
    }
}

object S52OpenCpnDiagnostics {
    private val supportedHpglCommands = setOf(
        "AA", "AC", "CI", "EA", "EP", "ER", "FP", "LT", "PM", "PU", "PD",
        "RA", "RR", "SP", "SW", "WG"
    )

    fun report(
        presLib: PresLibPack = PresLibPack.openCpn(),
        cspRegistry: CspRegistry = DefaultCspRegistry.openCpn()
    ): OpenCpnDiagnosticsReport {
        val records = presLib.lookupTable.records()
        val symbols = presLib.symbols.all()
        val lineStyles = presLib.lineStyles.all()
        val patterns = presLib.patterns.all()
        val refs = records.fold(io.github.s52.core.instruction.InstructionReferences()) { acc, record ->
            acc + InstructionReferenceCollector.collect(record.instructions)
        }
        val symbolNames = presLib.symbols.names().uppercased()
        val lineNames = presLib.lineStyles.names().uppercased()
        val patternNames = presLib.patterns.names().uppercased()
        val colorTokens = S52Palette.entries.flatMap { presLib.colors.tokens(it) }.toSet().uppercased()
        val cspNames = cspRegistry.names().uppercased()
        val assetColorRefs = symbols.flatMap { it.colorRefs } + lineStyles.flatMap { it.colorRefs } + patterns.flatMap { it.colorRefs }
        val referencedColors = (refs.colorTokens + assetColorRefs).filterMeaningfulTokens().uppercased()
        val hpglCommands = (symbols.mapNotNull { it.vectorHpgl } + lineStyles.mapNotNull { it.vectorHpgl } + patterns.mapNotNull { it.vectorHpgl })
            .flatMap { hpglMnemonics(it) }
            .toSet()

        return OpenCpnDiagnosticsReport(
            lookupCount = records.size,
            symbolCount = symbols.size,
            rasterSymbolCount = symbols.count { it.bitmap != null },
            vectorSymbolCount = symbols.count { !it.vectorHpgl.isNullOrBlank() || it.commands.isNotEmpty() },
            rasterOnlySymbolCount = symbols.count { it.bitmap != null && it.vectorHpgl.isNullOrBlank() && it.commands.isEmpty() },
            vectorOnlySymbolCount = symbols.count { it.bitmap == null && (!it.vectorHpgl.isNullOrBlank() || it.commands.isNotEmpty()) },
            lineStyleCount = lineStyles.size,
            vectorLineStyleCount = lineStyles.count { !it.vectorHpgl.isNullOrBlank() },
            patternCount = patterns.size,
            rasterPatternCount = patterns.count { it.bitmap != null },
            vectorPatternCount = patterns.count { !it.vectorHpgl.isNullOrBlank() },
            colorTableCount = S52Palette.entries.count { presLib.colors.tokens(it).isNotEmpty() },
            colorsPerPalette = S52Palette.entries.associateWith { presLib.colors.tokens(it).size },
            displayCategoryCounts = records.countBy { it.displayCategory.name },
            primitiveCounts = records.countBy { it.primitive.name },
            presentationTableCounts = records.countBy { it.sourceTableName.orEmpty().ifBlank { "<none>" } },
            referencedSymbols = refs.symbols.uppercased(),
            referencedLineStyles = refs.lineStyles.uppercased(),
            referencedPatterns = refs.patterns.uppercased(),
            referencedColors = referencedColors,
            referencedCsps = refs.csps.uppercased(),
            unresolvedSymbols = refs.symbols.uppercased() - symbolNames,
            unresolvedLineStyles = refs.lineStyles.uppercased() - lineNames,
            unresolvedPatterns = refs.patterns.uppercased() - patternNames,
            unresolvedColors = referencedColors - colorTokens,
            unresolvedCsps = refs.csps.uppercased() - cspNames,
            unsupportedHpglCommands = hpglCommands - supportedHpglCommands,
            knownRasterAtlases = (
                symbols.mapNotNull { it.bitmap?.atlasFileName } +
                    lineStyles.mapNotNull { it.bitmap?.atlasFileName } +
                    patterns.mapNotNull { it.bitmap?.atlasFileName }
                ).toSet()
        )
    }

    private fun hpglMnemonics(hpgl: String): List<String> = hpgl.split(';')
        .mapNotNull { token ->
            val trimmed = token.trim()
            if (trimmed.length < 2) null else trimmed.take(2).uppercase().takeIf { it.all(Char::isLetter) }
        }

    private fun Collection<String>.filterMeaningfulTokens(): Set<String> = asSequence()
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }
        .filterNot { it == "UNKNOWN" || it == "NONE" || it == "NULL" }
        .toSet()

    private fun Collection<String>.uppercased(): Set<String> = map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()

    private fun <T> List<T>.countBy(key: (T) -> String): Map<String, Int> =
        groupingBy(key).eachCount()
}
