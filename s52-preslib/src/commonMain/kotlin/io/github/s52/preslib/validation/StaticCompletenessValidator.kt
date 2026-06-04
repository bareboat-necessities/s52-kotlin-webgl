package io.github.s52.preslib.validation

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57AttributeValueKind
import io.github.s52.core.instruction.InstructionParser
import io.github.s52.core.instruction.InstructionReferenceCollector
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.settings.S52Palette
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.source.PresLibSourceNormalizer
import io.github.s52.preslib.source.PresLibSourcePack
import io.github.s52.preslib.source.SourceAttributeFilter
import io.github.s52.preslib.source.s52Token

/**
 * Phase 9 static-completeness diagnostic.
 *
 * The validator is intentionally renderer-independent. It proves that a
 * Presentation Library pack is self-consistent before a chart engine ever tries
 * to draw it.
 */
sealed interface StaticCompletenessDiagnostic {
    val message: String

    data class InvalidPrimitive(
        val rowIndex: Int,
        val objectClass: String,
        val primitive: PrimitiveType
    ) : StaticCompletenessDiagnostic {
        override val message: String =
            "Lookup row $rowIndex uses primitive $primitive, but $objectClass does not support it"
    }

    data class InstructionParseFailure(
        val rowIndex: Int,
        val instruction: String,
        val reason: String
    ) : StaticCompletenessDiagnostic {
        override val message: String = "Lookup row $rowIndex instruction did not parse: $reason :: $instruction"
    }

    data class MissingPalette(val palette: S52Palette) : StaticCompletenessDiagnostic {
        override val message: String = "Missing required palette $palette"
    }

    data class MissingColor(val token: String, val palette: S52Palette) : StaticCompletenessDiagnostic {
        override val message: String = "Missing color token $token in palette $palette"
    }

    data class MissingSymbol(val name: String) : StaticCompletenessDiagnostic {
        override val message: String = "Missing symbol $name"
    }

    data class MissingLineStyle(val name: String) : StaticCompletenessDiagnostic {
        override val message: String = "Missing line style $name"
    }

    data class MissingPattern(val name: String) : StaticCompletenessDiagnostic {
        override val message: String = "Missing pattern $name"
    }

    data class MissingCsp(val name: String) : StaticCompletenessDiagnostic {
        override val message: String = "Missing CSP $name"
    }

    data class IncompatibleAttributeFilter(
        val rowIndex: Int,
        val attribute: S57Attribute,
        val expected: String,
        val actual: S57AttributeValueKind
    ) : StaticCompletenessDiagnostic {
        override val message: String =
            "Lookup row $rowIndex filter uses ${attribute.acronym} as $expected, but catalogue kind is $actual"
    }

    data class DuplicateSourceName(
        val kind: String,
        val name: String
    ) : StaticCompletenessDiagnostic {
        override val message: String = "Duplicate $kind name $name"
    }

    data class InvalidColorRange(
        val token: String,
        val r: Int,
        val g: Int,
        val b: Int
    ) : StaticCompletenessDiagnostic {
        override val message: String = "Color $token has RGB values outside 0..255: $r,$g,$b"
    }
}

data class StaticCompletenessReport(
    val diagnostics: List<StaticCompletenessDiagnostic>,
    val lookupRecordCount: Int,
    val instructionCount: Int,
    val symbolCount: Int,
    val lineStyleCount: Int,
    val patternCount: Int,
    val paletteCount: Int,
    val referencedSymbols: Set<String>,
    val referencedLineStyles: Set<String>,
    val referencedPatterns: Set<String>,
    val referencedColors: Set<String>,
    val referencedCsps: Set<String>,
    val implementedCsps: Set<String> = emptySet()
) {
    val hasErrors: Boolean get() = diagnostics.isNotEmpty()
    val missingCsps: Set<String> get() = referencedCsps - implementedCsps

    fun toMarkdown(): String = buildString {
        appendLine("# Static Completeness Report")
        appendLine()
        appendLine("- Lookup records: $lookupRecordCount")
        appendLine("- Parsed instructions: $instructionCount")
        appendLine("- Symbols: $symbolCount")
        appendLine("- Line styles: $lineStyleCount")
        appendLine("- Patterns: $patternCount")
        appendLine("- Palettes: $paletteCount")
        appendLine("- Referenced symbols: ${referencedSymbols.size}")
        appendLine("- Referenced line styles: ${referencedLineStyles.size}")
        appendLine("- Referenced patterns: ${referencedPatterns.size}")
        appendLine("- Referenced colors: ${referencedColors.size}")
        appendLine("- Referenced CSPs: ${referencedCsps.size}")
        if (implementedCsps.isNotEmpty()) appendLine("- Implemented CSPs: ${implementedCsps.size}")
        appendLine("- Diagnostics: ${diagnostics.size}")
        if (diagnostics.isNotEmpty()) {
            appendLine()
            appendLine("## Diagnostics")
            diagnostics.forEach { appendLine("- ${it.message}") }
        }
    }
}

