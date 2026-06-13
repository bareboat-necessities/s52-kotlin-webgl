package io.github.s52.api

import io.github.s52.core.csp.CspRegistry
import io.github.s52.core.instruction.InstructionReferenceCollector
import io.github.s52.core.settings.S52Palette
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack

data class OpenCpnAssetClassCoverage(
    val declared: Int,
    val referenced: Int,
    val resolved: Int,
    val unresolved: Set<String>,
    val raster: Int = 0,
    val vector: Int = 0,
    val rasterOnly: Int = 0,
    val vectorOnly: Int = 0,
    val hpglCompiled: Int = 0,
    val hpglFillCapable: Int = 0
)

data class OpenCpnHpglCoverage(
    val assetCount: Int,
    val compiledDisplayListAssetCount: Int,
    val fillCapableAssetCount: Int,
    val unsupportedCommands: Set<String>
)

data class OpenCpnAssetCoverageIndex(
    val symbols: OpenCpnAssetClassCoverage,
    val lineStyles: OpenCpnAssetClassCoverage,
    val patterns: OpenCpnAssetClassCoverage,
    val colors: OpenCpnAssetClassCoverage,
    val csps: OpenCpnAssetClassCoverage,
    val hpgl: OpenCpnHpglCoverage,
    val knownRasterAtlases: Set<String>,
    val primitiveLookupCounts: Map<String, Int>,
    val displayCategoryLookupCounts: Map<String, Int>,
    val presentationTableLookupCounts: Map<String, Int>
) {
    val hasErrors: Boolean
        get() = symbols.unresolved.isNotEmpty() ||
            lineStyles.unresolved.isNotEmpty() ||
            patterns.unresolved.isNotEmpty() ||
            colors.unresolved.isNotEmpty() ||
            csps.unresolved.isNotEmpty() ||
            hpgl.unsupportedCommands.isNotEmpty()
}

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
    val knownRasterAtlases: Set<String>,
    val coverageIndex: OpenCpnAssetCoverageIndex
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
        appendLine("presentationTables=${presentationTableCounts.describeCounts()}")
        appendLine("primitives=${primitiveCounts.describeCounts()}")
        appendLine("displayCategories=${displayCategoryCounts.describeCounts()}")
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

    private fun Map<String, Int>.describeCounts(): String = entries
        .sortedBy { it.key }
        .joinToString { it.key + "=" + it.value }
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

        val referencedSymbols = refs.symbols.uppercased()
        val referencedLineStyles = refs.lineStyles.uppercased()
        val referencedPatterns = refs.patterns.uppercased()
        val referencedCsps = refs.csps.uppercased()
        val resolvedSymbolNames = symbolNames + openCpnSymbolAliases.keys.filter { openCpnSymbolAliases[it] in symbolNames }
        val unsupportedHpglCommands = hpglCommands - supportedHpglCommands
        val knownRasterAtlases = (
            symbols.mapNotNull { it.bitmap?.atlasFileName } +
                lineStyles.mapNotNull { it.bitmap?.atlasFileName } +
                patterns.mapNotNull { it.bitmap?.atlasFileName }
            ).toSet()
        val primitiveCounts = records.countBy { it.primitive.name }
        val displayCategoryCounts = records.countBy { it.displayCategory.name }
        val presentationTableCounts = records.countBy { it.sourceTableName.orEmpty().ifBlank { "<none>" } }
        val coverageIndex = OpenCpnAssetCoverageIndex(
            symbols = OpenCpnAssetClassCoverage(
                declared = symbols.size,
                referenced = referencedSymbols.size,
                resolved = referencedSymbols.count { it in resolvedSymbolNames },
                unresolved = referencedSymbols - resolvedSymbolNames,
                raster = symbols.count { it.bitmap != null },
                vector = symbols.count { !it.vectorHpgl.isNullOrBlank() || it.commands.isNotEmpty() },
                rasterOnly = symbols.count { it.bitmap != null && it.vectorHpgl.isNullOrBlank() && it.commands.isEmpty() },
                vectorOnly = symbols.count { it.bitmap == null && (!it.vectorHpgl.isNullOrBlank() || it.commands.isNotEmpty()) },
                hpglCompiled = symbols.count { !it.vectorHpgl.isNullOrBlank() },
                hpglFillCapable = symbols.count { it.vectorHpgl.hasHpglFill() }
            ),
            lineStyles = OpenCpnAssetClassCoverage(
                declared = lineStyles.size,
                referenced = referencedLineStyles.size,
                resolved = referencedLineStyles.count { it in lineNames },
                unresolved = referencedLineStyles - lineNames,
                raster = lineStyles.count { it.bitmap != null },
                vector = lineStyles.count { !it.vectorHpgl.isNullOrBlank() },
                rasterOnly = lineStyles.count { it.bitmap != null && it.vectorHpgl.isNullOrBlank() },
                vectorOnly = lineStyles.count { it.bitmap == null && !it.vectorHpgl.isNullOrBlank() },
                hpglCompiled = lineStyles.count { !it.vectorHpgl.isNullOrBlank() },
                hpglFillCapable = lineStyles.count { it.vectorHpgl.hasHpglFill() }
            ),
            patterns = OpenCpnAssetClassCoverage(
                declared = patterns.size,
                referenced = referencedPatterns.size,
                resolved = referencedPatterns.count { it in patternNames },
                unresolved = referencedPatterns - patternNames,
                raster = patterns.count { it.bitmap != null },
                vector = patterns.count { !it.vectorHpgl.isNullOrBlank() },
                rasterOnly = patterns.count { it.bitmap != null && it.vectorHpgl.isNullOrBlank() },
                vectorOnly = patterns.count { it.bitmap == null && !it.vectorHpgl.isNullOrBlank() },
                hpglCompiled = patterns.count { !it.vectorHpgl.isNullOrBlank() },
                hpglFillCapable = patterns.count { it.vectorHpgl.hasHpglFill() }
            ),
            colors = OpenCpnAssetClassCoverage(
                declared = colorTokens.size,
                referenced = referencedColors.size,
                resolved = referencedColors.count { it in colorTokens },
                unresolved = referencedColors - colorTokens
            ),
            csps = OpenCpnAssetClassCoverage(
                declared = cspNames.size,
                referenced = referencedCsps.size,
                resolved = referencedCsps.count { it in cspNames },
                unresolved = referencedCsps - cspNames
            ),
            hpgl = OpenCpnHpglCoverage(
                assetCount = symbols.count { !it.vectorHpgl.isNullOrBlank() } + lineStyles.count { !it.vectorHpgl.isNullOrBlank() } + patterns.count { !it.vectorHpgl.isNullOrBlank() },
                compiledDisplayListAssetCount = symbols.count { !it.vectorHpgl.isNullOrBlank() } + lineStyles.count { !it.vectorHpgl.isNullOrBlank() } + patterns.count { !it.vectorHpgl.isNullOrBlank() },
                fillCapableAssetCount = symbols.count { it.vectorHpgl.hasHpglFill() } + lineStyles.count { it.vectorHpgl.hasHpglFill() } + patterns.count { it.vectorHpgl.hasHpglFill() },
                unsupportedCommands = unsupportedHpglCommands
            ),
            knownRasterAtlases = knownRasterAtlases,
            primitiveLookupCounts = primitiveCounts,
            displayCategoryLookupCounts = displayCategoryCounts,
            presentationTableLookupCounts = presentationTableCounts
        )

        return OpenCpnDiagnosticsReport(
            lookupCount = records.size,
            symbolCount = symbols.size,
            rasterSymbolCount = coverageIndex.symbols.raster,
            vectorSymbolCount = coverageIndex.symbols.vector,
            rasterOnlySymbolCount = coverageIndex.symbols.rasterOnly,
            vectorOnlySymbolCount = coverageIndex.symbols.vectorOnly,
            lineStyleCount = lineStyles.size,
            vectorLineStyleCount = coverageIndex.lineStyles.vector,
            patternCount = patterns.size,
            rasterPatternCount = coverageIndex.patterns.raster,
            vectorPatternCount = coverageIndex.patterns.vector,
            colorTableCount = S52Palette.entries.count { presLib.colors.tokens(it).isNotEmpty() },
            colorsPerPalette = S52Palette.entries.associateWith { presLib.colors.tokens(it).size },
            displayCategoryCounts = displayCategoryCounts,
            primitiveCounts = primitiveCounts,
            presentationTableCounts = presentationTableCounts,
            referencedSymbols = referencedSymbols,
            referencedLineStyles = referencedLineStyles,
            referencedPatterns = referencedPatterns,
            referencedColors = referencedColors,
            referencedCsps = referencedCsps,
            unresolvedSymbols = coverageIndex.symbols.unresolved,
            unresolvedLineStyles = coverageIndex.lineStyles.unresolved,
            unresolvedPatterns = coverageIndex.patterns.unresolved,
            unresolvedColors = coverageIndex.colors.unresolved,
            unresolvedCsps = coverageIndex.csps.unresolved,
            unsupportedHpglCommands = unsupportedHpglCommands,
            knownRasterAtlases = knownRasterAtlases,
            coverageIndex = coverageIndex
        )
    }

    fun coverageIndex(
        presLib: PresLibPack = PresLibPack.openCpn(),
        cspRegistry: CspRegistry = DefaultCspRegistry.openCpn()
    ): OpenCpnAssetCoverageIndex = report(presLib, cspRegistry).coverageIndex

    private fun hpglMnemonics(hpgl: String): List<String> = hpgl.split(';')
        .mapNotNull { token ->
            val trimmed = token.trim()
            if (trimmed.length < 2) null else trimmed.take(2).uppercase().takeIf { it.all(Char::isLetter) }
        }

    private fun String?.hasHpglFill(): Boolean = !isNullOrBlank() && hpglMnemonics(this).any { it == "FP" || it == "RA" || it == "RR" || it == "WG" }

    private val openCpnSymbolAliases = mapOf(
        "TOPMAR_CONE_UP01" to "TOPMAR88",
        "TOPMAR_CONE_DOWN01" to "TOPMAR87",
        "TOPMAR_SPHERE01" to "TOPMAR65",
        "TOPMAR_TWO_SPHERES01" to "TOPMAR86",
        "TOPMAR_CYLINDER01" to "TOPMAR85",
        "TOPMAR_X01" to "QUESMRK1",
        "TOPMAR_CROSS01" to "QUESMRK1",
        "TOPMAR_UNKNOWN01" to "QUESMRK1",
        "WRECKS_DANGER01" to "ISODGR01",
        "WRECKS01" to "WRECKS05",
        "OBSTRN_DANGER01" to "ISODGR01",
        "OBSTRN01" to "OBSTRN11"
    )

    private fun Collection<String>.filterMeaningfulTokens(): Set<String> = asSequence()
        .map { it.trim().uppercase() }
        .filter { it.isNotEmpty() }
        .filterNot { it == "UNKNOWN" || it == "NONE" || it == "NULL" }
        .toSet()

    private fun Collection<String>.uppercased(): Set<String> = map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()

    private fun <T> List<T>.countBy(key: (T) -> String): Map<String, Int> =
        groupingBy(key).eachCount()
}
