package io.github.s52.core.instruction

/** Inclusive/exclusive character range inside an original instruction string. */
data class InstructionSourceRange(
    val startInclusive: Int,
    val endExclusive: Int
) {
    init {
        require(startInclusive >= 0) { "startInclusive must be >= 0" }
        require(endExclusive >= startInclusive) { "endExclusive must be >= startInclusive" }
    }

    val length: Int get() = endExclusive - startInclusive

    fun slice(source: String): String = source.substring(startInclusive, endExclusive)
}
