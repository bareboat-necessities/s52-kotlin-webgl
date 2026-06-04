package io.github.s52.core.model

/** Typed value container for already-normalized S-57 attributes. */
sealed interface S57Value {
    data class Integer(val value: Int) : S57Value
    data class Decimal(val value: Double) : S57Value
    data class Text(val value: String) : S57Value
    data class ListValue(val values: List<S57Value>) : S57Value
    data object Empty : S57Value
}
