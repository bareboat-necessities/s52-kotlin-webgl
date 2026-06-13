package io.github.s52.core.instruction

class InstructionParseException(
    message: String,
    val sourceRange: InstructionSourceRange? = null
) : IllegalArgumentException(message)

/**
 * Parser for S-52 Presentation Library instruction strings.
 *
 * The compatibility API returns AST nodes only. Also exposes detailed
 * parse results with source ranges, raw argument text, and canonical formatting.
 */
object InstructionParser {
    fun parseSequence(source: String): List<S52Instruction> =
        parseSequenceDetailed(source).ast()

    fun parseOne(source: String): S52Instruction =
        parseOneDetailed(source).instruction

    fun parseSequenceDetailed(source: String): InstructionSequence {
        val parsed = mutableListOf<ParsedInstruction>()
        splitTopLevel(source, ';', 0, source.length)
            .forEach { segment ->
                val trimmed = segment.trimmed(source)
                if (!trimmed.isEmpty) {
                    parsed += parseOneDetailed(source, trimmed.startInclusive, trimmed.endExclusive, parsed.size)
                }
            }
        return InstructionSequence(source = source, instructions = parsed)
    }

    fun parseOneDetailed(source: String): ParsedInstruction {
        val trimmed = InstructionSourceRange(0, source.length).trimmed(source)
        if (trimmed.isEmpty) throw error("Empty instruction", trimmed)
        return parseOneDetailed(source, trimmed.startInclusive, trimmed.endExclusive, 0)
    }

    private fun parseOneDetailed(
        source: String,
        start: Int,
        end: Int,
        index: Int
    ): ParsedInstruction {
        val open = findFirstInstructionOpenParen(source, start, end)
            ?: throw error("Malformed instruction: missing '('", InstructionSourceRange(start, end))
        val close = previousNonWhitespace(source, end - 1, start)
        if (close == null || source[close] != ')') {
            throw error("Malformed instruction: missing closing ')'", InstructionSourceRange(start, end))
        }
        ensureBalanced(source, open + 1, close)

        val tokenRange = InstructionSourceRange(start, open).trimmed(source)
        if (tokenRange.isEmpty) throw error("Instruction token is empty", InstructionSourceRange(start, open))
        val token = tokenRange.slice(source).uppercase()
        val kind = InstructionKind.fromToken(token)
            ?: throw error("Unsupported instruction kind '$token'", tokenRange)

        val argumentRange = InstructionSourceRange(open + 1, close)
        val arguments = parseArguments(source, argumentRange.startInclusive, argumentRange.endExclusive)
        val values = arguments.map { it.value }
        val instruction = toInstruction(kind, values, InstructionSourceRange(start, end))

        return ParsedInstruction(
            index = index,
            kind = kind,
            instruction = instruction,
            sourceRange = InstructionSourceRange(start, end),
            tokenRange = tokenRange,
            argumentRange = argumentRange,
            arguments = arguments,
            source = source
        )
    }

    private fun toInstruction(
        kind: InstructionKind,
        args: List<String>,
        sourceRange: InstructionSourceRange
    ): S52Instruction = when (kind) {
        InstructionKind.SY -> S52Instruction.Symbol(
            name = requiredArg(kind, args, 0, sourceRange),
            parameters = args.drop(1)
        )
        InstructionKind.AC -> S52Instruction.AreaColor(requiredArg(kind, args, 0, sourceRange))
        InstructionKind.AP -> S52Instruction.AreaPattern(
            name = requiredArg(kind, args, 0, sourceRange),
            parameters = args.drop(1)
        )
        InstructionKind.LC -> S52Instruction.ComplexLine(
            name = requiredArg(kind, args, 0, sourceRange),
            parameters = args.drop(1)
        )
        InstructionKind.CS -> {
            val raw = requiredArg(kind, args, 0, sourceRange)
            val split = raw.indexOf(';')
            val cspName = if (split >= 0) raw.substring(0, split).trim() else raw.trim()
            val extra = if (split >= 0) listOf(raw.substring(split + 1).trim()).filter { it.isNotEmpty() } else emptyList()
            S52Instruction.Conditional(
                cspName = cspName,
                parameters = extra + args.drop(1)
            )
        }
        InstructionKind.LS -> S52Instruction.SimpleLine(
            style = requiredArg(kind, args, 0, sourceRange),
            width = optionalPositiveDouble(args.getOrNull(1), default = 1.0, kind = kind, sourceRange = sourceRange),
            colorToken = args.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "CHBLK"
        )
        InstructionKind.TX,
        InstructionKind.TE -> {
            val expression = requiredArg(kind, args, 0, sourceRange)
            S52Instruction.Text(
                textExpression = expression,
                rawArgs = args,
                kind = kind,
                spec = TextSpec(expression, args, kind)
            )
        }
    }

