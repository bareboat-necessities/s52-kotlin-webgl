package io.github.s52.core.lookup

import io.github.s52.catalog.S57AttributeKey

/**
 * CommonMain evaluator for OpenCPN `attrib-code` lookup filters.
 *
 * The generator keeps raw attrib-code strings in the OpenCPN source pack so the
 * browser/runtime can evaluate lookup rows without a JVM XML parser. This
 * parser intentionally supports the common structural forms used by
 * `chartsymbols.xml`; unrecognized forms are represented as [Unsupported] and
 * deliberately do not match features.
 */
object OpenCpnAttribCodeRuntimeParser {
    fun parseAll(codes: List<String>): AttributeFilter = when {
        codes.isEmpty() -> AttributeFilter.Any
        else -> codes.map(::parse).let { filters ->
            val meaningful = filters.filterNot { it === AttributeFilter.Any }
            when (meaningful.size) {
                0 -> AttributeFilter.Any
                1 -> meaningful.single()
                else -> AttributeFilter.All(meaningful)
            }
        }
    }

    fun parse(raw: String): AttributeFilter {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return AttributeFilter.Any

        optionalExists.matchEntire(trimmed)?.let { match ->
            return AttributeFilter.KeyExists(S57AttributeKey.of(normalizeAttributeName(match.groupValues[1])))
        }

        equalsOrList.matchEntire(trimmed)?.let { match ->
            val attribute = S57AttributeKey.of(normalizeAttributeName(match.groupValues[1]))
            val values = match.groupValues[2].split(',').mapNotNull { it.toIntOrNull() }.toSet()
            return when (values.size) {
                0 -> Unsupported(trimmed, "no integer values")
                1 -> AttributeFilter.KeyEqualsInt(attribute, values.single())
                else -> AttributeFilter.KeyIntIn(attribute, values)
            }
        }

        exists.matchEntire(trimmed)?.let {
            return AttributeFilter.KeyExists(S57AttributeKey.of(normalizeAttributeName(trimmed)))
        }

        return Unsupported(trimmed, "unrecognized attrib-code syntax")
    }

    private val optionalExists = Regex("^([A-Za-z0-9_\\$]+)\\?$")
    private val equalsOrList = Regex("^([A-Za-z_\\$][A-Za-z0-9_\\$]*)([0-9]+(?:,[0-9]+)*)$")
    private val exists = Regex("^[A-Za-z_\\$][A-Za-z0-9_\\$]*$")

    private fun normalizeAttributeName(raw: String): String {
        val upper = raw.trim().uppercase()
        return OpenCpnAttributeAliases[upper] ?: upper
    }

    private val OpenCpnAttributeAliases: Map<String, String> = mapOf(
        "FNCTNM" to "FUNCTN",
        "FUNCTNM" to "FUNCTN",
        "CATTML" to "CATTRK",
        "SCODE" to "SCODE"
    )

    /** Unsupported attrib-code node. It is high-specificity and never matches. */
    data class Unsupported(val raw: String, val reason: String) : AttributeFilter {
        override val description: String = "unsupported OpenCPN attrib-code '$raw': $reason"
        override val specificity: Int = 100
        override fun matches(feature: io.github.s52.core.model.EncFeature): Boolean = false
    }
}
