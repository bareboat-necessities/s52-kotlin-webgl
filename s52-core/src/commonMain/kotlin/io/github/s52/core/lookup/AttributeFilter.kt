package io.github.s52.core.lookup

import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57AttributeKey
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Value
import kotlin.math.abs

/**
 * Typed lookup-table attribute predicate.
 *
 * The official Presentation Library lookup table contains attribute-condition
 * columns in addition to object class and primitive. Phase 4 keeps those
 * conditions as typed filter nodes so they can be indexed, scored, tested, and
 * generated. Use [Predicate] only for local experiments; generated lookup rows
 * should prefer the serializable structural filters below.
 */
sealed interface AttributeFilter {
    val description: String
    val specificity: Int

    fun matches(feature: EncFeature): Boolean

    data object Any : AttributeFilter {
        override val description: String = "*"
        override val specificity: Int = 0
        override fun matches(feature: EncFeature): Boolean = true
    }

    data class Exists(val attribute: S57Attribute) : AttributeFilter {
        override val description: String = "${attribute.acronym} exists"
        override val specificity: Int = 10
        override fun matches(feature: EncFeature): Boolean = attribute in feature.attributes
    }

    data class Missing(val attribute: S57Attribute) : AttributeFilter {
        override val description: String = "${attribute.acronym} missing"
        override val specificity: Int = 10
        override fun matches(feature: EncFeature): Boolean = attribute !in feature.attributes
    }

    data class EqualsInt(val attribute: S57Attribute, val expected: Int) : AttributeFilter {
        override val description: String = "${attribute.acronym} == $expected"
        override val specificity: Int = 20
        override fun matches(feature: EncFeature): Boolean = feature.attributes.ints(attribute).contains(expected)
    }

    data class IntIn(val attribute: S57Attribute, val expected: Set<Int>) : AttributeFilter {
        init { require(expected.isNotEmpty()) { "IntIn filter requires at least one expected value" } }
        override val description: String = "${attribute.acronym} in ${expected.sorted()}"
        override val specificity: Int = 20 + expected.size
        override fun matches(feature: EncFeature): Boolean = feature.attributes.ints(attribute).any { it in expected }
    }


    data class KeyExists(val attribute: S57AttributeKey) : AttributeFilter {
        override val description: String = "${attribute.acronym} exists"
        override val specificity: Int = 10
        override fun matches(feature: EncFeature): Boolean = attribute in feature.attributes
    }

    data class KeyMissing(val attribute: S57AttributeKey) : AttributeFilter {
        override val description: String = "${attribute.acronym} missing"
        override val specificity: Int = 10
        override fun matches(feature: EncFeature): Boolean = attribute !in feature.attributes
    }

    data class KeyEqualsInt(val attribute: S57AttributeKey, val expected: Int) : AttributeFilter {
        override val description: String = "${attribute.acronym} == $expected"
        override val specificity: Int = 20
        override fun matches(feature: EncFeature): Boolean = feature.attributes.ints(attribute).contains(expected)
    }

    data class KeyIntIn(val attribute: S57AttributeKey, val expected: Set<Int>) : AttributeFilter {
        init { require(expected.isNotEmpty()) { "KeyIntIn filter requires at least one expected value" } }
        override val description: String = "${attribute.acronym} in ${expected.sorted()}"
        override val specificity: Int = 20 + expected.size
        override fun matches(feature: EncFeature): Boolean = feature.attributes.ints(attribute).any { it in expected }
    }

    data class KeyEqualsDecimal(
        val attribute: S57AttributeKey,
        val expected: Double,
        val tolerance: Double = 1.0e-9
    ) : AttributeFilter {
        init { require(tolerance >= 0.0) { "Decimal comparison tolerance must be non-negative" } }
        override val description: String = "${attribute.acronym} == $expected"
        override val specificity: Int = 20
        override fun matches(feature: EncFeature): Boolean =
            feature.attributes.double(attribute)?.let { abs(it - expected) <= tolerance } == true
    }

    data class KeyDecimalRange(
        val attribute: S57AttributeKey,
        val minInclusive: Double? = null,
        val maxInclusive: Double? = null
    ) : AttributeFilter {
        init {
            require(minInclusive != null || maxInclusive != null) { "KeyDecimalRange requires at least one bound" }
            require(minInclusive == null || maxInclusive == null || minInclusive <= maxInclusive) {
                "KeyDecimalRange lower bound must be <= upper bound"
            }
        }
        override val description: String = buildString {
            append(attribute.acronym)
            append(" in ")
            append(minInclusive?.toString() ?: "-∞")
            append("..")
            append(maxInclusive?.toString() ?: "+∞")
        }
        override val specificity: Int = 20
        override fun matches(feature: EncFeature): Boolean {
            val value = feature.attributes.double(attribute) ?: return false
            if (minInclusive != null && value < minInclusive) return false
            if (maxInclusive != null && value > maxInclusive) return false
            return true
        }
    }

