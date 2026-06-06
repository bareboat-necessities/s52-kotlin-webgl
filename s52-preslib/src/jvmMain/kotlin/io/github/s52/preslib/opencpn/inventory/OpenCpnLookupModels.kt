package io.github.s52.preslib.opencpn.inventory

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57AttributeKey
import io.github.s52.catalog.S57ObjectClassKey

/** Raw OpenCPN lookup row parsed from chartsymbols.xml. */
data class OpenCpnRawLookupRecord(
    val id: String?,
    val rcid: String?,
    val objectClassKey: S57ObjectClassKey,
    val primitive: PrimitiveType,
    val displayPriorityLabel: String,
    val displayPriority: Int,
    val radarPriority: OpenCpnRadarPriority,
    val tableName: OpenCpnLookupTableName,
    val attribCodes: List<String>,
    val attributeFilter: OpenCpnAttributeFilter,
    val instruction: String,
    val displayCategory: OpenCpnDisplayCategory,
    val viewingGroup: Int?,
    val comment: String,
    val instructionRefs: OpenCpnInstructionRefs
)

enum class OpenCpnLookupTableName {
    Plain,
    Symbolized,
    Simplified,
    Paper,
    Lines,
    Unknown;

    companion object {
        fun parse(value: String): OpenCpnLookupTableName = when (value.trim().uppercase()) {
            "PLAIN" -> Plain
            "SYMBOLIZED" -> Symbolized
            "SIMPLIFIED" -> Simplified
            "PAPER" -> Paper
            "LINES" -> Lines
            else -> Unknown
        }
    }
}

enum class OpenCpnRadarPriority {
    Suppressed,
    OnTop,
    OverRadar,
    Unknown;

    companion object {
        fun parse(value: String): OpenCpnRadarPriority = when (value.trim().uppercase().replace(" ", "")) {
            "SUPPRESSED" -> Suppressed
            "ONTOP" -> OnTop
            "OVERRADAR" -> OverRadar
            else -> Unknown
        }
    }
}

enum class OpenCpnDisplayCategory {
    DisplayBase,
    Standard,
    Other,
    Mariners,
    Unknown;

    companion object {
        fun parse(value: String): OpenCpnDisplayCategory = when (value.trim().uppercase()) {
            "DISPLAYBASE", "DISPLAY BASE", "BASE" -> DisplayBase
            "STANDARD" -> Standard
            "OTHER" -> Other
            "MARINERS", "MARINER" -> Mariners
            else -> Unknown
        }
    }
}

data class OpenCpnInstructionRefs(
    val symbols: Set<String> = emptySet(),
    val lineStyles: Set<String> = emptySet(),
    val patterns: Set<String> = emptySet(),
    val cspNames: Set<String> = emptySet()
) {
    val isEmpty: Boolean get() = symbols.isEmpty() && lineStyles.isEmpty() && patterns.isEmpty() && cspNames.isEmpty()
}

sealed interface OpenCpnAttributeFilter {
    val description: String

    data object Any : OpenCpnAttributeFilter {
        override val description: String = "*"
    }

    data class Exists(val attribute: S57AttributeKey) : OpenCpnAttributeFilter {
        override val description: String = "${attribute.acronym} exists"
    }

    data class EqualsInt(val attribute: S57AttributeKey, val expected: Int) : OpenCpnAttributeFilter {
        override val description: String = "${attribute.acronym} == $expected"
    }

    data class IntIn(val attribute: S57AttributeKey, val expected: Set<Int>) : OpenCpnAttributeFilter {
        override val description: String = "${attribute.acronym} in ${expected.sorted()}"
    }

    data class All(val filters: List<OpenCpnAttributeFilter>) : OpenCpnAttributeFilter {
        override val description: String = filters.joinToString(separator = " AND ") { it.description }
    }

    data class Unsupported(val raw: String, val reason: String) : OpenCpnAttributeFilter {
        override val description: String = "unsupported($raw: $reason)"
    }
}

data class OpenCpnLookupDiagnostics(
    val lookupCount: Int,
    val unknownObjectNames: Set<String>,
    val unknownAttributeNames: Set<String>,
    val unsupportedAttribCodes: List<String>,
    val unresolvedSymbolRefs: Set<String>,
    val unresolvedLineStyleRefs: Set<String>,
    val unresolvedPatternRefs: Set<String>,
    val cspNames: Set<String>,
    val tableNames: Set<OpenCpnLookupTableName>,
    val displayCategories: Set<OpenCpnDisplayCategory>
) {
    fun toHumanText(): String = buildString {
        appendLine("OpenCPN lookup diagnostics:")
        appendLine("  lookups=$lookupCount")
        appendLine("  unknownObjectNames=${unknownObjectNames.size}")
        appendLine("  unknownAttributeNames=${unknownAttributeNames.size}")
        appendLine("  unsupportedAttribCodes=${unsupportedAttribCodes.size}")
        appendLine("  unresolvedSymbolRefs=${unresolvedSymbolRefs.size}")
        appendLine("  unresolvedLineStyleRefs=${unresolvedLineStyleRefs.size}")
        appendLine("  unresolvedPatternRefs=${unresolvedPatternRefs.size}")
        appendLine("  cspNames=${cspNames.size}")
        appendLine("  tableNames=${tableNames.sortedBy { it.name }}")
        appendLine("  displayCategories=${displayCategories.sortedBy { it.name }}")
        if (unknownObjectNames.isNotEmpty()) appendLine("  unknownObjectSample=${unknownObjectNames.take(20)}")
        if (unknownAttributeNames.isNotEmpty()) appendLine("  unknownAttributeSample=${unknownAttributeNames.take(20)}")
        if (unsupportedAttribCodes.isNotEmpty()) appendLine("  unsupportedAttribCodeSample=${unsupportedAttribCodes.take(20)}")
        if (cspNames.isNotEmpty()) appendLine("  cspNameSample=${cspNames.sorted().take(30)}")
    }
}
