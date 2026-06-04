package io.github.s52.catalog

/** Coarse S-57 attribute type metadata used for import validation and diagnostics. */
enum class S57AttributeValueKind {
    Integer,
    Decimal,
    Text,
    Enumeration,
    EnumerationList,
    Unknown
}
