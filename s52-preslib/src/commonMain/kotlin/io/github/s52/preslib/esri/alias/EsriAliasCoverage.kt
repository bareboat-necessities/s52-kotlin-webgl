package io.github.s52.preslib.esri.alias

/** Summary counts copied into generated ESRI profile metadata. */
data class EsriAliasCoverage(
    val symbolAliasCount: Int = 0,
    val lineAliasCount: Int = 0,
    val patternAliasCount: Int = 0,
    val missingSymbolCount: Int = 0,
    val missingLineCount: Int = 0,
    val missingPatternCount: Int = 0
) {
    val isClosed: Boolean get() = missingSymbolCount == 0 && missingLineCount == 0 && missingPatternCount == 0
}
