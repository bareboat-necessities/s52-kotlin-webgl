package io.github.s52.preslib.esri.svg

/**
 * Lightweight SVG path tokenizer for Phase ESRI-2 validation.
 *
 * It records path commands and numeric payloads without doing geometric normalization yet.
 * Mesh generation in later phases can build on this parser or replace it with a fuller implementation.
 */
object EsriSvgPathParser {
    private val supportedCommands = setOf('M', 'L', 'H', 'V', 'C', 'S', 'Q', 'T', 'Z')

    fun parse(d: String): EsriSvgPathData {
        val tokens = tokenize(d)
        val commands = mutableListOf<EsriSvgPathCommand>()
        val unsupported = linkedSetOf<Char>()
        var currentCommand: Char? = null
        val values = mutableListOf<Double>()

        fun flush() {
            val cmd = currentCommand ?: return
            commands += EsriSvgPathCommand(
                command = cmd.uppercaseChar(),
                relative = cmd.isLowerCase(),
                values = values.toList()
            )
            if (cmd.uppercaseChar() !in supportedCommands) unsupported += cmd.uppercaseChar()
            values.clear()
        }

        for (token in tokens) {
            if (token.length == 1 && token[0].isLetter()) {
                flush()
                currentCommand = token[0]
            } else {
                val number = token.toDoubleOrNull()
                if (number != null) values += number
            }
        }
        flush()
        return EsriSvgPathData(commands = commands, unsupportedCommands = unsupported.toList())
    }

    private fun tokenize(d: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < d.length) {
            val ch = d[i]
            when {
                ch.isWhitespace() || ch == ',' -> i++
                ch.isLetter() -> {
                    out += ch.toString()
                    i++
                }
                ch == '+' || ch == '-' || ch == '.' || ch.isDigit() -> {
                    val start = i
                    i++
                    while (i < d.length) {
                        val c = d[i]
                        val previous = d[i - 1]
                        val exponentSign = (c == '+' || c == '-') && (previous == 'e' || previous == 'E')
                        if (c.isDigit() || c == '.' || c == 'e' || c == 'E' || exponentSign) {
                            i++
                        } else {
                            break
                        }
                    }
                    out += d.substring(start, i)
                }
                else -> i++
            }
        }
        return out
    }
}
