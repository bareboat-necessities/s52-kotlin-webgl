package io.github.s52.core.lookup

import io.github.s52.catalog.S57Attribute
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Value

sealed interface AttributeFilter {
    fun matches(feature: EncFeature): Boolean

    data object Any : AttributeFilter {
        override fun matches(feature: EncFeature): Boolean = true
    }

    data class Exists(val attribute: S57Attribute) : AttributeFilter {
        override fun matches(feature: EncFeature): Boolean = attribute in feature.attributes
    }

    data class EqualsInt(val attribute: S57Attribute, val expected: Int) : AttributeFilter {
        override fun matches(feature: EncFeature): Boolean = feature.attributes.int(attribute) == expected
    }

    data class Predicate(
        val description: String,
        val predicate: (EncFeature) -> Boolean
    ) : AttributeFilter {
        override fun matches(feature: EncFeature): Boolean = predicate(feature)
    }

    companion object {
        fun equals(attribute: S57Attribute, expected: Int): AttributeFilter = EqualsInt(attribute, expected)

        fun has(attribute: S57Attribute): AttributeFilter = Exists(attribute)

        fun textEquals(attribute: S57Attribute, expected: String): AttributeFilter = Predicate(
            description = "${attribute.acronym} == $expected"
        ) { feature ->
            when (val value = feature.attributes.value(attribute)) {
                is S57Value.Text -> value.value == expected
                else -> false
            }
        }
    }
}