/**
 * Phase 9 static-completeness validator.
 *
 * For runtime packs, it validates already parsed lookup records. For source
 * packs, it also validates instruction parsing and source-side duplicates.
 */
object StaticCompletenessValidator {
    fun validatePack(
        pack: PresLibPack,
        implementedCsps: Set<String> = emptySet(),
        requiredPalettes: Set<S52Palette> = S52Palette.entries.toSet()
    ): StaticCompletenessReport {
        val records = pack.lookupTable.records()
        val instructions = records.flatMap { it.instructions }
        val references = InstructionReferenceCollector.collect(instructions).normalized()
        val implemented = implementedCsps.mapTo(mutableSetOf()) { it.s52Token() }

        val diagnostics = mutableListOf<StaticCompletenessDiagnostic>()
        records.forEachIndexed { row, record -> validateRuntimeLookupRow(row, record, diagnostics) }
        validateRuntimeReferences(pack, references, implemented, requiredPalettes, diagnostics)

        return StaticCompletenessReport(
            diagnostics = diagnostics.sortedBy { it.message },
            lookupRecordCount = records.size,
            instructionCount = instructions.size,
            symbolCount = pack.symbols.names().size,
            lineStyleCount = pack.lineStyles.names().size,
            patternCount = pack.patterns.names().size,
            paletteCount = S52Palette.entries.count { pack.colors.tokens(it).isNotEmpty() },
            referencedSymbols = references.symbols,
            referencedLineStyles = references.lineStyles,
            referencedPatterns = references.patterns,
            referencedColors = references.colorTokens,
            referencedCsps = references.csps,
            implementedCsps = implemented
        )
    }

    fun validateSource(
        source: PresLibSourcePack,
        implementedCsps: Set<String> = emptySet(),
        requiredPalettes: Set<S52Palette> = S52Palette.entries.toSet()
    ): StaticCompletenessReport {
        val normalized = PresLibSourceNormalizer.normalize(source)
        val diagnostics = mutableListOf<StaticCompletenessDiagnostic>()

        diagnostics += PresLibValidator.validateSource(source).map { it.toStaticDiagnostic() }

        val instructions = mutableListOf<S52Instruction>()
        normalized.lookupRecords.forEachIndexed { row, record ->
            validateSourceLookupRow(row, record.objectClass.acronym, record.primitive, record.attributeFilter, diagnostics)
            try {
                instructions += InstructionParser.parseSequenceDetailed(record.instruction).ast()
            } catch (t: Throwable) {
                diagnostics += StaticCompletenessDiagnostic.InstructionParseFailure(
                    rowIndex = row,
                    instruction = record.instruction,
                    reason = t.message ?: t::class.simpleName.orEmpty()
                )
            }
        }

        val references = InstructionReferenceCollector.collect(instructions).normalized()
        val implemented = implementedCsps.mapTo(mutableSetOf()) { it.s52Token() }
        validateSourceReferences(normalized, references, implemented, requiredPalettes, diagnostics)

        return StaticCompletenessReport(
            diagnostics = diagnostics.sortedBy { it.message },
            lookupRecordCount = normalized.lookupRecords.size,
            instructionCount = instructions.size,
            symbolCount = normalized.symbols.size,
            lineStyleCount = normalized.lineStyles.size,
            patternCount = normalized.patterns.size,
            paletteCount = normalized.colorTables.count { it.colors.isNotEmpty() },
            referencedSymbols = references.symbols,
            referencedLineStyles = references.lineStyles,
            referencedPatterns = references.patterns,
            referencedColors = references.colorTokens,
            referencedCsps = references.csps,
            implementedCsps = implemented
        )
    }

