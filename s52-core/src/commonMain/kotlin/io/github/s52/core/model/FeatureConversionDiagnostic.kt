package io.github.s52.core.model

data class FeatureConversionDiagnostic(
    val severity: Severity,
    val featureId: Long,
    val message: String
) {
    enum class Severity { Warning, Error }
}

sealed interface FeatureConversionResult {
    data class Success(
        val feature: EncFeature,
        val diagnostics: List<FeatureConversionDiagnostic> = emptyList()
    ) : FeatureConversionResult

    data class Failure(
        val diagnostics: List<FeatureConversionDiagnostic>
    ) : FeatureConversionResult {
        val message: String = diagnostics.joinToString(separator = "; ") { it.message }
    }
}
