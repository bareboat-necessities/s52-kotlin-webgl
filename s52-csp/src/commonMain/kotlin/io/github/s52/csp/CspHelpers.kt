package io.github.s52.csp

import io.github.s52.catalog.S57Attribute
import io.github.s52.core.model.EncFeature
import kotlin.math.abs
import kotlin.math.roundToInt

internal object CspHelpers {
    fun depth(feature: EncFeature): Double? =
        feature.attributes.double(S57Attribute.VALSOU)
            ?: feature.attributes.double(S57Attribute.VALDCO)
            ?: feature.attributes.double(S57Attribute.DRVAL1)
            ?: feature.attributes.double(S57Attribute.DRVAL2)

    fun formatDepth(value: Double): String {
        val roundedTenth = (value * 10.0).roundToInt() / 10.0
        return if (abs(roundedTenth - roundedTenth.roundToInt()) < 0.0001) {
            roundedTenth.roundToInt().toString()
        } else {
            roundedTenth.toString()
        }
    }

    fun isNear(a: Double, b: Double, tolerance: Double = 0.05): Boolean = abs(a - b) <= tolerance

    fun isUnsafeDepth(depth: Double?, safetyDepthMeters: Double): Boolean =
        depth == null || depth <= safetyDepthMeters

    fun objectName(feature: EncFeature): String? =
        feature.attributes.text(S57Attribute.OBJNAM)?.takeIf { it.isNotBlank() }

    fun lightDescription(feature: EncFeature): String {
        val name = objectName(feature)
        val litchr = feature.attributes.int(S57Attribute.LITCHR)
        val siggrp = feature.attributes.text(S57Attribute.SIGGRP)?.takeIf { it.isNotBlank() }
        val sigper = feature.attributes.double(S57Attribute.SIGPER)
        val sectors = listOfNotNull(
            feature.attributes.double(S57Attribute.SECTR1),
            feature.attributes.double(S57Attribute.SECTR2)
        )
        return listOfNotNull(
            name,
            litchr?.let { "LITCHR=$it" },
            siggrp?.let { "SIGGRP=$it" },
            sigper?.let { "${formatDepth(it)}s" },
            sectors.takeIf { it.size == 2 }?.let { "${formatDepth(it[0])}-${formatDepth(it[1])}°" }
        ).joinToString(" ").ifBlank { "LIGHTS" }
    }
}
