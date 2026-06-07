package io.github.s52.preslib.generator

import io.github.s52.preslib.LineStyleDefinition
import io.github.s52.preslib.PatternDefinition
import io.github.s52.preslib.PresLibPack
import io.github.s52.preslib.RasterBitmapDefinition
import io.github.s52.preslib.SymbolDefinition
import io.github.s52.preslib.VectorCommand
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Generates browsable SVG image artifacts for symbols, line styles, and patterns.
 *
 * The original Phase 2 generator was vector-only and always drew a red pivot dot.
 * That is fine for tiny synthetic line art, but it is wrong for OpenCPN where most
 * symbols are bitmap cells in rastersymbols-*.png and have no vector commands.
 * When those bitmap symbols were rendered by this generator they appeared as
 * nothing but the red pivot dot.  The generator now embeds cropped raster cells
 * when [RasterBitmapDefinition] metadata is available and never uses the pivot
 * marker as a bitmap fallback.
 */
object SymbologyImageGenerator {
    private const val CARD_WIDTH = 176
    private const val CARD_HEIGHT = 128
    private const val SYMBOL_VIEWBOX_PADDING = 8.0

    @JvmStatic
    fun main(args: Array<String>) {
        val outputDir = args.firstOrNull()?.let(Path::of)
            ?: Path.of("build", "symbology-images")
        generate(outputDir)
    }

    fun generate(outputDir: Path, pack: PresLibPack = PresLibPack.phase2Synthetic()) {
        val rasterAtlases = RasterAtlasStore.locate()
        val symbolsDir = outputDir.resolve("symbols")
        val lineStylesDir = outputDir.resolve("line-styles")
        val patternsDir = outputDir.resolve("patterns")
        listOf(outputDir, symbolsDir, lineStylesDir, patternsDir).forEach(Files::createDirectories)

        val symbolEntries = uniqueSymbolEntries(pack.symbols.all())
        val lineStyleNames = pack.lineStyles.names().sorted()
        val patternNames = pack.patterns.names().sorted()

        symbolEntries.forEach { entry ->
            symbolsDir.resolve("${entry.fileStem}.svg").writeText(symbolSvg(entry.symbol, title = entry.title, rasterAtlases = rasterAtlases))
        }
        lineStyleNames.forEach { name ->
            val lineStyle = pack.lineStyles.find(name) ?: LineStyleDefinition(name)
            lineStylesDir.resolve("${name.s52FileName()}.svg").writeText(lineStyleSvg(lineStyle))
        }
        patternNames.forEach { name ->
            val pattern = pack.patterns.find(name) ?: PatternDefinition(name)
            patternsDir.resolve("${name.s52FileName()}.svg").writeText(patternSvg(pattern))
        }

        outputDir.resolve("all-symbology.svg").writeText(
            contactSheetSvg(
                symbols = symbolEntries,
                lineStyles = lineStyleNames.map { pack.lineStyles.find(it) ?: LineStyleDefinition(it) },
                patterns = patternNames.map { pack.patterns.find(it) ?: PatternDefinition(it) },
                rasterAtlases = rasterAtlases
            )
        )
        outputDir.resolve("manifest.txt").writeText(
            buildString {
                appendLine("S-52 symbology image artifact")
                appendLine("Symbols: ${symbolEntries.size}")
                symbolEntries.forEach { appendLine("  symbols/${it.fileStem}.svg") }
                appendLine("Line styles: ${lineStyleNames.size}")
                lineStyleNames.forEach { appendLine("  line-styles/${it.s52FileName()}.svg") }
                appendLine("Patterns: ${patternNames.size}")
                patternNames.forEach { appendLine("  patterns/${it.s52FileName()}.svg") }
                appendLine("Contact sheet: all-symbology.svg")
                appendLine("Bitmap atlases embedded: ${rasterAtlases.names().sorted().joinToString(",")}")
            }
        )
    }

