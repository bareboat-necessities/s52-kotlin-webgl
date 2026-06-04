package io.github.s52.core.model

import io.github.s52.catalog.PrimitiveType
import io.github.s52.core.geometry.EncGeometry

/** Boundary model for upstream parsers before catalogue validation. */
data class RawEncFeature(
    val id: Long,
    val objectClassAcronym: String,
    val primitive: PrimitiveType,
    val rawAttributes: Map<String, S57Value>,
    val geometry: EncGeometry,
    val objectClassCode: Int? = null,
    val scaleMin: Int? = null,
    val scaleMax: Int? = null
)

/**
 * Strict conversion helper for callers that want exception-on-error behavior.
 * Prefer [RawEncFeatureConverter.convert] when building import diagnostics for UI.
 */
fun RawEncFeature.toTypedFeature(): EncFeature = when (val result = RawEncFeatureConverter.convert(this)) {
    is FeatureConversionResult.Success -> result.feature
    is FeatureConversionResult.Failure -> error(result.message)
}
