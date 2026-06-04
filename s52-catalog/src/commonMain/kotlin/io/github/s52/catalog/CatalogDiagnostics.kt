package io.github.s52.catalog

data class CatalogDiagnostic(
    val severity: Severity,
    val message: String
) {
    enum class Severity { Info, Warning, Error }
}

data class CatalogValidationReport(
    val objectClassCount: Int,
    val attributeCount: Int,
    val enumeratedValueCount: Int,
    val diagnostics: List<CatalogDiagnostic>
) {
    val hasErrors: Boolean = diagnostics.any { it.severity == CatalogDiagnostic.Severity.Error }
}

object S57CatalogValidator {
    fun validate(): CatalogValidationReport {
        val diagnostics = mutableListOf<CatalogDiagnostic>()
        reportDuplicateAcronyms("object class", S57ObjectClass.entries.map { it.acronym }, diagnostics)
        reportDuplicateAcronyms("attribute", S57Attribute.entries.map { it.acronym }, diagnostics)
        reportDuplicateCodes("object class", S57ObjectClass.entries.mapNotNull { it.code }, diagnostics)
        reportDuplicateCodes("attribute", S57Attribute.entries.mapNotNull { it.code }, diagnostics)
        reportDuplicateEnumValues(diagnostics)

        S57ObjectClass.entries.forEach { objectClass ->
            if (objectClass.primitives.isEmpty()) {
                diagnostics += CatalogDiagnostic(
                    CatalogDiagnostic.Severity.Error,
                    "${objectClass.acronym} has no supported primitive types"
                )
            }
        }

        return CatalogValidationReport(
            objectClassCount = S57ObjectClass.entries.size,
            attributeCount = S57Attribute.entries.size,
            enumeratedValueCount = S57EnumeratedValue.entries.size,
            diagnostics = diagnostics
        )
    }

    private fun reportDuplicateAcronyms(
        kind: String,
        values: List<String>,
        diagnostics: MutableList<CatalogDiagnostic>
    ) {
        values.groupBy { it }.filterValues { it.size > 1 }.forEach { (value, matches) ->
            diagnostics += CatalogDiagnostic(
                CatalogDiagnostic.Severity.Error,
                "Duplicate S-57 $kind acronym '$value' appears ${matches.size} times"
            )
        }
    }

    private fun reportDuplicateCodes(
        kind: String,
        values: List<Int>,
        diagnostics: MutableList<CatalogDiagnostic>
    ) {
        values.groupBy { it }.filterValues { it.size > 1 }.forEach { (value, matches) ->
            diagnostics += CatalogDiagnostic(
                CatalogDiagnostic.Severity.Error,
                "Duplicate S-57 $kind code '$value' appears ${matches.size} times"
            )
        }
    }

    private fun reportDuplicateEnumValues(diagnostics: MutableList<CatalogDiagnostic>) {
        S57EnumeratedValue.entries
            .groupBy { it.attribute to it.code }
            .filterValues { it.size > 1 }
            .forEach { (key, matches) ->
                diagnostics += CatalogDiagnostic(
                    CatalogDiagnostic.Severity.Error,
                    "Duplicate enumerated value ${key.second} for ${key.first.acronym} appears ${matches.size} times"
                )
            }
    }
}
