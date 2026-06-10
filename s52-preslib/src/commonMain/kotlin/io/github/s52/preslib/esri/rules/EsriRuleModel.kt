package io.github.s52.preslib.esri.rules

/**
 * Runtime-neutral ESRI rule model generated from CustomSymbolMap.xml.
 *
 * The model intentionally preserves ESRI object acronyms and primitive numbers so
 * it can be used before the final S-57 catalogue adapter is wired in.
 */
data class EsriPortrayalRule(
    val objects: List<String>,
    /** ESRI/S-57 primitive number: 1 point, 2 line, 3 area. Null means any. */
    val primitive: Int?,
    val action: EsriRuleAction,
    val filters: List<EsriRuleFilter> = emptyList(),
    val sourceOrder: Int = 0
) {
    fun matches(feature: EsriRuleFeature): Boolean {
        if (objects.isNotEmpty() && feature.objectAcronym !in objects) return false
        if (primitive != null && feature.primitive != primitive) return false
        return filters.all { it.matches(feature) }
    }
}

data class EsriRuleFeature(
    val objectAcronym: String,
    val primitive: Int?,
    val attributes: Map<String, List<String>> = emptyMap(),
    val coincidentObjects: Set<String> = emptySet()
) {
    fun first(field: String): String? = attributes[field]?.firstOrNull()
    fun values(field: String): List<String> = attributes[field].orEmpty()
}

sealed interface EsriRuleAction {
    data class Symbol(val name: String) : EsriRuleAction
    data class Function(val names: List<String>) : EsriRuleAction
}

sealed interface EsriRuleFilter {
    fun matches(feature: EsriRuleFeature): Boolean

    data class Attribute(
        val field: String,
        val operator: EsriRuleOperator,
        val values: List<String>
    ) : EsriRuleFilter {
        override fun matches(feature: EsriRuleFeature): Boolean = operator.matches(feature.values(field), values)
    }

    data class ListAttribute(
        val field: String,
        val operator: EsriRuleOperator,
        val values: List<String>
    ) : EsriRuleFilter {
        override fun matches(feature: EsriRuleFeature): Boolean = operator.matches(feature.values(field), values)
    }

    data class CoincidentFeature(
        val objectName: String,
        val operator: EsriRuleOperator,
        val display: Boolean
    ) : EsriRuleFilter {
        override fun matches(feature: EsriRuleFeature): Boolean = when (operator) {
            EsriRuleOperator.EXISTS,
            EsriRuleOperator.FETCH -> objectName in feature.coincidentObjects
            EsriRuleOperator.NOT_EQUAL,
            EsriRuleOperator.CONTAINS_NONE -> objectName !in feature.coincidentObjects
            else -> objectName in feature.coincidentObjects
        }
    }
}

enum class EsriRuleOperator {
    EQUAL,
    NOT_EQUAL,
    LT,
    LTE,
    GT,
    GTE,
    BETWEEN,
    LIKE,
    NOT_LIKE,
    CONTAINS_ANY,
    CONTAINS_NONE,
    CONTAINS_ALL,
    HAS_MULTIPLE,
    EXISTS,
    FETCH,
    UNKNOWN;

    fun matches(actual: List<String>, expected: List<String>): Boolean = when (this) {
        EQUAL -> expected.any { wanted -> actual.any { it.eqEsri(wanted) } }
        NOT_EQUAL -> expected.none { wanted -> actual.any { it.eqEsri(wanted) } }
        LT -> compareFirst(actual, expected) { a, b -> a < b }
        LTE -> compareFirst(actual, expected) { a, b -> a <= b }
        GT -> compareFirst(actual, expected) { a, b -> a > b }
        GTE -> compareFirst(actual, expected) { a, b -> a >= b }
        BETWEEN -> {
            val a = actual.firstOrNull()?.toDoubleOrNull()
            val lo = expected.getOrNull(0)?.toDoubleOrNull()
            val hi = expected.getOrNull(1)?.toDoubleOrNull()
            a != null && lo != null && hi != null && a >= lo && a <= hi
        }
        LIKE -> expected.any { pattern -> actual.any { it.matchesWildcard(pattern) } }
        NOT_LIKE -> expected.none { pattern -> actual.any { it.matchesWildcard(pattern) } }
        CONTAINS_ANY -> expected.any { wanted -> actual.any { it.eqEsri(wanted) } }
        CONTAINS_NONE -> expected.none { wanted -> actual.any { it.eqEsri(wanted) } }
        CONTAINS_ALL -> expected.all { wanted -> actual.any { it.eqEsri(wanted) } }
        HAS_MULTIPLE -> actual.size > 1
        EXISTS -> actual.isNotEmpty()
        FETCH -> actual.isNotEmpty()
        UNKNOWN -> false
    }

    companion object {
        fun fromXml(value: String): EsriRuleOperator = when (value.trim().lowercase()) {
            "equal" -> EQUAL
            "notequal" -> NOT_EQUAL
            "lt" -> LT
            "lte" -> LTE
            "gt" -> GT
            "gte" -> GTE
            "between" -> BETWEEN
            "like" -> LIKE
            "notlike" -> NOT_LIKE
            "containsany" -> CONTAINS_ANY
            "containsnone" -> CONTAINS_NONE
            "containsall" -> CONTAINS_ALL
            "hasmultiple" -> HAS_MULTIPLE
            "exists" -> EXISTS
            "fetch" -> FETCH
            else -> UNKNOWN
        }
    }
}

private fun compareFirst(actual: List<String>, expected: List<String>, predicate: (Double, Double) -> Boolean): Boolean {
    val a = actual.firstOrNull()?.toDoubleOrNull() ?: return false
    val b = expected.firstOrNull()?.toDoubleOrNull() ?: return false
    return predicate(a, b)
}

private fun String.eqEsri(other: String): Boolean = trim().equals(other.trim(), ignoreCase = true)

private fun String.matchesWildcard(pattern: String): Boolean {
    val regex = pattern
        .trim()
        .split('*')
        .joinToString(".*") { Regex.escape(it) }
        .let { "^$it$" }
        .toRegex(RegexOption.IGNORE_CASE)
    return regex.matches(trim())
}
