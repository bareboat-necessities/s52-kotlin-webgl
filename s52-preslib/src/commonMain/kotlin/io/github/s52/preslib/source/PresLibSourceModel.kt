package io.github.s52.preslib.source

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette

/**
 * Source-side Presentation Library interchange model.
 *
 * Phase 2 intentionally keeps this independent from the runtime registries so
 * an importer/generator can validate and normalize names before creating a
 * [io.github.s52.preslib.PresLibPack]. It is not the official IHO source
 * format; it is a compact internal handoff format for generated data.
 */
data class PresLibSourcePack(
    val metadata: PresLibMetadata,
    val colorTables: List<SourceColorTable>,
    val symbols: List<SourceSymbol>,
    val lineStyles: List<SourceLineStyle>,
    val patterns: List<SourcePattern>,
    val lookupRecords: List<SourceLookupRecord>
)

data class PresLibMetadata(
    val name: String,
    val edition: String,
    val sourceDescription: String,
    val generatedBy: String
)

data class SourceColorTable(
    val palette: S52Palette,
    val colors: List<SourceColor>
)

data class SourceColor(
    val token: String,
    val r: Int,
    val g: Int,
    val b: Int
)

data class SourceSymbol(
    val name: String,
    val pivotX: Double = 0.0,
    val pivotY: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val commands: List<SourceVectorCommand> = emptyList()
)

sealed interface SourceVectorCommand {
    data class MoveTo(val x: Double, val y: Double) : SourceVectorCommand
    data class LineTo(val x: Double, val y: Double) : SourceVectorCommand
    data object ClosePath : SourceVectorCommand
}

data class SourceLineStyle(
    val name: String,
    val description: String = ""
)

data class SourcePattern(
    val name: String,
    val description: String = ""
)

data class SourceLookupRecord(
    val objectClass: S57ObjectClass,
    val primitive: PrimitiveType,
    val instruction: String,
    val displayCategory: DisplayCategory,
    val viewingGroup: Int,
    val displayPriority: Int,
    val overRadar: Boolean = false
)
