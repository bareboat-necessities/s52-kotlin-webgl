package io.github.s52.preslib.esri.importer

import java.io.File

internal data class EsriSourceLayout(val root: File) {
    val customPresentationLibrary: File = root.resolve("CustomPresentationLibrary")
    val customSymbolMap: File = customPresentationLibrary.resolve("CustomSymbolMap.xml")
    val symbolsDir: File = customPresentationLibrary.resolve("symbols")
    val pointSymbolsDir: File = symbolsDir.resolve("point")
    val lineSymbolsDir: File = symbolsDir.resolve("line")
    val patternSymbolsDir: File = symbolsDir.resolve("pattern")
    val luaDir: File = customPresentationLibrary.resolve("lua")

    fun missingRequiredPaths(): List<String> = buildList {
        if (!root.isDirectory) add(root.path)
        if (!customPresentationLibrary.isDirectory) add(customPresentationLibrary.path)
        if (!customSymbolMap.isFile) add(customSymbolMap.path)
        if (!symbolsDir.isDirectory) add(symbolsDir.path)
        if (!pointSymbolsDir.isDirectory) add(pointSymbolsDir.path)
        if (!lineSymbolsDir.isDirectory) add(lineSymbolsDir.path)
        if (!patternSymbolsDir.isDirectory) add(patternSymbolsDir.path)
        if (!luaDir.isDirectory) add(luaDir.path)
    }

    fun requireUsable() {
        val missing = missingRequiredPaths()
        require(missing.isEmpty()) {
            "ESRI nautical-chart-symbols source is incomplete. Missing: ${missing.joinToString()}. " +
                "Pass -Pesri.sourceDir=/path/to/nautical-chart-symbols or set ESRI_NAUTICAL_CHART_SYMBOLS_DIR."
        }
    }

    fun svgFiles(): List<File> = listOf(pointSymbolsDir, lineSymbolsDir, patternSymbolsDir)
        .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension.equals("svg", ignoreCase = true) }.toList() }
        .sortedBy { it.relativeTo(root).invariantSeparatorsPath }

    fun luaFiles(): List<File> = luaDir.walkTopDown()
        .filter { it.isFile && it.extension.equals("lua", ignoreCase = true) }
        .sortedBy { it.relativeTo(root).invariantSeparatorsPath }
        .toList()

    fun svgCategory(file: File): EsriSvgCategory = when {
        file.toPath().startsWith(pointSymbolsDir.toPath()) -> EsriSvgCategory.POINT
        file.toPath().startsWith(lineSymbolsDir.toPath()) -> EsriSvgCategory.LINE
        file.toPath().startsWith(patternSymbolsDir.toPath()) -> EsriSvgCategory.PATTERN
        else -> EsriSvgCategory.UNKNOWN
    }
}

internal enum class EsriSvgCategory { POINT, LINE, PATTERN, UNKNOWN }
