package io.github.s52.api

/**
 * Public semantic-version metadata for the portrayal library.
 *
 * The project is still pre-1.0 while the Presentation Library importer and
 * validation harness mature, but the Phase 12 public facade is intended to be
 * the stable integration surface for downstream Kotlin/JVM and Kotlin/JS apps.
 */
data class S52Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val qualifier: String? = null
) : Comparable<S52Version> {
    override fun compareTo(other: S52Version): Int = compareValuesBy(
        this,
        other,
        { it.major },
        { it.minor },
        { it.patch },
        { it.qualifier ?: "" }
    )

    override fun toString(): String = buildString {
        append(major).append('.').append(minor).append('.').append(patch)
        if (!qualifier.isNullOrBlank()) append('-').append(qualifier)
    }

    companion object {
        val Current: S52Version = S52Version(0, 12, 0, "SNAPSHOT")
    }
}
