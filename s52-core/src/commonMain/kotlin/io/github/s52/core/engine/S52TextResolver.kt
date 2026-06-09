package io.github.s52.core.engine

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57AttributeKey
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Value
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Resolves S-52 text instructions into user-visible labels.
 *
 * Imported PLib rows often carry TX/TE expressions such as OBJNAM, NOBJNM,
 * VALSOU, or light attributes instead of the literal text that should be drawn.
 * This resolver intentionally supports the common expression subset first and
 * preserves the original expression as a fallback so portrayal remains stable
 * while the expression coverage grows.
 */
object S52TextResolver {
    fun resolveText(
        feature: EncFeature,
        textExpression: String,
        rawArgs: List<String> = emptyList()
    ): String {
        val expression = textExpression.trim()
        val direct = resolveExpression(feature, expression)
        if (!direct.isNullOrBlank()) return direct

        for (arg in rawArgs) {
            val resolved = resolveExpression(feature, arg.trim())
            if (!resolved.isNullOrBlank()) return resolved
        }

        if (feature.objectClass == S57ObjectClass.LIGHTS) {
            return lightDescription(feature).ifBlank { expression.ifBlank { feature.objectClass.acronym } }
        }

        return objectName(feature) ?: expression
    }

    fun resolveSoundingLabel(feature: EncFeature, fallbackExpression: String): String? {
        val depth = feature.attributes.double(S57Attribute.VALSOU)
            ?: feature.attributes.double(S57Attribute.VALDCO)
            ?: (feature.geometry as? EncGeometry.Point)?.coordinate?.z
            ?: fallbackExpression.trim().toDoubleOrNull()
        return depth?.let(::formatNumber) ?: resolveText(feature, fallbackExpression).takeIf { it.isNotBlank() }
    }

    private fun resolveExpression(feature: EncFeature, expression: String): String? {
        if (expression.isBlank()) return null

        val directAttribute = attributeValue(feature, expression)
        if (!directAttribute.isNullOrBlank()) return directAttribute

        if (looksLikeLiteralText(expression)) return expression

        var replaced = expression
        var changed = false
        for (token in attributeTokens(expression)) {
            val value = attributeValue(feature, token) ?: continue
            replaced = replaced
                .replace("{$token}", value)
                .replace("[$token]", value)
                .replace("$" + token, value)
            if (replaced == expression && expression == token) replaced = value
            changed = true
        }
        if (changed && replaced != expression) return replaced

        return when (expression.uppercase()) {
            "OBJNAM", "NOBJNM", "INFORM", "NINFOM", "TXTDSC" -> objectName(feature)
            "LIGHTS", "LITCHR", "SIGGRP", "SIGPER", "SECTR1", "SECTR2" -> lightDescription(feature)
            "VALSOU", "SOUNDG" -> resolveSoundingLabel(feature, expression)
            else -> null
        }
    }

    private fun objectName(feature: EncFeature): String? =
        feature.attributes.text(S57Attribute.OBJNAM)?.takeIf { it.isNotBlank() }
            ?: feature.attributes.text(S57Attribute.NOBJNM)?.takeIf { it.isNotBlank() }
            ?: feature.attributes.text(S57Attribute.INFORM)?.takeIf { it.isNotBlank() }
            ?: feature.attributes.text(S57Attribute.NINFOM)?.takeIf { it.isNotBlank() }
            ?: feature.attributes.text(S57Attribute.TXTDSC)?.takeIf { it.isNotBlank() }

    private fun lightDescription(feature: EncFeature): String {
        val pieces = mutableListOf<String>()
        objectName(feature)?.let { pieces += it }
        feature.attributes.int(S57Attribute.LITCHR)?.let { pieces += "LITCHR=$it" }
        feature.attributes.text(S57Attribute.SIGGRP)?.takeIf { it.isNotBlank() }?.let { pieces += "SIGGRP=$it" }
        feature.attributes.double(S57Attribute.SIGPER)?.let { pieces += "${formatNumber(it)}s" }
        val sector1 = feature.attributes.double(S57Attribute.SECTR1)
        val sector2 = feature.attributes.double(S57Attribute.SECTR2)
        if (sector1 != null && sector2 != null) pieces += "${formatNumber(sector1)}-${formatNumber(sector2)}°"
        return pieces.joinToString(" ")
    }

    private fun attributeValue(feature: EncFeature, acronymOrExpression: String): String? {
        val acronym = normalizedAttributeAcronym(acronymOrExpression) ?: return null
        val value = feature.attributes.value(S57AttributeKey.of(acronym)) ?: return null
        return value.formatForLabel().takeIf { it.isNotBlank() }
    }

    private fun normalizedAttributeAcronym(value: String): String? {
        val cleaned = value.trim().trim('"', '\'', '[', ']', '{', '}').uppercase()
        if (!cleaned.matches(Regex("[A-Z][A-Z0-9_]{2,9}"))) return null
        return cleaned
    }

    private fun attributeTokens(expression: String): List<String> =
        Regex("[A-Z][A-Z0-9_]{2,9}")
            .findAll(expression.uppercase())
            .map { it.value }
            .distinct()
            .toList()

    private fun looksLikeLiteralText(expression: String): Boolean =
        expression.any { it.isWhitespace() } ||
            expression.any { it.isLowerCase() } ||
            expression.any { it in ":,.;-/()" }

    private fun S57Value.formatForLabel(): String = when (this) {
        is S57Value.Integer -> value.toString()
        is S57Value.Decimal -> formatNumber(value)
        is S57Value.Text -> value
        is S57Value.ListValue -> values.joinToString(",") { it.formatForLabel() }
        S57Value.Empty -> ""
    }

    private fun formatNumber(value: Double): String {
        if (abs(value) < 0.0000000005) return "0"
        val roundedTenth = (value * 10.0).roundToInt() / 10.0
        val roundedInt = roundedTenth.roundToInt()
        return if (abs(roundedTenth - roundedInt.toDouble()) < 0.0001) {
            roundedInt.toString()
        } else {
            roundedTenth.toString().trimEnd('0').trimEnd('.')
        }
    }
}