    data class EqualsDecimal(
        val attribute: S57Attribute,
        val expected: Double,
        val tolerance: Double = 1.0e-9
    ) : AttributeFilter {
        init { require(tolerance >= 0.0) { "Decimal comparison tolerance must be non-negative" } }
        override val description: String = "${attribute.acronym} == $expected"
        override val specificity: Int = 20
        override fun matches(feature: EncFeature): Boolean =
            feature.attributes.double(attribute)?.let { abs(it - expected) <= tolerance } == true
    }

    data class DecimalRange(
        val attribute: S57Attribute,
        val minInclusive: Double? = null,
        val maxInclusive: Double? = null
    ) : AttributeFilter {
        init {
            require(minInclusive != null || maxInclusive != null) { "DecimalRange requires at least one bound" }
            require(minInclusive == null || maxInclusive == null || minInclusive <= maxInclusive) {
                "DecimalRange lower bound must be <= upper bound"
            }
        }
        override val description: String = buildString {
            append(attribute.acronym)
            append(" in ")
            append(minInclusive?.toString() ?: "-∞")
            append("..")
            append(maxInclusive?.toString() ?: "+∞")
        }
        override val specificity: Int = 20
        override fun matches(feature: EncFeature): Boolean {
            val value = feature.attributes.double(attribute) ?: return false
            if (minInclusive != null && value < minInclusive) return false
            if (maxInclusive != null && value > maxInclusive) return false
            return true
        }
    }

    data class TextEquals(
        val attribute: S57Attribute,
        val expected: String,
        val ignoreCase: Boolean = false
    ) : AttributeFilter {
        override val description: String = "${attribute.acronym} == \"$expected\""
        override val specificity: Int = 20
        override fun matches(feature: EncFeature): Boolean =
            feature.attributes.list(attribute).any { value ->
                value.asText()?.equals(expected, ignoreCase = ignoreCase) == true
            }
    }

    data class TextIn(
        val attribute: S57Attribute,
        val expected: Set<String>,
        val ignoreCase: Boolean = false
    ) : AttributeFilter {
        init { require(expected.isNotEmpty()) { "TextIn filter requires at least one expected value" } }
        override val description: String = "${attribute.acronym} in $expected"
        override val specificity: Int = 20 + expected.size
        override fun matches(feature: EncFeature): Boolean =
            feature.attributes.list(attribute).any { value ->
                val text = value.asText() ?: return@any false
                expected.any { it.equals(text, ignoreCase = ignoreCase) }
            }
    }

    data class All(val filters: List<AttributeFilter>) : AttributeFilter {
        init { require(filters.isNotEmpty()) { "All filter requires at least one child filter" } }
        override val description: String = filters.joinToString(prefix = "(", postfix = ")", separator = " AND ") { it.description }
        override val specificity: Int = filters.sumOf { it.specificity } + 5
        override fun matches(feature: EncFeature): Boolean = filters.all { it.matches(feature) }
    }

    data class AnyOf(val filters: List<AttributeFilter>) : AttributeFilter {
        init { require(filters.isNotEmpty()) { "AnyOf filter requires at least one child filter" } }
        override val description: String = filters.joinToString(prefix = "(", postfix = ")", separator = " OR ") { it.description }
        override val specificity: Int = filters.maxOf { it.specificity } + 1
        override fun matches(feature: EncFeature): Boolean = filters.any { it.matches(feature) }
    }

    data class Not(val filter: AttributeFilter) : AttributeFilter {
        override val description: String = "NOT (${filter.description})"
        override val specificity: Int = filter.specificity + 1
        override fun matches(feature: EncFeature): Boolean = !filter.matches(feature)
    }

    data class Predicate(
        override val description: String,
        val predicate: (EncFeature) -> Boolean
    ) : AttributeFilter {
        override val specificity: Int = 1
        override fun matches(feature: EncFeature): Boolean = predicate(feature)
    }

    companion object {
        fun equals(attribute: S57Attribute, expected: Int): AttributeFilter = EqualsInt(attribute, expected)

        fun equals(attribute: S57AttributeKey, expected: Int): AttributeFilter = KeyEqualsInt(attribute, expected)

        fun has(attribute: S57Attribute): AttributeFilter = Exists(attribute)

        fun has(attribute: S57AttributeKey): AttributeFilter = KeyExists(attribute)

        fun missing(attribute: S57Attribute): AttributeFilter = Missing(attribute)

        fun missing(attribute: S57AttributeKey): AttributeFilter = KeyMissing(attribute)

        fun oneOf(attribute: S57Attribute, expected: Set<Int>): AttributeFilter = IntIn(attribute, expected)

        fun oneOf(attribute: S57AttributeKey, expected: Set<Int>): AttributeFilter = KeyIntIn(attribute, expected)

        fun range(attribute: S57Attribute, minInclusive: Double? = null, maxInclusive: Double? = null): AttributeFilter =
            DecimalRange(attribute, minInclusive, maxInclusive)

        fun textEquals(attribute: S57Attribute, expected: String, ignoreCase: Boolean = false): AttributeFilter =
            TextEquals(attribute, expected, ignoreCase)
    }
}

private fun S57Value.asText(): String? = when (this) {
    is S57Value.Text -> value
    is S57Value.Integer -> value.toString()
    is S57Value.Decimal -> value.toString()
    S57Value.Empty -> ""
    is S57Value.ListValue -> null
}
