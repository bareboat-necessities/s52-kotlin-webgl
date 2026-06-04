package io.github.s52.core.model

import io.github.s52.catalog.S57Attribute

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
        else -> null
    }

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
