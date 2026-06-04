package io.github.s52.tests.golden

/** Small, dependency-free transcript comparator used by JVM tests and tooling. */
data class GoldenTranscriptComparison(
    val expected: String,
    val actual: String
) {
    val matches: Boolean = normalize(expected) == normalize(actual)

    fun failureMessage(caseId: String): String {
        if (matches) return "Golden transcript matched for $caseId"
        val expectedLines = normalize(expected).lines()
        val actualLines = normalize(actual).lines()
        val max = maxOf(expectedLines.size, actualLines.size)
        val firstMismatch = (0 until max).firstOrNull { index ->
            expectedLines.getOrNull(index) != actualLines.getOrNull(index)
        } ?: 0
        return buildString {
            appendLine("Golden transcript mismatch for $caseId at line ${firstMismatch + 1}")
            appendLine("expected: ${expectedLines.getOrNull(firstMismatch) ?: "<missing>"}")
            appendLine("actual:   ${actualLines.getOrNull(firstMismatch) ?: "<missing>"}")
            appendLine()
            appendLine("--- expected ---")
            appendLine(normalize(expected))
            appendLine("--- actual ---")
            appendLine(normalize(actual))
        }
    }

    companion object {
        fun normalize(value: String): String =
            value.trim().lineSequence().map { it.trimEnd() }.joinToString("\n")
    }
}
