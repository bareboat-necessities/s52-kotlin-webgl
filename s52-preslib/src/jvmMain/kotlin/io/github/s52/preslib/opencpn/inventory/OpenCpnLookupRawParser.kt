package io.github.s52.preslib.opencpn.inventory

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57AttributeKey
import io.github.s52.catalog.S57ObjectClassKey
import org.w3c.dom.Element

/** Parser for OpenCPN lookup rows and attrib-code columns in chartsymbols.xml. */
object OpenCpnLookupRawParser {
    fun parseLookups(root: Element): List<OpenCpnRawLookupRecord> =
        root.getElementsByTagName("lookup").asElements().mapIndexed { index, lookup ->
            val objectName = lookup.getAttribute("name").trim().ifBlank { "######" }
            val typeText = lookup.childText("type").orEmpty()
            val displayPriorityLabel = lookup.childText("disp-prio").orEmpty()
            val attribCodes = lookup.childTexts("attrib-code")
            val instruction = lookup.childText("instruction").orEmpty()
            val comment = lookup.childText("comment").orEmpty()
            OpenCpnRawLookupRecord(
                id = lookup.getAttribute("id").takeIf { it.isNotBlank() } ?: index.toString(),
                rcid = lookup.getAttribute("RCID").takeIf { it.isNotBlank() },
                objectClassKey = S57ObjectClassKey.of(objectName),
                primitive = parsePrimitive(typeText),
                displayPriorityLabel = displayPriorityLabel,
                displayPriority = parseDisplayPriority(displayPriorityLabel),
                radarPriority = OpenCpnRadarPriority.parse(lookup.childText("radar-prio").orEmpty()),
                tableName = OpenCpnLookupTableName.parse(lookup.childText("table-name").orEmpty()),
                attribCodes = attribCodes,
                attributeFilter = OpenCpnAttribCodeParser.parseAll(attribCodes),
                instruction = instruction,
                displayCategory = OpenCpnDisplayCategory.parse(lookup.childText("display-cat").orEmpty()),
                viewingGroup = comment.toIntOrNull(),
                comment = comment,
                instructionRefs = parseInstructionRefs(instruction)
            )
        }

    fun diagnostics(
        summary: OpenCpnChartSymbolsSummary,
        catalog: OpenCpnCsvCatalogSummary = OpenCpnCsvCatalogSummary()
    ): OpenCpnLookupDiagnostics {
        val symbolNames = summary.symbols.map { it.name.uppercase() }.toSet()
        val lineStyleNames = summary.lineStyles.map { it.name.uppercase() }.toSet()
        val patternNames = summary.patterns.map { it.name.uppercase() }.toSet()
        val catalogObjectNames = catalog.objectClasses.map { it.acronym.uppercase() }.toSet()
        val catalogAttributeNames = catalog.attributes.map { it.acronym.uppercase() }.toSet()

        val unknownObjects = summary.lookups
            .map { it.objectClassKey.acronym }
            .filter { it !in catalogObjectNames && !it.startsWith("\$") && it != "######" }
            .toSortedSet()

        val attributeKeys = summary.lookups.flatMap { it.attributeFilter.collectAttributeKeys() }
        val unknownAttributes = attributeKeys
            .map { it.acronym }
            .filter { it !in catalogAttributeNames }
            .toSortedSet()

        val unsupported = summary.lookups
            .flatMap { lookup -> lookup.attributeFilter.collectUnsupported().map { "${lookup.objectClassKey.acronym}:$it" } }

        return OpenCpnLookupDiagnostics(
            lookupCount = summary.lookups.size,
            unknownObjectNames = unknownObjects,
            unknownAttributeNames = unknownAttributes,
            unsupportedAttribCodes = unsupported,
            unresolvedSymbolRefs = summary.lookups.flatMap { it.instructionRefs.symbols }.filter { it.uppercase() !in symbolNames }.toSortedSet(),
            unresolvedLineStyleRefs = summary.lookups.flatMap { it.instructionRefs.lineStyles }.filter { it.uppercase() !in lineStyleNames }.toSortedSet(),
            unresolvedPatternRefs = summary.lookups.flatMap { it.instructionRefs.patterns }.filter { it.uppercase() !in patternNames }.toSortedSet(),
            cspNames = summary.lookups.flatMap { it.instructionRefs.cspNames }.toSortedSet(),
            tableNames = summary.lookups.map { it.tableName }.toSet(),
            displayCategories = summary.lookups.map { it.displayCategory }.toSet()
        )
    }

    private fun parsePrimitive(text: String): PrimitiveType = when (text.trim().uppercase()) {
        "POINT" -> PrimitiveType.Point
        "LINE" -> PrimitiveType.Line
        "AREA" -> PrimitiveType.Area
        else -> PrimitiveType.Point
    }

    private fun parseDisplayPriority(text: String): Int = when (text.trim().uppercase()) {
        "NO DATA" -> 0
        "GROUP 1" -> 1
        "AREA 1" -> 2
        "AREA 2" -> 3
        "AREA SYMBOL" -> 4
        "LINE SYMBOL" -> 5
        "POINT SYMBOL" -> 6
        "MARINERS" -> 7
        "ROUTING" -> 8
        "HAZARDS" -> 9
        else -> 0
    }