    private fun contactSheetSvg(
        symbols: List<SymbolEntry>,
        lineStyles: List<LineStyleDefinition>,
        patterns: List<PatternDefinition>,
        rasterAtlases: RasterAtlasStore
    ): String {
        val cards = buildList {
            addAll(symbols.map { cardSvg("Symbol", it.title, symbolPreview(it.symbol, x = 28.0, y = 30.0, scale = 4.0, rasterAtlases = rasterAtlases)) })
            addAll(lineStyles.map { cardSvg("Line style", it.name, lineStylePreview(it, x = 20, y = 58, width = 136)) })
            addAll(patterns.map { cardSvg("Pattern", it.name, patternPreview(it, x = 44, y = 28, size = 88)) })
        }
        val columns = 4
        val rows = max(1, ceil(cards.size / columns.toDouble()).toInt())
        val width = columns * CARD_WIDTH
        val height = rows * CARD_HEIGHT
        val body = cards.mapIndexed { index, card ->
            val col = index % columns
            val row = index / columns
            "<g transform=\"translate(${col * CARD_WIDTH}, ${row * CARD_HEIGHT})\">$card</g>"
        }.joinToString("\n")
        return svgDocument(width, height, body)
    }

    private fun cardSvg(type: String, name: String, preview: String): String = """
        <rect x="8" y="8" width="160" height="112" rx="8" fill="#ffffff" stroke="#d0d7de" />
        <text x="16" y="24" font-size="10" fill="#57606a">${type.escapeXml()}</text>
        <text x="16" y="112" font-size="12" font-family="monospace" fill="#24292f">${name.escapeXml()}</text>
        $preview
    """.trimIndent()

    private fun symbolSvg(symbol: SymbolDefinition, title: String, rasterAtlases: RasterAtlasStore): String = svgDocument(
        width = 128,
        height = 128,
        body = """
            <rect width="128" height="128" fill="#ffffff" />
            <text x="8" y="118" font-size="10" font-family="monospace" fill="#57606a">${title.escapeXml()}</text>
            ${symbolPreview(symbol, x = 24.0, y = 16.0, scale = 5.0, rasterAtlases = rasterAtlases)}
        """.trimIndent()
    )

    private fun symbolPreview(
        symbol: SymbolDefinition,
        x: Double,
        y: Double,
        scale: Double,
        rasterAtlases: RasterAtlasStore
    ): String {
        val bitmap = symbol.bitmap
        val dataUri = bitmap?.let { rasterAtlases.dataUri(it) }
        if (bitmap != null && dataUri != null) {
            val fitScale = min(scale, min(76.0 / bitmap.width.coerceAtLeast(1.0), 76.0 / bitmap.height.coerceAtLeast(1.0)))
            val tx = x + SYMBOL_VIEWBOX_PADDING
            val ty = y + SYMBOL_VIEWBOX_PADDING
            return """
                <g transform="translate($tx $ty) scale($fitScale)">
                    <image href="$dataUri" xlink:href="$dataUri" x="0" y="0" width="${bitmap.width}" height="${bitmap.height}" preserveAspectRatio="none" image-rendering="pixelated" />
                </g>
            """.trimIndent()
        }

        if (bitmap != null) {
            return missingBitmapPreview(symbol.name, bitmap, x, y)
        }

        val path = symbol.commands.toSvgPath()
        if (path.isBlank()) {
            return missingVectorPreview(symbol.name, x, y)
        }

        val translateX = x + SYMBOL_VIEWBOX_PADDING
        val translateY = y + SYMBOL_VIEWBOX_PADDING
        return """
            <g transform="translate($translateX $translateY) scale($scale)" fill="none" stroke="#111827" stroke-width="0.45" stroke-linecap="round" stroke-linejoin="round">
                <path d="${path.escapeXml()}" />
            </g>
        """.trimIndent()
    }

    private fun missingBitmapPreview(name: String, bitmap: RasterBitmapDefinition, x: Double, y: Double): String = """
        <g transform="translate(${x + SYMBOL_VIEWBOX_PADDING} ${y + SYMBOL_VIEWBOX_PADDING})">
            <rect x="0" y="0" width="72" height="54" rx="4" fill="#fff7ed" stroke="#f97316" stroke-width="1" />
            <text x="6" y="20" font-size="8" font-family="monospace" fill="#9a3412">bitmap</text>
            <text x="6" y="34" font-size="8" font-family="monospace" fill="#9a3412">atlas missing</text>
            <text x="6" y="48" font-size="7" font-family="monospace" fill="#9a3412">${bitmap.atlasFileName.escapeXml()}</text>
        </g>
    """.trimIndent()

