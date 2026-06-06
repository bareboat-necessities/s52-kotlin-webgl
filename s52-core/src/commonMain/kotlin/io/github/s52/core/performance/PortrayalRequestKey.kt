package io.github.s52.core.performance

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57AttributeKey
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/**
 * Stable content key for cached portrayal requests.
 *
 * The key intentionally uses the normalized feature content, settings, and
 * portrayal context rather than object identity. This keeps cache hits correct
 * when callers rebuild equivalent feature lists between frames.
 */
data class PortrayalRequestKey(
    val featureCount: Int,
    val featureHash: Int,
    val settingsHash: Int,
    val contextHash: Int
) {
    companion object {
        fun from(
            features: List<EncFeature>,
            settings: MarinerSettings,
            context: PortrayalContext
        ): PortrayalRequestKey = PortrayalRequestKey(
            featureCount = features.size,
            featureHash = features.fold(1) { acc, feature -> 31 * acc + feature.stableHash() },
            settingsHash = settings.stableHash(),
            contextHash = context.stableHash()
        )
    }
}

private fun EncFeature.stableHash(): Int = hashMany(
    id.hashCode(),
    objectClassKey.acronym.hashCode(),
    primitive.name.hashCode(),
    attributes.asKeyMap().stableHash(),
    geometry.stableHash(),
    scaleMin ?: 0,
    scaleMax ?: 0
)

private fun Map<S57AttributeKey, S57Value>.stableHash(): Int = entries
    .sortedBy { it.key.acronym }
    .fold(1) { acc, entry ->
        31 * acc + hashMany(entry.key.acronym.hashCode(), entry.value.stableHash())
    }

private fun S57Value.stableHash(): Int = when (this) {
    is S57Value.Integer -> hashMany(1, value)
    is S57Value.Decimal -> hashMany(2, value.hashCode())
    is S57Value.Text -> hashMany(3, value.hashCode())
    is S57Value.ListValue -> values.fold(4) { acc, value -> 31 * acc + value.stableHash() }
    S57Value.Empty -> 5
}

private fun EncGeometry.stableHash(): Int = when (this) {
    is EncGeometry.Point -> hashMany(1, coordinate.stableHash())
    is EncGeometry.MultiPoint -> coordinates.fold(2) { acc, coordinate -> 31 * acc + coordinate.stableHash() }
    is EncGeometry.LineString -> coordinates.fold(3) { acc, coordinate -> 31 * acc + coordinate.stableHash() }
    is EncGeometry.Polygon -> hashMany(
        4,
        outer.fold(1) { acc, coordinate -> 31 * acc + coordinate.stableHash() },
        holes.fold(1) { acc, ring -> 31 * acc + ring.fold(1) { ringAcc, coordinate -> 31 * ringAcc + coordinate.stableHash() } }
    )
}

private fun Coordinate.stableHash(): Int = hashMany(
    lon.hashCode(),
    lat.hashCode(),
    z?.hashCode() ?: 0
)

private fun MarinerSettings.stableHash(): Int = hashMany(
    displayCategory.name.hashCode(),
    palette.name.hashCode(),
    symbolStyle.name.hashCode(),
    boundaryStyle.name.hashCode(),
    safetyDepthMeters.hashCode(),
    safetyContourMeters.hashCode(),
    shallowContourMeters.hashCode(),
    deepContourMeters.hashCode(),
    showText.hashCode(),
    showSoundings.hashCode(),
    showLightDescriptions.hashCode(),
    scale.hashCode(),
    enabledViewingGroups?.sorted()?.fold(1) { acc, value -> 31 * acc + value } ?: 0,
    disabledViewingGroups.sorted().fold(1) { acc, value -> 31 * acc + value }
)

private fun PortrayalContext.stableHash(): Int = hashMany(
    compilationScale.hashCode(),
    displayScale.hashCode(),
    viewportId.hashCode()
)

private fun hashMany(vararg values: Int): Int = values.fold(1) { acc, value -> 31 * acc + value }
