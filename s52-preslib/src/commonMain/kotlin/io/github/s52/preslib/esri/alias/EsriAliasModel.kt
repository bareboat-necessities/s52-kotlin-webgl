package io.github.s52.preslib.esri.alias

data class EsriAlias(
    val sourceName: String,
    val targetName: String,
    val confidence: String,
    val reason: String
)

class EsriAliasRegistry(
    val symbolAliases: Map<String, EsriAlias> = emptyMap(),
    val lineAliases: Map<String, EsriAlias> = emptyMap(),
    val patternAliases: Map<String, EsriAlias> = emptyMap()
) {
    fun symbol(name: String): String? = symbolAliases[name]?.targetName
    fun line(name: String): String? = lineAliases[name]?.targetName
    fun pattern(name: String): String? = patternAliases[name]?.targetName

    companion object {
        val Empty = EsriAliasRegistry()
    }
}