    private fun missingVectorPreview(name: String, x: Double, y: Double): String = """
        <g transform="translate(${x + SYMBOL_VIEWBOX_PADDING} ${y + SYMBOL_VIEWBOX_PADDING})">
            <rect x="0" y="0" width="72" height="54" rx="4" fill="#f6f8fa" stroke="#d0d7de" stroke-width="1" />
            <text x="6" y="24" font-size="8" font-family="monospace" fill="#57606a">no preview</text>
            <text x="6" y="38" font-size="7" font-family="monospace" fill="#57606a">${name.take(8).escapeXml()}</text>
        </g>
    """.trimIndent()

    private fun lineStyleSvg(lineStyle: LineStyleDefinition): String = svgDocument(
        width = 320,
        height = 96,
        body = """
            <rect width="320" height="96" fill="#ffffff" />
            <text x="16" y="24" font-size="14" font-family="monospace" fill="#24292f">${lineStyle.name.escapeXml()}</text>
            <text x="16" y="42" font-size="11" fill="#57606a">${lineStyle.description.escapeXml()}</text>
            ${lineStylePreview(lineStyle, x = 20, y = 66, width = 280)}
        """.trimIndent()
    )

    private fun lineStylePreview(lineStyle: LineStyleDefinition, x: Int, y: Int, width: Int): String {
        val dash = when {
            lineStyle.name.contains("DASH", ignoreCase = true) -> " stroke-dasharray=\"12 8\""
            lineStyle.name.contains("DATCVR", ignoreCase = true) -> " stroke-dasharray=\"4 5\""
            lineStyle.name.contains("LIGHT", ignoreCase = true) -> " stroke-dasharray=\"18 5 4 5\""
            else -> ""
        }
        val color = when {
            lineStyle.name.contains("LIGHT", ignoreCase = true) -> "#d1242f"
            lineStyle.name.contains("COAL", ignoreCase = true) -> "#8a6a16"
            lineStyle.name.contains("DATCVR", ignoreCase = true) -> "#57606a"
            else -> "#111827"
        }
        return "<line x1=\"$x\" y1=\"$y\" x2=\"${x + width}\" y2=\"$y\" stroke=\"$color\" stroke-width=\"5\" stroke-linecap=\"round\"$dash />"
    }

    private fun patternSvg(pattern: PatternDefinition): String = svgDocument(
        width = 160,
        height = 160,
        body = """
            <rect width="160" height="160" fill="#ffffff" />
            ${patternPreview(pattern, x = 24, y = 20, size = 112)}
            <text x="12" y="148" font-size="12" font-family="monospace" fill="#24292f">${pattern.name.escapeXml()}</text>
        """.trimIndent()
    )

    private fun patternPreview(pattern: PatternDefinition, x: Int, y: Int, size: Int): String {
        val color = when {
            pattern.name.contains("DANGER", ignoreCase = true) || pattern.name.contains("WRECK", ignoreCase = true) -> "#d1242f"
            pattern.name.contains("MQUAL", ignoreCase = true) -> "#1a7f37"
            pattern.name.contains("DRG", ignoreCase = true) -> "#0969da"
            pattern.name.contains("NODATA", ignoreCase = true) -> "#6e7781"
            else -> "#8250df"
        }
        val hatch = (x until x + size step 16).joinToString("\n") { hx ->
            "<path d=\"M $hx ${y + size} L ${hx + size / 2} $y\" stroke=\"$color\" stroke-width=\"2\" opacity=\"0.45\" />"
        }
        return """
            <clipPath id="clip-${pattern.name.s52FileName()}"><rect x="$x" y="$y" width="$size" height="$size" rx="6" /></clipPath>
            <rect x="$x" y="$y" width="$size" height="$size" rx="6" fill="#f6f8fa" stroke="#d0d7de" />
            <g clip-path="url(#clip-${pattern.name.s52FileName()})">$hatch</g>
            <text x="${x + size / 2}" y="${y + size / 2 + 4}" text-anchor="middle" font-size="12" font-family="monospace" fill="$color">${pattern.name.take(6).escapeXml()}</text>
        """.trimIndent()
    }

