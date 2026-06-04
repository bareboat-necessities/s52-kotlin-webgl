package io.github.s52.core.lookup

import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

class LookupTable(
    private val records: List<LookupRecord>
) {
    fun records(): List<LookupRecord> = records

    fun match(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<LookupRecord> {
        return records.filter { record ->
            record.objectClass == feature.objectClass &&
                record.primitive == feature.primitive &&
                record.attributeFilter.matches(feature) &&
                isVisibleAtScale(feature, settings, context)
        }
    }

    private fun isVisibleAtScale(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): Boolean {
        val scale = context.displayScale.takeIf { it > 0.0 } ?: settings.scale
        val minOk = feature.scaleMin?.let { scale <= it } ?: true
        val maxOk = feature.scaleMax?.let { scale >= it } ?: true
        return minOk && maxOk
    }
}