    private fun parseInstructionRefs(instruction: String): OpenCpnInstructionRefs = OpenCpnInstructionRefs(
        symbols = instruction.extractInstructionNames("SY"),
        lineStyles = instruction.extractInstructionNames("LC"),
        patterns = instruction.extractInstructionNames("AP"),
        cspNames = instruction.extractInstructionNames("CS")
    )

    private fun String.extractInstructionNames(opcode: String): Set<String> {
        val regex = Regex("(?:^|[;\\s])${Regex.escape(opcode)}\\(([^),;]+)")
        return regex.findAll(this)
            .map { it.groupValues[1].trim().trim('\'', '"') }
            .filter { it.isNotBlank() }
            .map { it.uppercase() }
            .toSortedSet()
    }

    private fun OpenCpnAttributeFilter.collectAttributeKeys(): List<S57AttributeKey> = when (this) {
        OpenCpnAttributeFilter.Any -> emptyList()
        is OpenCpnAttributeFilter.Exists -> listOf(attribute)
        is OpenCpnAttributeFilter.EqualsInt -> listOf(attribute)
        is OpenCpnAttributeFilter.IntIn -> listOf(attribute)
        is OpenCpnAttributeFilter.All -> filters.flatMap { it.collectAttributeKeys() }
        is OpenCpnAttributeFilter.Unsupported -> emptyList()
    }

    private fun OpenCpnAttributeFilter.collectUnsupported(): List<String> = when (this) {
        OpenCpnAttributeFilter.Any -> emptyList()
        is OpenCpnAttributeFilter.Exists -> emptyList()
        is OpenCpnAttributeFilter.EqualsInt -> emptyList()
        is OpenCpnAttributeFilter.IntIn -> emptyList()
        is OpenCpnAttributeFilter.All -> filters.flatMap { it.collectUnsupported() }
        is OpenCpnAttributeFilter.Unsupported -> listOf("$raw ($reason)")
    }

    private fun Element.childText(name: String): String? = childElements(name).firstOrNull()?.textContent?.trim()?.takeIf { it.isNotBlank() }

    private fun Element.childTexts(name: String): List<String> = childElements(name).mapNotNull { it.textContent?.trim()?.takeIf(String::isNotBlank) }

    private fun Element.childElements(name: String): List<Element> = childNodes.asElements().filter { it.tagName == name }

    private fun org.w3c.dom.NodeList.asElements(): List<Element> {
        val result = mutableListOf<Element>()
        for (i in 0 until length) (item(i) as? Element)?.let(result::add)
        return result
    }
}

object OpenCpnAttribCodeParser {
    /**
     * Parses common OpenCPN lookup attrib-code forms:
     * CATACH8, COLOUR3,1, DRVAL1?, CONDTN, fnctnm5, cattml3.
     */
    fun parseAll(codes: List<String>): OpenCpnAttributeFilter = when {
        codes.isEmpty() -> OpenCpnAttributeFilter.Any
        codes.size == 1 -> parse(codes.single())
        else -> OpenCpnAttributeFilter.All(codes.map(::parse))
    }

    fun parse(raw: String): OpenCpnAttributeFilter {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return OpenCpnAttributeFilter.Any

        val existsOptional = Regex("^([A-Za-z0-9_\\$]+)\\?$" ).matchEntire(trimmed)
        if (existsOptional != null) {
            return OpenCpnAttributeFilter.Exists(S57AttributeKey.of(normalizeAttributeName(existsOptional.groupValues[1])))
        }

        val equalsOrList = Regex("^([A-Za-z_\\$][A-Za-z0-9_\\$]*)([0-9]+(?:,[0-9]+)*)$" ).matchEntire(trimmed)
        if (equalsOrList != null) {
            val attribute = S57AttributeKey.of(normalizeAttributeName(equalsOrList.groupValues[1]))
            val values = equalsOrList.groupValues[2].split(',').mapNotNull { it.toIntOrNull() }.toSet()
            return when (values.size) {
                0 -> OpenCpnAttributeFilter.Unsupported(trimmed, "no integer values")
                1 -> OpenCpnAttributeFilter.EqualsInt(attribute, values.single())
                else -> OpenCpnAttributeFilter.IntIn(attribute, values)
            }
        }

        val exists = Regex("^[A-Za-z_\\$][A-Za-z0-9_\\$]*$" ).matchEntire(trimmed)
        if (exists != null) {
            return OpenCpnAttributeFilter.Exists(S57AttributeKey.of(normalizeAttributeName(trimmed)))
        }

        return OpenCpnAttributeFilter.Unsupported(trimmed, "unrecognized attrib-code syntax")
    }

    private fun normalizeAttributeName(raw: String): String {
        val upper = raw.trim().uppercase()
        return OpenCpnAttributeAliases[upper] ?: upper
    }

    private val OpenCpnAttributeAliases: Map<String, String> = mapOf(
        "FNCTNM" to "FUNCTN",
        "FUNCTNM" to "FUNCTN",
        "CATTRK" to "CATTRK",
        "CATTML" to "CATTRK",
        "SCODE" to "SCODE"
    )
}
