package io.github.s52.core.model

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57AttributeKey
import io.github.s52.catalog.S57EnumeratedValue
import io.github.s52.catalog.toKey

class S57Attributes(
    private val values: Map<S57Attribute, S57Value> = emptyMap(),
    private val keyedValues: Map<S57AttributeKey, S57Value> = values.mapKeys { it.key.toKey() }
) {
    fun value(attribute: S57Attribute): S57Value? = values[attribute]

    fun value(attribute: S57AttributeKey): S57Value? = keyedValues[attribute] ?: attribute.standard?.let(::value)

    fun int(attribute: S57AttributeKey): Int? = value(attribute).asIntOrNull()

    fun int(attribute: S57Attribute): Int? = value(attribute).asIntOrNull()

    fun double(attribute: S57AttributeKey): Double? = value(attribute).asDoubleOrNull()

    fun double(attribute: S57Attribute): Double? = value(attribute).asDoubleOrNull()

    fun text(attribute: S57AttributeKey): String? = value(attribute).asTextOrNull()

    fun text(attribute: S57Attribute): String? = value(attribute).asTextOrNull()

    fun ints(attribute: S57AttributeKey): List<Int> = list(attribute).mapNotNull { it.asIntOrNull() }

    fun ints(attribute: S57Attribute): List<Int> = list(attribute).mapNotNull { it.asIntOrNull() }

    fun enum(attribute: S57Attribute): S57EnumeratedValue? =
        int(attribute)?.let { code -> S57EnumeratedValue.fromCode(attribute, code) }

    fun enumList(attribute: S57Attribute): List<S57EnumeratedValue> =
        ints(attribute).mapNotNull { code -> S57EnumeratedValue.fromCode(attribute, code) }

    fun list(attribute: S57AttributeKey): List<S57Value> = when (val value = value(attribute)) {
        is S57Value.ListValue -> value.values
        null -> emptyList()
        else -> listOf(value)
    }

    fun list(attribute: S57Attribute): List<S57Value> = when (val value = value(attribute)) {
        is S57Value.ListValue -> value.values
        null -> emptyList()
        else -> listOf(value)
    }

    operator fun contains(attribute: S57Attribute): Boolean = attribute in values

    operator fun contains(attribute: S57AttributeKey): Boolean = value(attribute) != null

    fun asMap(): Map<S57Attribute, S57Value> = values

    fun asKeyMap(): Map<S57AttributeKey, S57Value> = keyedValues

    companion object {
        val Empty = S57Attributes()

        fun of(vararg pairs: Pair<S57Attribute, S57Value>): S57Attributes =
            S57Attributes(mapOf(*pairs))

        fun ofKeys(vararg pairs: Pair<S57AttributeKey, S57Value>): S57Attributes =
            ofKeyMap(mapOf(*pairs))

        fun ofKeyMap(values: Map<S57AttributeKey, S57Value>): S57Attributes {
            val standard = values.mapNotNull { (key, value) -> key.standard?.let { it to value } }.toMap()
            return S57Attributes(standard, values)
        }
    }
}

private fun S57Value?.asIntOrNull(): Int? = when (this) {
    is S57Value.Integer -> value
    is S57Value.Decimal -> value.toInt()
    is S57Value.Text -> value.toIntOrNull()
    else -> null
}

private fun S57Value?.asDoubleOrNull(): Double? = when (this) {
    is S57Value.Decimal -> value
    is S57Value.Integer -> value.toDouble()
    is S57Value.Text -> value.toDoubleOrNull()
    else -> null
}

private fun S57Value?.asTextOrNull(): String? = when (this) {
    is S57Value.Text -> value
    is S57Value.Integer -> value.toString()
    is S57Value.Decimal -> value.toString()
    S57Value.Empty -> ""
    else -> null
}
