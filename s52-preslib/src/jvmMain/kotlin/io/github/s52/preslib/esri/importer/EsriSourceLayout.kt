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

    fun isUsable(): Boolean = missingRequiredPaths().isEmpty()

    fun requireUsable() {
        if (!isUsable()) {
            tryAutoFetchForGitHubActions()
        }
        val missing = missingRequiredPaths()
        require(missing.isEmpty()) {
            "ESRI nautical-chart-symbols source is incomplete. Missing: ${missing.joinToString()}. " +
                "Pass -Pesri.sourceDir=/path/to/nautical-chart-symbols or set ESRI_NAUTICAL_CHART_SYMBOLS_DIR. " +
                "For GitHub Actions, run scripts/prepare-esri-source.sh before ESRI generation tasks."
        }
    }

    private fun tryAutoFetchForGitHubActions() {
        if (System.getenv("ESRI_DISABLE_AUTO_FETCH_SOURCE") == "true") return
        val autoFetchRequested = System.getenv("ESRI_AUTO_FETCH_SOURCE") == "true"
        val runningInGitHubActions = System.getenv("GITHUB_ACTIONS") == "true"
        if (!autoFetchRequested && !runningInGitHubActions) return

        val normalized = root.invariantSeparatorsPath.trimEnd('/')
        val looksLikeDefaultCheckout = normalized.endsWith("s52/esri/source") || normalized == "s52/esri/source"
        if (!looksLikeDefaultCheckout && !autoFetchRequested) return

        val existingEntries = root.listFiles().orEmpty().filterNot { it.name == ".git" }
        if (root.exists() && existingEntries.isNotEmpty()) {
            // Do not delete arbitrary user-provided partial data. The caller will get the normal error below.
            return
        }

        root.parentFile?.mkdirs()
        if (root.exists()) root.deleteRecursively()

        val command = listOf(
            "git",
            "clone",
            "--depth",
            "1",
            "https://github.com/Esri/nautical-chart-symbols.git",
            root.absolutePath
        )
        val result = runCatching {
            ProcessBuilder(command)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor()
        }.getOrElse { error ->
            System.err.println("Unable to auto-fetch ESRI nautical-chart-symbols source: ${error.message}")
            return
        }
        if (result != 0) {
            System.err.println("Unable to auto-fetch ESRI nautical-chart-symbols source; git clone exited with $result")
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
