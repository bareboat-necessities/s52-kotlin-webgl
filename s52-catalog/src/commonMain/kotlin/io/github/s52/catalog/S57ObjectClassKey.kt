package io.github.s52.catalog

/**
 * Stable S-57 object-class key used when an upstream catalogue contains names
 * outside the curated [S57ObjectClass] enum.
 *
 * OpenCPN's portrayal XML includes pseudo/mariner object names such as
 * `$AREAS`, `$LINES`, `OWNSHP`, `VESSEL`, and `NOTMRK`. Keeping the raw
 * acronym lets those lookup rows survive import before the runtime catalogue is
 * fully widened. This is intentionally not a data class: the constructor is
 * private, and generated public copy methods on private data-class constructors
 * are a Kotlin 2.5 migration warning source.
 */
class S57ObjectClassKey private constructor(
    val acronym: String,
    val standard: S57ObjectClass?
) {
    init {
        require(acronym.isNotBlank()) { "Object class acronym must not be blank" }
    }

    val isStandard: Boolean get() = standard != null

    override fun equals(other: Any?): Boolean =
        this === other || (other is S57ObjectClassKey && acronym == other.acronym && standard == other.standard)

    override fun hashCode(): Int = 31 * acronym.hashCode() + (standard?.hashCode() ?: 0)

    override fun toString(): String = acronym

    companion object {
        fun of(acronym: String): S57ObjectClassKey {
            val normalized = acronym.trim().uppercase()
            return S57ObjectClassKey(normalized, S57ObjectClass.fromAcronym(normalized))
        }

        fun of(objectClass: S57ObjectClass): S57ObjectClassKey =
            S57ObjectClassKey(objectClass.acronym, objectClass)
    }
}

fun S57ObjectClass.toKey(): S57ObjectClassKey = S57ObjectClassKey.of(this)