    private fun List<VectorCommand>.toSvgPath(): String = joinToString(" ") { command ->
        when (command) {
            is VectorCommand.MoveTo -> "M ${command.x} ${command.y}"
            is VectorCommand.LineTo -> "L ${command.x} ${command.y}"
            VectorCommand.ClosePath -> "Z"
        }
    }

    private fun uniqueSymbolEntries(symbols: List<SymbolDefinition>): List<SymbolEntry> {
        val seen = mutableMapOf<String, Int>()
        return symbols.map { symbol ->
            val key = symbol.name.uppercase()
            val ordinal = (seen[key] ?: 0) + 1
            seen[key] = ordinal
            val title = if (ordinal == 1) symbol.name else "${symbol.name}#$ordinal"
            val fileStem = if (ordinal == 1) symbol.name.s52FileName() else "${symbol.name.s52FileName()}-$ordinal"
            SymbolEntry(title = title, fileStem = fileStem, symbol = symbol)
        }
    }

    private fun svgDocument(width: Int, height: Int, body: String): String = """
        <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="$width" height="$height" viewBox="0 0 $width $height" role="img">
        $body
        </svg>
    """.trimIndent() + "\n"

    private fun String.s52FileName(): String = lowercase().replace(Regex("[^a-z0-9._-]+"), "-")

    private fun String.escapeXml(): String = buildString(length) {
        for (char in this@escapeXml) {
            append(
                when (char) {
                    '&' -> "&amp;"
                    '<' -> "&lt;"
                    '>' -> "&gt;"
                    '"' -> "&quot;"
                    '\'' -> "&apos;"
                    else -> char
                }
            )
        }
    }

    private data class SymbolEntry(
        val title: String,
        val fileStem: String,
        val symbol: SymbolDefinition
    )

    private class RasterAtlasStore private constructor(private val images: Map<String, BufferedImage>) {
        fun names(): Set<String> = images.keys

        fun dataUri(bitmap: RasterBitmapDefinition): String? {
            val atlas = images[bitmap.atlasFileName] ?: return null
            val x = bitmap.x.toInt().coerceIn(0, atlas.width - 1)
            val y = bitmap.y.toInt().coerceIn(0, atlas.height - 1)
            val w = ceil(bitmap.width).toInt().coerceAtLeast(1).coerceAtMost(atlas.width - x)
            val h = ceil(bitmap.height).toInt().coerceAtLeast(1).coerceAtMost(atlas.height - y)
            if (w <= 0 || h <= 0) return null
            val crop = atlas.getSubimage(x, y, w, h)
            val output = ByteArrayOutputStream()
            ImageIO.write(crop, "png", output)
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray())
        }

        companion object {
            fun locate(): RasterAtlasStore {
                val directory = locateOpenCpnDirectory()
                val images = if (directory == null) {
                    emptyMap()
                } else {
                    listOf("rastersymbols-day.png", "rastersymbols-dusk.png", "rastersymbols-dark.png")
                        .mapNotNull { name ->
                            val file = directory.resolve(name)
                            if (!file.isRegularFile()) {
                                null
                            } else {
                                runCatching { ImageIO.read(file.toFile()) }
                                    .getOrNull()
                                    ?.let { image -> name to image }
                            }
                        }
                        .toMap()
                }
                return RasterAtlasStore(images)
            }

            private fun locateOpenCpnDirectory(): Path? {
                val explicit = System.getProperty("opencpn.assets")
                    ?: System.getenv("OPENCPN_ASSETS_DIR")
                if (!explicit.isNullOrBlank()) {
                    val dir = Path.of(explicit).toAbsolutePath().normalize()
                    if (dir.isDirectory()) return dir
                }

                val start = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
                generateSequence(start) { it.parent }.forEach { dir ->
                    val candidate = dir.resolve("s52/opencpn")
                    if (candidate.resolve("rastersymbols-day.png").isRegularFile()) return candidate
                    val sibling = dir.resolve("../s52/opencpn").normalize()
                    if (sibling.resolve("rastersymbols-day.png").isRegularFile()) return sibling
                }
                return null
            }
        }
    }
}
