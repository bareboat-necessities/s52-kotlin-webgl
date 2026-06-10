package io.github.s52.preslib.esri.importer

internal object EsriJson {
    fun quote(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> {
                    if (ch.code < 0x20) {
                        append("\\u")
                        append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        append(ch)
                    }
                }
            }
        }
        append('"')
    }

    fun stringArray(values: Iterable<String>): String = values.joinToString(prefix = "[", postfix = "]") { quote(it) }

    fun objectBody(vararg pairs: Pair<String, String>): String = pairs.joinToString(",\n") { (key, value) ->
        "  ${quote(key)}: $value"
    }.prependIndent("  ").trimStart()
}
