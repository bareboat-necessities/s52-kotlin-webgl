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
        val resolvedDirectory = resolvePayloadDirectory(directory)

        val files = RequiredFiles.map { fileName ->
            val file = resolvedDirectory.resolve(fileName)
            OpenCpnPayloadFile(fileName, if (file.isFile) file.length() else 0L, file.isFile)
        }
        val warnings = mutableListOf<String>()
        val missing = files.filterNot { it.exists }.map { it.fileName }

        val chartSymbols = resolvedDirectory.resolve("chartsymbols.xml").takeIf { it.isFile }?.let { file ->
            runCatching { OpenCpnChartSymbolsRawParser.parseFile(file) }
                .onFailure { warnings += "chartsymbols.xml parse failed: ${it.message}" }
                .getOrNull()
        }

        val csvCatalog = runCatching { OpenCpnCsvCatalogParser.parseDirectory(resolvedDirectory) }
            .onFailure { warnings += "CSV catalog parse failed: ${it.message}" }
            .getOrElse { OpenCpnCsvCatalogSummary() }

        val atlases = listOf("rastersymbols-day.png", "rastersymbols-dusk.png", "rastersymbols-dark.png")
            .mapNotNull { fileName -> readRasterAtlas(resolvedDirectory.resolve(fileName), warnings) }

        val lookupDiagnostics = chartSymbols?.let { OpenCpnLookupRawParser.diagnostics(it, csvCatalog) }

        return OpenCpnPayloadInventory(
            directory = resolvedDirectory,
            files = files,
            chartSymbols = chartSymbols,
            csvCatalog = csvCatalog,
            rasterAtlases = atlases,
            diagnostics = OpenCpnInventoryDiagnostics(missingRequiredFiles = missing, parseWarnings = warnings),
            lookupDiagnostics = lookupDiagnostics
        )
    }

    private fun resolvePayloadDirectory(requested: File): File {
        val attempts = linkedSetOf<File>()
        fun add(file: File) {
            attempts += file.absoluteFile.toPath().normalize().toFile()
        }

        add(requested)
        add(File("s52/opencpn"))
        add(File("../s52/opencpn"))
        add(File("../../s52/opencpn"))

        var cursor: File? = File(System.getProperty("user.dir")).absoluteFile
        while (cursor != null) {
            add(File(cursor, "s52/opencpn"))
            cursor = cursor.parentFile
        }

        return attempts.firstOrNull { it.isDirectory && it.resolve("chartsymbols.xml").isFile }
            ?: throw IllegalArgumentException(
                "OpenCPN payload directory does not exist or is missing chartsymbols.xml. Tried: " +
                    attempts.joinToString { it.absolutePath }
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
