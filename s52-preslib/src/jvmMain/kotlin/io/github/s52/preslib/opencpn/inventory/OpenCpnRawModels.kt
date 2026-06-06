package io.github.s52.preslib.opencpn.inventory

import java.io.File

/**
 * Raw, lossless-enough OpenCPN portrayal payload model used by Phase 28A.
 *
 * This package intentionally does not feed the runtime Presentation Library yet.
 * It gives later phases a verified view of the XML/CSV/PNG payload while keeping
 * existing rendering behavior unchanged.
 */
data class OpenCpnPayloadInventory(
    val directory: File,
    val files: List<OpenCpnPayloadFile>,
    val chartSymbols: OpenCpnChartSymbolsSummary?,
    val csvCatalog: OpenCpnCsvCatalogSummary,
    val rasterAtlases: List<OpenCpnRasterAtlas>,
    val diagnostics: OpenCpnInventoryDiagnostics,
    val lookupDiagnostics: OpenCpnLookupDiagnostics? = null
) {
    fun toHumanText(): String = buildString {
        appendLine("OpenCPN portrayal payload: ${directory.absolutePath}")
        appendLine("files=${files.size}")
        chartSymbols?.let { summary ->
            appendLine("chartsymbols.xml:")
            appendLine("  colorTables=${summary.colorTables.size}")
            appendLine("  colors=${summary.colorTables.sumOf { it.colors.size }}")
            appendLine("  lookups=${summary.lookupCount}")
            appendLine("  lookupObjects=${summary.lookups.map { it.objectClassKey.acronym }.distinct().size}")
            appendLine("  symbols=${summary.symbols.size}")
            appendLine("  lineStyles=${summary.lineStyles.size}")
            appendLine("  patterns=${summary.patterns.size}")
            appendLine("  symbolBitmap=${summary.symbols.count { it.bitmap != null }}")
            appendLine("  symbolVector=${summary.symbols.count { it.vector != null }}")
            appendLine("  lineStyleVector=${summary.lineStyles.count { it.vector != null }}")
            appendLine("  patternBitmap=${summary.patterns.count { it.bitmap != null }}")
            appendLine("  patternVector=${summary.patterns.count { it.vector != null }}")
        } ?: appendLine("chartsymbols.xml: missing")
        appendLine("CSV catalog:")
        appendLine("  objectClasses=${csvCatalog.objectClasses.size}")
        appendLine("  attributes=${csvCatalog.attributes.size}")
        appendLine("  expectedInputs=${csvCatalog.expectedInputs.size}")
        appendLine("  attributeDecodes=${csvCatalog.attributeDecodes.size}")
        appendLine("rasterAtlases=${rasterAtlases.size}")
        rasterAtlases.forEach { atlas ->
            appendLine("  ${atlas.fileName}: ${atlas.width}x${atlas.height} ${atlas.paletteHint}")
        }
        lookupDiagnostics?.let { lookup ->
            appendLine(lookup.toHumanText().trimEnd())
        }
        if (diagnostics.hasIssues()) {
            appendLine("diagnostics:")
            diagnostics.missingRequiredFiles.forEach { appendLine("  missing=$it") }
            diagnostics.parseWarnings.forEach { appendLine("  warning=$it") }
        }
    }
}

data class OpenCpnPayloadFile(
    val fileName: String,
    val sizeBytes: Long,
    val exists: Boolean
)

data class OpenCpnInventoryDiagnostics(
    val missingRequiredFiles: List<String> = emptyList(),
    val parseWarnings: List<String> = emptyList()
) {
    fun hasIssues(): Boolean = missingRequiredFiles.isNotEmpty() || parseWarnings.isNotEmpty()
}

data class OpenCpnRasterAtlas(
    val fileName: String,
    val width: Int,
    val height: Int,
    val paletteHint: OpenCpnRasterPaletteHint,
    val sizeBytes: Long
)

enum class OpenCpnRasterPaletteHint {
    Day,
    Dusk,
    Dark,
    Unknown
}

data class OpenCpnChartSymbolsSummary(
    val colorTables: List<OpenCpnRawColorTable>,
    val lookupCount: Int,
    val symbols: List<OpenCpnRawAsset>,
    val lineStyles: List<OpenCpnRawAsset>,
    val patterns: List<OpenCpnRawAsset>,
    val lookups: List<OpenCpnRawLookupRecord> = emptyList()
)

data class OpenCpnRawColorTable(
    val name: String,
    val colors: List<OpenCpnRawColor>
)

data class OpenCpnRawColor(
    val name: String,
    val r: Int,
    val g: Int,
    val b: Int
)

data class OpenCpnRawAsset(
    val rcid: String?,
    val name: String,
    val kind: OpenCpnRawAssetKind,
    val description: String,
    val colorRefs: List<String>,
    val bitmap: OpenCpnRawBitmap?,
    val vector: OpenCpnRawVector?,
    val definition: String?
) {
    val hasBitmap: Boolean get() = bitmap != null
    val hasVector: Boolean get() = vector != null && vector.hpgl.isNotBlank()
}

enum class OpenCpnRawAssetKind {
    Symbol,
    LineStyle,
    Pattern
}

data class OpenCpnRawBitmap(
    val width: Int?,
    val height: Int?,
    val pivot: OpenCpnRawPoint?,
    val origin: OpenCpnRawPoint?,
    val graphicsLocation: OpenCpnRawPoint?,
    val minDistance: Double?,
    val maxDistance: Double?
)

data class OpenCpnRawVector(
    val width: Double?,
    val height: Double?,
    val pivot: OpenCpnRawPoint?,
    val origin: OpenCpnRawPoint?,
    val hpgl: String
)

data class OpenCpnRawPoint(
    val x: Double,
    val y: Double
)

data class OpenCpnCsvCatalogSummary(
    val objectClasses: List<OpenCpnObjectClassRow> = emptyList(),
    val attributes: List<OpenCpnAttributeRow> = emptyList(),
    val expectedInputs: List<OpenCpnExpectedInputRow> = emptyList(),
    val attributeDecodes: List<OpenCpnAttributeDecodeRow> = emptyList()
)

data class OpenCpnObjectClassRow(
    val code: Int?,
    val objectClass: String,
    val acronym: String,
    val attributeA: List<String>,
    val attributeB: List<String>,
    val attributeC: List<String>,
    val clazz: String,
    val primitives: List<String>
)

data class OpenCpnAttributeRow(
    val code: Int?,
    val attribute: String,
    val acronym: String,
    val attributeType: String,
    val clazz: String
)

data class OpenCpnExpectedInputRow(
    val code: Int?,
    val id: Int?,
    val meaning: String
)

data class OpenCpnAttributeDecodeRow(
    val attribute: String,
    val values: List<OpenCpnDecodedAttributeValue>
)

data class OpenCpnDecodedAttributeValue(
    val id: Int,
    val label: String
)
