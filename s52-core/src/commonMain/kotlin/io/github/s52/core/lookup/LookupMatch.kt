package io.github.s52.core.lookup

/** A lookup record that matched a feature, plus ranking metadata used for diagnostics. */
data class LookupMatch(
    val record: LookupRecord,
    val recordIndex: Int,
    val attributeSpecificity: Int = record.attributeFilter.specificity
)
