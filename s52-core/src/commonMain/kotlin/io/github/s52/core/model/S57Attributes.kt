package io.github.s52.core.model

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57EnumeratedValue

class S57Attributes(
    private val values: Map<S57Attribute, S57Value> = emptyMap()
) {
    fun value(attribute: S57Attribute): S57Value? = values[attribute]

    fun int(attribute: S57Attribute): Int? = when (val value = values[attribute]) {
        is S57Value.Integer -> value.value
        is S57Value.Decimal -> value.value.toInt()
        is S57Value.Text -> value.value.toIntOrNull()
        else -> null
    }

    fun double(attribute: S57Attribute): Double? = when (val value = values[attribute]) {
        is S57Value.Decimal -> value.value
        is S57Value.Integer -> value.value.toDouble()
        is S57Value.Text -> value.value.toDoubleOrNull()
        else -> null
    }

    fun text(attribute: S57Attribute): String? = when (val value = values[attribute]) {
        is S57Value.Text -> value.value
        is S57Value.Integer -> value.value.toString()
        is S57Value.Decimal -> value.value.toString()
        S57Value.Empty -> ""
        else -> null
    }

    fun ints(attribute: S57Attribute): List<Int> = when (val value = values[attribute]) {
        is S57Value.ListValue -> value.values.mapNotNull { item ->
            when (item) {
                is S57Value.Integer -> item.value
                is S57Value.Decimal -> item.value.toInt()
                is S57Value.Text -> item.value.toIntOrNull()
                else -> null
            }
        }
        null -> emptyList()
        else -> int(attribute)?.let(::listOf).orEmpty()
    }

    fun enum(attribute: S57Attribute): S57EnumeratedValue? =
        int(attribute)?.let { code -> S57EnumeratedValue.fromCode(attribute, code) }

    fun enumList(attribute: S57Attribute): List<S57EnumeratedValue> =
        ints(attribute).mapNotNull { code -> S57EnumeratedValue.fromCode(attribute, code) }

    fun list(attribute: S57Attribute): List<S57Value> = when (val value = values[attribute]) {
        is S57Value.ListValue -> value.values
        null -> emptyList()
        else -> listOf(value)
    }

    operator fun contains(attribute: S57Attribute): Boolean = attribute in values

    fun asMap(): Map<S57Attribute, S57Value> = values

    companion object {
        val Empty = S57Attributes()

        fun of(vararg pairs: Pair<S57Attribute, S57Value>): S57Attributes =
            S57Attributes(mapOf(*pairs))
    }
}
