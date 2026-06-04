package io.github.s52.catalog

data class CatalogDiagnostic(
    val severity: Severity,
    val message: String
) {
    enum class Severity { Info, Warning, Error }
}