    private fun validateRuntimeLookupRow(
        row: Int,
        record: LookupRecord,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        if (record.primitive !in record.objectClass.primitives) {
            diagnostics += StaticCompletenessDiagnostic.InvalidPrimitive(row, record.objectClass.acronym, record.primitive)
        }
    }

    private fun validateSourceLookupRow(
        row: Int,
        objectClass: String,
        primitive: PrimitiveType,
        filter: SourceAttributeFilter,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        val cls = io.github.s52.catalog.S57ObjectClass.fromAcronym(objectClass)
        if (cls != null && primitive !in cls.primitives) {
            diagnostics += StaticCompletenessDiagnostic.InvalidPrimitive(row, objectClass, primitive)
        }
        validateAttributeFilter(row, filter, diagnostics)
    }

    private fun validateAttributeFilter(
        row: Int,
        filter: SourceAttributeFilter,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        when (filter) {
            SourceAttributeFilter.Any -> Unit
            is SourceAttributeFilter.Exists -> Unit
            is SourceAttributeFilter.Missing -> Unit
            is SourceAttributeFilter.EqualsInt -> validateIntegerLike(row, filter.attribute, diagnostics)
            is SourceAttributeFilter.IntIn -> validateIntegerLike(row, filter.attribute, diagnostics)
            is SourceAttributeFilter.EqualsDecimal -> validateDecimalLike(row, filter.attribute, diagnostics)
            is SourceAttributeFilter.DecimalRange -> validateDecimalLike(row, filter.attribute, diagnostics)
            is SourceAttributeFilter.TextEquals -> validateTextLike(row, filter.attribute, diagnostics)
            is SourceAttributeFilter.TextIn -> validateTextLike(row, filter.attribute, diagnostics)
            is SourceAttributeFilter.All -> filter.filters.forEach { validateAttributeFilter(row, it, diagnostics) }
            is SourceAttributeFilter.AnyOf -> filter.filters.forEach { validateAttributeFilter(row, it, diagnostics) }
            is SourceAttributeFilter.Not -> validateAttributeFilter(row, filter.filter, diagnostics)
        }
    }

    private fun validateIntegerLike(
        row: Int,
        attribute: S57Attribute,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        if (attribute.valueKind !in setOf(
                S57AttributeValueKind.Integer,
                S57AttributeValueKind.Enumeration,
                S57AttributeValueKind.EnumerationList,
                S57AttributeValueKind.Unknown
            )
        ) {
            diagnostics += StaticCompletenessDiagnostic.IncompatibleAttributeFilter(
                row,
                attribute,
                "integer/enumeration",
                attribute.valueKind
            )
        }
    }

    private fun validateDecimalLike(
        row: Int,
        attribute: S57Attribute,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        if (attribute.valueKind !in setOf(S57AttributeValueKind.Decimal, S57AttributeValueKind.Integer, S57AttributeValueKind.Unknown)) {
            diagnostics += StaticCompletenessDiagnostic.IncompatibleAttributeFilter(row, attribute, "decimal", attribute.valueKind)
        }
    }

    private fun validateTextLike(
        row: Int,
        attribute: S57Attribute,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        if (attribute.valueKind !in setOf(S57AttributeValueKind.Text, S57AttributeValueKind.Unknown)) {
            diagnostics += StaticCompletenessDiagnostic.IncompatibleAttributeFilter(row, attribute, "text", attribute.valueKind)
        }
    }

    private fun validateRuntimeReferences(
        pack: PresLibPack,
        references: NormalizedReferences,
        implementedCsps: Set<String>,
        requiredPalettes: Set<S52Palette>,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        for (palette in requiredPalettes) {
            val tokens = pack.colors.tokens(palette)
            if (tokens.isEmpty()) diagnostics += StaticCompletenessDiagnostic.MissingPalette(palette)
            references.colorTokens.forEach { token ->
                if (token !in tokens) diagnostics += StaticCompletenessDiagnostic.MissingColor(token, palette)
            }
        }
        references.symbols.forEach { if (pack.symbols.find(it) == null) diagnostics += StaticCompletenessDiagnostic.MissingSymbol(it) }
        references.lineStyles.forEach { if (pack.lineStyles.find(it) == null) diagnostics += StaticCompletenessDiagnostic.MissingLineStyle(it) }
        references.patterns.forEach { if (pack.patterns.find(it) == null) diagnostics += StaticCompletenessDiagnostic.MissingPattern(it) }
        references.csps.forEach { if (implementedCsps.isNotEmpty() && it !in implementedCsps) diagnostics += StaticCompletenessDiagnostic.MissingCsp(it) }
    }

