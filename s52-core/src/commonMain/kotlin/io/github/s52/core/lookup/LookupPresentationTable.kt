package io.github.s52.core.lookup

import io.github.s52.catalog.PrimitiveType
import io.github.s52.core.settings.BoundaryStyle
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.SymbolStyle

/**
 * Presentation-table selector carried by OpenCPN lookup rows.
 *
 * OpenCPN's `chartsymbols.xml` splits point, area, and line lookup rows into
 * table-name families (`Simplified`, `Paper`, `Plain`, `Symbolized`, `Lines`).
 * The core lookup table keeps synthetic/generated packs compatible by treating
 * unknown or absent table names as [Any], but OpenCPN rows are filtered against
 * mariner symbol/boundary settings before attribute matching.
 */
enum class LookupPresentationTable {
    Any,
    Plain,
    Symbolized,
    Simplified,
    Paper,
    Lines,
    Unknown;

    fun matches(primitive: PrimitiveType, settings: MarinerSettings): Boolean = when (this) {
        Any -> true
        Unknown -> true
        Plain -> primitive == PrimitiveType.Area && settings.boundaryStyle == BoundaryStyle.Plain
        Symbolized -> primitive == PrimitiveType.Area && settings.boundaryStyle == BoundaryStyle.Symbolized
        Simplified -> primitive == PrimitiveType.Point && settings.symbolStyle == SymbolStyle.Simplified
        Paper -> primitive == PrimitiveType.Point && settings.symbolStyle == SymbolStyle.PaperChart
        Lines -> primitive == PrimitiveType.Line
    }

    companion object {
        fun parse(sourceTableName: String?): LookupPresentationTable {
            val normalized = sourceTableName?.trim()?.uppercase()?.replace(" ", "").orEmpty()
            return when (normalized) {
                "" -> Any
                "PLAIN" -> Plain
                "SYMBOLIZED" -> Symbolized
                "SIMPLIFIED" -> Simplified
                "PAPER", "PAPERCHART" -> Paper
                "LINES" -> Lines
                else -> Unknown
            }
        }
    }
}