    private fun requiredArg(
        kind: InstructionKind,
        args: List<String>,
        index: Int,
        sourceRange: InstructionSourceRange
    ): String = args.getOrNull(index)?.takeIf { it.isNotBlank() }
        ?: throw error("${kind.token} requires argument ${index + 1}", sourceRange)

    private fun optionalPositiveDouble(
        raw: String?,
        default: Double,
        kind: InstructionKind,
        sourceRange: InstructionSourceRange
    ): Double {
        if (raw.isNullOrBlank()) return default
        val value = raw.toDoubleOrNull()
            ?: throw error("${kind.token} numeric width is invalid: '$raw'", sourceRange)
        if (value <= 0.0 || !value.isFinite()) {
            throw error("${kind.token} numeric width must be positive: '$raw'", sourceRange)
        }
        return value
    }

    private fun parseArguments(source: String, start: Int, end: Int): List<InstructionArgument> {
        if (InstructionSourceRange(start, end).trimmed(source).isEmpty) return emptyList()
        return splitTopLevel(source, ',', start, end).map { rawRange ->
            val trimmed = rawRange.trimmed(source)
            val raw = rawRange.slice(source)
            if (trimmed.isEmpty) {
                InstructionArgument(value = "", raw = raw, sourceRange = rawRange, wasQuoted = false)
            } else {
                val trimmedRaw = trimmed.slice(source)
                val quote = source[trimmed.startInclusive]
                if ((quote == '"' || quote == '\'') && source[trimmed.endExclusive - 1] == quote) {
                    InstructionArgument(
                        value = unescapeQuoted(trimmedRaw.substring(1, trimmedRaw.length - 1)),
                        raw = raw,
                        sourceRange = rawRange,
                        wasQuoted = true
                    )
                } else {
                    InstructionArgument(
                        value = trimmedRaw,
                        raw = raw,
                        sourceRange = rawRange,
                        wasQuoted = false
                    )
                }
            }
        }
    }

    private fun unescapeQuoted(source: String): String = buildString {
        var escaping = false
        source.forEach { ch ->
            if (escaping) {
                append(
                    when (ch) {
                        'n' -> '\n'
                        'r' -> '\r'
                        't' -> '\t'
                        else -> ch
                    }
                )
                escaping = false
            } else if (ch == '\\') {
                escaping = true
            } else {
                append(ch)
            }
        }
        if (escaping) append('\\')
    }

    private fun splitTopLevel(source: String, separator: Char, start: Int, end: Int): List<InstructionSourceRange> {
        val out = mutableListOf<InstructionSourceRange>()
        var segmentStart = start
        var depth = 0
        var quote: Char? = null
        var escaping = false
        var i = start
        while (i < end) {
            val ch = source[i]
            if (escaping) {
                escaping = false
            } else if (quote != null) {
                when (ch) {
                    '\\' -> escaping = true
                    quote -> quote = null
                }
            } else {
                when (ch) {
                    '\'', '"' -> quote = ch
                    '(' -> depth += 1
                    ')' -> {
                        depth -= 1
                        if (depth < 0) throw error("Unbalanced ')'", InstructionSourceRange(i, i + 1))
                    }
                    separator -> {
                        if (depth == 0) {
                            out += InstructionSourceRange(segmentStart, i)
                            segmentStart = i + 1
                        }
                    }
                }
            }
            i += 1
        }
        if (quote != null) throw error("Unclosed quote", InstructionSourceRange(start, end))
        if (depth != 0) throw error("Unbalanced '('", InstructionSourceRange(start, end))
        out += InstructionSourceRange(segmentStart, end)
        return out
    }

    private fun findFirstInstructionOpenParen(source: String, start: Int, end: Int): Int? {
        var i = start
        while (i < end) {
            val ch = source[i]
            if (ch == '(') return i
            if (!ch.isLetterOrDigit() && ch != '_' && !ch.isWhitespace()) {
                throw error("Invalid character before instruction argument list: '$ch'", InstructionSourceRange(i, i + 1))
            }
            i += 1
        }
        return null
    }

    private fun ensureBalanced(source: String, start: Int, end: Int) {
        splitTopLevel(source, ',', start, end)
    }

    private fun previousNonWhitespace(source: String, start: Int, lowerBound: Int): Int? {
        var i = start
        while (i >= lowerBound) {
            if (!source[i].isWhitespace()) return i
            i -= 1
        }
        return null
    }

    private fun InstructionSourceRange.trimmed(source: String): InstructionSourceRange {
        var s = startInclusive
        var e = endExclusive
        while (s < e && source[s].isWhitespace()) s += 1
        while (e > s && source[e - 1].isWhitespace()) e -= 1
        return InstructionSourceRange(s, e)
    }

    private val InstructionSourceRange.isEmpty: Boolean get() = startInclusive == endExclusive

    private fun error(message: String, range: InstructionSourceRange): InstructionParseException =
        InstructionParseException("$message at ${range.startInclusive}..${range.endExclusive}", range)
}
