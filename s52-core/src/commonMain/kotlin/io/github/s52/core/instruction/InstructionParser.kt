package io.github.s52.core.instruction

class InstructionParseException(message: String) : IllegalArgumentException(message)

object InstructionParser {
    fun parseSequence(source: String): List<S52Instruction> =
        splitTopLevel(source, ';')
            .filter { it.isNotBlank() }
            .map { parseOne(it.trim()) }

    fun parseOne(source: String): S52Instruction {
        val open = source.indexOf('(')
        val close = source.lastIndexOf(')')
        if (open <= 0 || close != source.length - 1) {
            throw InstructionParseException("Malformed instruction: '$source'")
        }

        val token = source.substring(0, open).trim().uppercase()
        val kind = InstructionKind.fromToken(token)
            ?: throw InstructionParseException("Unsupported instruction kind '$token' in '$source'")
        val args = splitTopLevel(source.substring(open + 1, close), ',').map { it.trim() }

        return when (kind) {
            InstructionKind.SY -> S52Instruction.Symbol(requiredArg(kind, args, 0))
            InstructionKind.AC -> S52Instruction.AreaColor(requiredArg(kind, args, 0))
            InstructionKind.AP -> S52Instruction.AreaPattern(requiredArg(kind, args, 0))
            InstructionKind.LC -> S52Instruction.ComplexLine(requiredArg(kind, args, 0))
            InstructionKind.CS -> S52Instruction.Conditional(requiredArg(kind, args, 0))
            InstructionKind.LS -> S52Instruction.SimpleLine(
                style = requiredArg(kind, args, 0),
                width = args.getOrNull(1)?.toDoubleOrNull() ?: 1.0,
                colorToken = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "CHBLK"
            )
            InstructionKind.TX,
            InstructionKind.TE -> S52Instruction.Text(
                textExpression = requiredArg(kind, args, 0),
                rawArgs = args,
                kind = kind
            )
        }
    }

    private fun requiredArg(kind: InstructionKind, args: List<String>, index: Int): String =
        args.getOrNull(index)?.takeIf { it.isNotBlank() }
            ?: throw InstructionParseException("${kind.token} requires argument ${index + 1}")

    private fun splitTopLevel(source: String, separator: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var depth = 0
        for (ch in source) {
            when (ch) {
                '(' -> {
                    depth += 1
                    sb.append(ch)
                }
                ')' -> {
                    depth -= 1
                    if (depth < 0) {
                        throw InstructionParseException("Unbalanced ')' in '$source'")
                    }
                    sb.append(ch)
                }
                separator -> {
                    if (depth == 0) {
                        out += sb.toString()
                        sb.clear()
                    } else {
                        sb.append(ch)
                    }
                }
                else -> sb.append(ch)
            }
        }
        if (depth != 0) {
            throw InstructionParseException("Unbalanced '(' in '$source'")
        }
        out += sb.toString()
        return out
    }
}
