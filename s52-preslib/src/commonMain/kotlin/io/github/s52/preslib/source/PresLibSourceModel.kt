package io.github.s52.preslib.source

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.lookup.AttributeFilter
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.S52Palette

/**
 * Source-side Presentation Library interchange model.
 *
 * It is not the official IHO source format; it is a compact internal handoff
 * format for generated data after an importer has decoded the official/local
 * Presentation Library package.
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
    val overRadar: Boolean = false,
    val attributeFilter: SourceAttributeFilter = SourceAttributeFilter.Any,
    val minimumDisplayScale: Double? = null,
    val maximumDisplayScale: Double? = null
)

/** Structural, generator-friendly lookup filter model. */
sealed interface SourceAttributeFilter {
    fun toRuntime(): AttributeFilter

    data object Any : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.Any
    }

    data class Exists(val attribute: S57Attribute) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.Exists(attribute)
    }

    data class Missing(val attribute: S57Attribute) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.Missing(attribute)
    }

    data class EqualsInt(val attribute: S57Attribute, val expected: Int) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.EqualsInt(attribute, expected)
    }

    data class IntIn(val attribute: S57Attribute, val expected: Set<Int>) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.IntIn(attribute, expected)
    }

    data class EqualsDecimal(
        val attribute: S57Attribute,
        val expected: Double,
        val tolerance: Double = 1.0e-9
    ) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.EqualsDecimal(attribute, expected, tolerance)
    }

    data class DecimalRange(
        val attribute: S57Attribute,
        val minInclusive: Double? = null,
        val maxInclusive: Double? = null
    ) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.DecimalRange(attribute, minInclusive, maxInclusive)
    }

    data class TextEquals(
        val attribute: S57Attribute,
        val expected: String,
        val ignoreCase: Boolean = false
    ) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.TextEquals(attribute, expected, ignoreCase)
    }

    data class TextIn(
        val attribute: S57Attribute,
        val expected: Set<String>,
        val ignoreCase: Boolean = false
    ) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.TextIn(attribute, expected, ignoreCase)
    }

    data class All(val filters: List<SourceAttributeFilter>) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.All(filters.map { it.toRuntime() })
    }

    data class AnyOf(val filters: List<SourceAttributeFilter>) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.AnyOf(filters.map { it.toRuntime() })
    }

    data class Not(val filter: SourceAttributeFilter) : SourceAttributeFilter {
        override fun toRuntime(): AttributeFilter = AttributeFilter.Not(filter.toRuntime())
    }
}
