package io.github.s52.core.instruction

/** Canonical formatter for parsed S-52 instruction ASTs. */
object InstructionFormatter {
    fun formatSequence(instructions: List<S52Instruction>): String =
        instructions.joinToString(";") { format(it) }

    fun format(instruction: S52Instruction): String = when (instruction) {
        is S52Instruction.Symbol -> "SY(${formatArgs(listOf(instruction.name) + instruction.parameters)})"
        is S52Instruction.SimpleLine -> "LS(${instruction.style},${formatNumber(instruction.width)},${instruction.colorToken})"
        is S52Instruction.ComplexLine -> "LC(${formatArgs(listOf(instruction.name) + instruction.parameters)})"
        is S52Instruction.AreaColor -> "AC(${instruction.colorToken})"
        is S52Instruction.AreaPattern -> "AP(${formatArgs(listOf(instruction.name) + instruction.parameters)})"
        is S52Instruction.Text -> "${instruction.kind.token}(${formatArgs(instruction.rawArgs)})"
        is S52Instruction.Conditional -> "CS(${formatArgs(listOf(instruction.cspName) + instruction.parameters)})"
    }

    private fun formatArgs(args: List<String>): String = args.joinToString(",") { formatArgumentIfNeeded(it) }

    private fun formatNumber(value: Double): String =
        if (value.isFinite() && value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    private fun formatArgumentIfNeeded(value: String): String {
        if (value.isEmpty()) return "\"\""
        val needsQuotes = value.any { it.isWhitespace() || it == ',' || it == '(' || it == ')' || it == ';' || it == '\'' || it == '"' }
        if (!needsQuotes) return value
        return buildString {
            append('"')
            value.forEach { ch ->
                when (ch) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(ch)
                }
            }
            append('"')
        }
    }
}