    private fun validateSourceReferences(
        source: PresLibSourcePack,
        references: NormalizedReferences,
        implementedCsps: Set<String>,
        requiredPalettes: Set<S52Palette>,
        diagnostics: MutableList<StaticCompletenessDiagnostic>
    ) {
        val symbols = source.symbols.mapTo(mutableSetOf()) { it.name.s52Token() }
        val lineStyles = source.lineStyles.mapTo(mutableSetOf()) { it.name.s52Token() }
        val patterns = source.patterns.mapTo(mutableSetOf()) { it.name.s52Token() }
        val colorTables = source.colorTables.associate { table -> table.palette to table.colors.mapTo(mutableSetOf()) { it.token.s52Token() } }

        for (palette in requiredPalettes) {
            val tokens = colorTables[palette]
            if (tokens == null || tokens.isEmpty()) diagnostics += StaticCompletenessDiagnostic.MissingPalette(palette)
            references.colorTokens.forEach { token ->
                if (tokens == null || token !in tokens) diagnostics += StaticCompletenessDiagnostic.MissingColor(token, palette)
            }
        }
        references.symbols.forEach { if (it !in symbols) diagnostics += StaticCompletenessDiagnostic.MissingSymbol(it) }
        references.lineStyles.forEach { if (it !in lineStyles) diagnostics += StaticCompletenessDiagnostic.MissingLineStyle(it) }
        references.patterns.forEach { if (it !in patterns) diagnostics += StaticCompletenessDiagnostic.MissingPattern(it) }
        references.csps.forEach { if (implementedCsps.isNotEmpty() && it !in implementedCsps) diagnostics += StaticCompletenessDiagnostic.MissingCsp(it) }
    }

    private fun PresLibValidationDiagnostic.toStaticDiagnostic(): StaticCompletenessDiagnostic = when (this) {
        is PresLibValidationDiagnostic.MissingPalette -> StaticCompletenessDiagnostic.MissingPalette(palette)
        is PresLibValidationDiagnostic.MissingColor -> StaticCompletenessDiagnostic.MissingColor(token.s52Token(), palette)
        is PresLibValidationDiagnostic.MissingSymbol -> StaticCompletenessDiagnostic.MissingSymbol(name.s52Token())
        is PresLibValidationDiagnostic.MissingLineStyle -> StaticCompletenessDiagnostic.MissingLineStyle(name.s52Token())
        is PresLibValidationDiagnostic.MissingPattern -> StaticCompletenessDiagnostic.MissingPattern(name.s52Token())
        is PresLibValidationDiagnostic.DuplicateSourceName -> StaticCompletenessDiagnostic.DuplicateSourceName(kind, name.s52Token())
        is PresLibValidationDiagnostic.InvalidColorRange -> StaticCompletenessDiagnostic.InvalidColorRange(token.s52Token(), r, g, b)
    }

    private fun io.github.s52.core.instruction.InstructionReferences.normalized(): NormalizedReferences = NormalizedReferences(
        symbols = symbols.mapTo(mutableSetOf()) { it.s52Token() },
        lineStyles = lineStyles.mapTo(mutableSetOf()) { it.s52Token() },
        patterns = patterns.mapTo(mutableSetOf()) { it.s52Token() },
        colorTokens = colorTokens.mapTo(mutableSetOf()) { it.s52Token() },
        csps = csps.mapTo(mutableSetOf()) { it.s52Token() }
    )
}

private data class NormalizedReferences(
    val symbols: Set<String>,
    val lineStyles: Set<String>,
    val patterns: Set<String>,
    val colorTokens: Set<String>,
    val csps: Set<String>
)
