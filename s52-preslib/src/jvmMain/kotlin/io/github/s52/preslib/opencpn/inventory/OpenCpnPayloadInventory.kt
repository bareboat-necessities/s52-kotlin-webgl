package io.github.s52.preslib.opencpn.inventory

import java.io.File
import javax.imageio.ImageIO

/** Discovers and summarizes a complete OpenCPN portrayal payload directory. */
object OpenCpnPayloadInventoryReader {
    val RequiredFiles: List<String> = listOf(
        "chartsymbols.xml",
        "rastersymbols-day.png",
        "rastersymbols-dusk.png",
        "rastersymbols-dark.png",
        "s57objectclasses.csv",
        "s57attributes.csv",
        "s57expectedinput.csv",
        "attdecode.csv",
        "Helvetica.txf",
        "S52RAZDS.RLE"
    )

    fun read(directory: File): OpenCpnPayloadInventory {
        require(directory.isDirectory) { "OpenCPN payload directory does not exist: ${directory.absolutePath}" }

        val files = RequiredFiles.map { fileName ->
            val file = directory.resolve(fileName)
            OpenCpnPayloadFile(fileName, if (file.isFile) file.length() else 0L, file.isFile)
        }
        val warnings = mutableListOf<String>()
        val missing = files.filterNot { it.exists }.map { it.fileName }

        val chartSymbols = directory.resolve("chartsymbols.xml").takeIf { it.isFile }?.let { file ->
            runCatching { OpenCpnChartSymbolsRawParser.parseFile(file) }
                .onFailure { warnings += "chartsymbols.xml parse failed: ${it.message}" }
                .getOrNull()
        }

        val csvCatalog = runCatching { OpenCpnCsvCatalogParser.parseDirectory(directory) }
            .onFailure { warnings += "CSV catalog parse failed: ${it.message}" }
            .getOrElse { OpenCpnCsvCatalogSummary() }

        val atlases = listOf("rastersymbols-day.png", "rastersymbols-dusk.png", "rastersymbols-dark.png")
            .mapNotNull { fileName -> readRasterAtlas(directory.resolve(fileName), warnings) }

        return OpenCpnPayloadInventory(
            directory = directory,
            files = files,
            chartSymbols = chartSymbols,
            csvCatalog = csvCatalog,
            rasterAtlases = atlases,
            diagnostics = OpenCpnInventoryDiagnostics(missingRequiredFiles = missing, parseWarnings = warnings)
        )
    }

    private fun readRasterAtlas(file: File, warnings: MutableList<String>): OpenCpnRasterAtlas? {
        if (!file.isFile) return null
        val image = runCatching { ImageIO.read(file) }
            .onFailure { warnings += "${file.name} image parse failed: ${it.message}" }
            .getOrNull()
            ?: return null
        return OpenCpnRasterAtlas(
            fileName = file.name,
            width = image.width,
            height = image.height,
            paletteHint = paletteHint(file.name),
            sizeBytes = file.length()
        )
    }

    private fun paletteHint(fileName: String): OpenCpnRasterPaletteHint = when {
        fileName.contains("day", ignoreCase = true) -> OpenCpnRasterPaletteHint.Day
        fileName.contains("dusk", ignoreCase = true) -> OpenCpnRasterPaletteHint.Dusk
        fileName.contains("dark", ignoreCase = true) || fileName.contains("night", ignoreCase = true) -> OpenCpnRasterPaletteHint.Dark
        else -> OpenCpnRasterPaletteHint.Unknown
    }
}
