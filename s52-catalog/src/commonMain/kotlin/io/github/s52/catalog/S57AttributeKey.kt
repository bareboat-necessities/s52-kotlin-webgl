package io.github.s52.catalog

/**
 * Stable S-57 attribute key used when an upstream source contains attribute
 * acronyms outside the curated [S57Attribute] enum.
 *
 * This keeps OpenCPN attrib-code filters and lowercase aliases representable
 * without losing rows during import. This is intentionally not a data class:
 * the constructor is private, and generated public copy methods on private
 * data-class constructors are a Kotlin 2.5 migration warning source.
 */
class S57AttributeKey private constructor(
    val acronym: String,
    val standard: S57Attribute?
) {
    init {
        require(acronym.isNotBlank()) { "Attribute acronym must not be blank" }
    }

    val isStandard: Boolean get() = standard != null

    override fun equals(other: Any?): Boolean =
        this === other || (other is S57AttributeKey && acronym == other.acronym && standard == other.standard)

    override fun hashCode(): Int = 31 * acronym.hashCode() + (standard?.hashCode() ?: 0)

    override fun toString(): String = acronym

    companion object {
        fun of(acronym: String): S57AttributeKey {
            val normalized = acronym.trim().uppercase()
            return S57AttributeKey(normalized, S57Attribute.fromAcronym(normalized))
        }

        fun of(attribute: S57Attribute): S57AttributeKey =
            S57AttributeKey(attribute.acronym, attribute)
    }
}

fun S57Attribute.toKey(): S57AttributeKey = S57AttributeKey.of(this)
