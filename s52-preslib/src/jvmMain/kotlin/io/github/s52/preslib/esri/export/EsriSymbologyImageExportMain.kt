package io.github.s52.preslib.esri.export

import io.github.s52.preslib.esri.importer.EsriSourceLayout
import io.github.s52.preslib.esri.importer.EsriSvgCategory
import io.github.s52.preslib.esri.svg.EsriGeneratedPaint
import io.github.s52.preslib.esri.svg.EsriGeneratedSvgMesh
import io.github.s52.preslib.esri.svg.EsriSvgDocument
import io.github.s52.preslib.esri.svg.EsriSvgMeshGenerator
import io.github.s52.preslib.esri.svg.EsriSvgParser
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.system.exitProcess

/**
 * Exports ESRI/INT1 symbology artifacts in the same handoff style as the
 * OpenCPN symbology image export:
 *
 * - index.html
 * - manifest.properties
 * - source SVG copies under symbols/, lines/, and patterns/
 * - symbol-atlas-day.png
 * - symbol-atlas-dusk.png
 * - symbol-atlas-dark.png
 * - JSON report under build/reports/esri
 *
 * The PNG atlases are generated from the same build-time SVG meshes used by
 * the generated Kotlin vector-symbol path. Runtime chart rendering still uses
 * generated Kotlin/WebGL meshes; these atlases are for CI artifacts, visual
 * review, and release handoff compatibility.
 */
object EsriSymbologyImageExportMain {
    private const val CellPx = 72
    private const val PaddingPx = 6

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 3) {
            "Usage: EsriSymbologyImageExportMain <esri-source-dir> <output-dir> <report-dir>"
        }

        val sourceDir = File(args[0])
        val outputDir = File(args[1])
        val reportDir = File(args[2])
        reportDir.mkdirs()

        val layout = EsriSourceLayout(sourceDir)
        layout.requireUsable()

        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val symbolsOut = outputDir.resolve("symbols").apply { mkdirs() }
        val linesOut = outputDir.resolve("lines").apply { mkdirs() }
        val patternsOut = outputDir.resolve("patterns").apply { mkdirs() }

        val sourceSvgs = layout.svgFiles()
        val copied = sourceSvgs.map { svg ->
            val category = layout.svgCategory(svg)
            val targetDir = when (category) {
                EsriSvgCategory.POINT -> symbolsOut
                EsriSvgCategory.LINE -> linesOut
                EsriSvgCategory.PATTERN -> patternsOut
                EsriSvgCategory.UNKNOWN -> outputDir.resolve("unknown").apply { mkdirs() }
            }
            svg.copyTo(targetDir.resolve(svg.name), overwrite = true)
            svg to category
        }

        val pointAssets = copied
            .filter { (_, category) -> category == EsriSvgCategory.POINT }
            .map { (svg, _) -> loadPointAsset(sourceDir, svg) }

        val renderable = pointAssets.filterIsInstance<AtlasPointAsset>()
        val failures = pointAssets.filterIsInstance<AtlasPointFailure>()

        val atlasLayout = AtlasLayout(renderable.size, CellPx)
        val palettes = listOf(
            EsriAtlasPalette.DAY,
            EsriAtlasPalette.DUSK,
            EsriAtlasPalette.DARK
        )
        palettes.forEach { palette ->
            val image = EsriRgbaImage(atlasLayout.widthPx, atlasLayout.heightPx)
            renderable.forEachIndexed { index, asset ->
                val col = index % atlasLayout.columns
                val row = index / atlasLayout.columns
                drawAsset(image, asset, col * CellPx, row * CellPx, palette)
            }
            image.writePng(outputDir.resolve("symbol-atlas-${palette.fileSuffix}.png"))
        }

        writeManifest(
            outputDir.resolve("manifest.properties"),
            sourceDir,
            copied,
            renderable,
            failures,
            atlasLayout
        )
        writeIndex(outputDir.resolve("index.html"), copied, renderable, failures, atlasLayout)
        writeReport(reportDir.resolve("esri-symbology-images.json"), outputDir, copied, renderable, failures, atlasLayout)

        if (renderable.isEmpty()) {
            System.err.println("No ESRI point symbols could be rendered into atlases from ${sourceDir.path}")
            exitProcess(1)
        }
        if (failures.isNotEmpty()) {
            System.err.println("ESRI atlas export skipped ${failures.size} point symbols. See ${reportDir.resolve("esri-symbology-images.json")}")
        }
        println("Exported ESRI symbology images to ${outputDir.path}; point atlas symbols=${renderable.size}, skipped=${failures.size}")
    }

    private fun loadPointAsset(sourceDir: File, svg: File): AtlasPointResult {
        return try {
            val document = EsriSvgParser.parse(svg, EsriSvgCategory.POINT.name)
            if (!document.isSubsetSupported) {
                AtlasPointFailure(svg.name, svg.relativeTo(sourceDir).invariantSeparatorsPath, "unsupported SVG subset")
            } else if (document.viewBox == null || !document.viewBox.isValid) {
                AtlasPointFailure(svg.name, svg.relativeTo(sourceDir).invariantSeparatorsPath, "missing or invalid viewBox")
            } else {
                val meshes = EsriSvgMeshGenerator.generate(document)
                if (meshes.none { it.isRenderable }) {
                    AtlasPointFailure(svg.name, svg.relativeTo(sourceDir).invariantSeparatorsPath, "no renderable mesh")
                } else {
                    AtlasPointAsset(svg.name, svg.relativeTo(sourceDir).invariantSeparatorsPath, document, meshes.filter { it.isRenderable })
                }
            }
        } catch (exc: Exception) {
            AtlasPointFailure(svg.name, svg.relativeTo(sourceDir).invariantSeparatorsPath, exc.message ?: exc::class.simpleName.orEmpty())
        }
    }

    private fun drawAsset(image: EsriRgbaImage, asset: AtlasPointAsset, x0: Int, y0: Int, palette: EsriAtlasPalette) {
        val viewBox = asset.document.viewBox ?: return
        val scale = min(
            (CellPx - PaddingPx * 2).toDouble() / viewBox.width,
            (CellPx - PaddingPx * 2).toDouble() / viewBox.height
        )
        val ox = x0 + (CellPx - viewBox.width * scale) / 2.0
        val oy = y0 + (CellPx - viewBox.height * scale) / 2.0
        asset.meshes.forEach { mesh ->
            val color = palette.resolve(mesh.paint)
            if (color.a == 0) return@forEach
            val vertices = mesh.vertices
            val indices = mesh.indices
            var i = 0
            while (i + 2 < indices.size) {
                val ia = indices[i].toInt() * 2
                val ib = indices[i + 1].toInt() * 2
                val ic = indices[i + 2].toInt() * 2
                val ax = ox + (vertices[ia] - viewBox.minX) * scale
                val ay = oy + (vertices[ia + 1] - viewBox.minY) * scale
                val bx = ox + (vertices[ib] - viewBox.minX) * scale
                val by = oy + (vertices[ib + 1] - viewBox.minY) * scale
                val cx = ox + (vertices[ic] - viewBox.minX) * scale
                val cy = oy + (vertices[ic + 1] - viewBox.minY) * scale
                image.fillTriangle(ax, ay, bx, by, cx, cy, color)
                i += 3
            }
        }
    }

    private fun writeManifest(
        file: File,
        sourceDir: File,
        copied: List<Pair<File, EsriSvgCategory>>,
        renderable: List<AtlasPointAsset>,
        failures: List<AtlasPointFailure>,
        atlasLayout: AtlasLayout
    ) {
        file.writeText(buildString {
            appendLine("kind=esri-symbology-images")
            appendLine("edition=esri-nautical-chart-symbols")
            appendLine("generatedAt=${Instant.now()}")
            appendLine("sourceDir=${sourceDir.invariantSeparatorsPath}")
            appendLine("synthetic=false")
            appendLine("svgSource=true")
            appendLine("runtimePath=generated-kotlin-vector-mesh")
            appendLine("pngSymbolAtlases=3")
            appendLine("symbols=${copied.count { it.second == EsriSvgCategory.POINT }}")
            appendLine("lines=${copied.count { it.second == EsriSvgCategory.LINE }}")
            appendLine("patterns=${copied.count { it.second == EsriSvgCategory.PATTERN }}")
            appendLine("atlasSymbols=${renderable.size}")
            appendLine("atlasSkipped=${failures.size}")
            appendLine("atlasCellPx=$CellPx")
            appendLine("atlasColumns=${atlasLayout.columns}")
            appendLine("atlasRows=${atlasLayout.rows}")
            appendLine("atlasWidthPx=${atlasLayout.widthPx}")
            appendLine("atlasHeightPx=${atlasLayout.heightPx}")
        })
    }

    private fun writeIndex(
        file: File,
        copied: List<Pair<File, EsriSvgCategory>>,
        renderable: List<AtlasPointAsset>,
        failures: List<AtlasPointFailure>,
        atlasLayout: AtlasLayout
    ) {
        val pointNames = copied.filter { it.second == EsriSvgCategory.POINT }.map { it.first.name }
        val lineNames = copied.filter { it.second == EsriSvgCategory.LINE }.map { it.first.name }
        val patternNames = copied.filter { it.second == EsriSvgCategory.PATTERN }.map { it.first.name }
        file.writeText(buildString {
            appendLine("<!doctype html>")
            appendLine("<html><head><meta charset=\"utf-8\"><title>ESRI S-52 Symbology Images</title>")
            appendLine("<style>body{font-family:system-ui,sans-serif;margin:24px} img{max-width:100%;height:auto;border:1px solid #ccc} code{background:#f5f5f5;padding:2px 4px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(120px,1fr));gap:10px}.card{border:1px solid #ddd;padding:8px;overflow:hidden}.card img{width:72px;height:72px;object-fit:contain;border:0}</style>")
            appendLine("</head><body>")
            appendLine("<h1>ESRI / INT1 symbology image export</h1>")
            appendLine("<p>Generated from ESRI SVG assets. Runtime chart rendering should use generated Kotlin vector meshes; the PNG atlases are release/debug artifacts.</p>")
            appendLine("<ul>")
            appendLine("<li>Point SVGs: ${pointNames.size}</li>")
            appendLine("<li>Line SVGs: ${lineNames.size}</li>")
            appendLine("<li>Pattern SVGs: ${patternNames.size}</li>")
            appendLine("<li>Atlas-rendered point symbols: ${renderable.size}</li>")
            appendLine("<li>Skipped point symbols: ${failures.size}</li>")
            appendLine("<li>Atlas grid: ${atlasLayout.columns} × ${atlasLayout.rows}, cell ${CellPx}px</li>")
            appendLine("</ul>")
            appendLine("<h2>Symbol atlases</h2>")
            appendLine("<h3>Day</h3><img src=\"symbol-atlas-day.png\" alt=\"ESRI day symbol atlas\">")
            appendLine("<h3>Dusk</h3><img src=\"symbol-atlas-dusk.png\" alt=\"ESRI dusk symbol atlas\">")
            appendLine("<h3>Dark</h3><img src=\"symbol-atlas-dark.png\" alt=\"ESRI dark symbol atlas\">")
            appendLine("<h2>Point symbols</h2><div class=\"grid\">")
            pointNames.take(500).forEach { name ->
                appendLine("<div class=\"card\"><img src=\"symbols/${html(name)}\" alt=\"${html(name)}\"><br><code>${html(name)}</code></div>")
            }
            appendLine("</div>")
            if (pointNames.size > 500) appendLine("<p>Only first 500 point SVG previews shown.</p>")
            appendLine("</body></html>")
        })
    }

    private fun writeReport(
        file: File,
        outputDir: File,
        copied: List<Pair<File, EsriSvgCategory>>,
        renderable: List<AtlasPointAsset>,
        failures: List<AtlasPointFailure>,
        atlasLayout: AtlasLayout
    ) {
        file.writeText(buildString {
            appendLine("{")
            appendLine("  \"outputDir\": \"${json(outputDir.invariantSeparatorsPath)}\",")
            appendLine("  \"pointSvgCount\": ${copied.count { it.second == EsriSvgCategory.POINT }},")
            appendLine("  \"lineSvgCount\": ${copied.count { it.second == EsriSvgCategory.LINE }},")
            appendLine("  \"patternSvgCount\": ${copied.count { it.second == EsriSvgCategory.PATTERN }},")
            appendLine("  \"atlasSymbolCount\": ${renderable.size},")
            appendLine("  \"atlasSkippedCount\": ${failures.size},")
            appendLine("  \"atlas\": {\"cellPx\": $CellPx, \"columns\": ${atlasLayout.columns}, \"rows\": ${atlasLayout.rows}, \"widthPx\": ${atlasLayout.widthPx}, \"heightPx\": ${atlasLayout.heightPx}},")
            appendLine("  \"failures\": [")
            failures.forEachIndexed { index, failure ->
                append("    {\"name\": \"${json(failure.name)}\", \"relativePath\": \"${json(failure.relativePath)}\", \"reason\": \"${json(failure.reason)}\"}")
                if (index != failures.lastIndex) append(',')
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        })
    }

    private fun html(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun json(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
}

private sealed interface AtlasPointResult
private data class AtlasPointAsset(
    val name: String,
    val relativePath: String,
    val document: EsriSvgDocument,
    val meshes: List<EsriGeneratedSvgMesh>
) : AtlasPointResult

private data class AtlasPointFailure(
    val name: String,
    val relativePath: String,
    val reason: String
) : AtlasPointResult

private data class AtlasLayout(val itemCount: Int, val cellPx: Int) {
    val columns: Int = max(1, ceil(sqrt(itemCount.toDouble())).toInt())
    val rows: Int = max(1, ceil(itemCount.toDouble() / columns.toDouble()).toInt())
    val widthPx: Int = columns * cellPx
    val heightPx: Int = rows * cellPx
}

private enum class EsriAtlasPalette(val fileSuffix: String) {
    DAY("day"),
    DUSK("dusk"),
    DARK("dark");

    fun resolve(paint: EsriGeneratedPaint): Rgba = when (paint) {
        is EsriGeneratedPaint.Token -> token(paint.token)
        is EsriGeneratedPaint.LiteralHex -> literal(paint.hex)
        EsriGeneratedPaint.None -> Rgba(0, 0, 0, 0)
    }

    private fun token(token: String): Rgba = when (token.uppercase(Locale.US)) {
        "CHBLK" -> when (this) {
            DAY -> Rgba(35, 31, 32, 255)
            DUSK -> Rgba(20, 20, 20, 255)
            DARK -> Rgba(220, 220, 220, 255)
        }
        "CHWHT" -> when (this) {
            DAY -> Rgba(255, 255, 255, 255)
            DUSK -> Rgba(230, 220, 205, 255)
            DARK -> Rgba(20, 20, 20, 255)
        }
        else -> Rgba(35, 31, 32, 255)
    }

    private fun literal(raw: String): Rgba {
        val value = raw.trim().lowercase(Locale.US)
        val hex = when {
            value.startsWith("#") -> value.drop(1)
            else -> return token("CHBLK")
        }
        val normalized = when (hex.length) {
            3 -> hex.flatMap { listOf(it, it) }.joinToString("")
            6 -> hex
            else -> return token("CHBLK")
        }
        val r = normalized.substring(0, 2).toIntOrNull(16) ?: return token("CHBLK")
        val g = normalized.substring(2, 4).toIntOrNull(16) ?: return token("CHBLK")
        val b = normalized.substring(4, 6).toIntOrNull(16) ?: return token("CHBLK")
        return when (this) {
            DAY -> Rgba(r, g, b, 255)
            DUSK -> Rgba((r * 0.82).roundToInt(), (g * 0.78).roundToInt(), (b * 0.72).roundToInt(), 255)
            DARK -> {
                val luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b).roundToInt()
                if (luminance < 80) Rgba(218, 218, 218, 255) else Rgba((r * 0.45).roundToInt(), (g * 0.45).roundToInt(), (b * 0.45).roundToInt(), 255)
            }
        }
    }
}

private data class Rgba(val r: Int, val g: Int, val b: Int, val a: Int)

private class EsriRgbaImage(val width: Int, val height: Int) {
    private val pixels = ByteArray(width * height * 4)

    fun fillTriangle(ax: Double, ay: Double, bx: Double, by: Double, cx: Double, cy: Double, color: Rgba) {
        val minX = max(0, floor(min(ax, min(bx, cx))).toInt())
        val maxX = min(width - 1, ceil(max(ax, max(bx, cx))).toInt())
        val minY = max(0, floor(min(ay, min(by, cy))).toInt())
        val maxY = min(height - 1, ceil(max(ay, max(by, cy))).toInt())
        val area = edge(ax, ay, bx, by, cx, cy)
        if (kotlin.math.abs(area) < 1.0e-9) return
        for (y in minY..maxY) {
            for (x in minX..maxX) {
                val px = x + 0.5
                val py = y + 0.5
                val w0 = edge(bx, by, cx, cy, px, py)
                val w1 = edge(cx, cy, ax, ay, px, py)
                val w2 = edge(ax, ay, bx, by, px, py)
                val hasNeg = w0 < 0.0 || w1 < 0.0 || w2 < 0.0
                val hasPos = w0 > 0.0 || w1 > 0.0 || w2 > 0.0
                if (!(hasNeg && hasPos)) blendPixel(x, y, color)
            }
        }
    }

    private fun edge(ax: Double, ay: Double, bx: Double, by: Double, px: Double, py: Double): Double =
        (px - ax) * (by - ay) - (py - ay) * (bx - ax)

    private fun blendPixel(x: Int, y: Int, src: Rgba) {
        if (src.a <= 0) return
        val idx = (y * width + x) * 4
        if (src.a >= 255 || pixels[idx + 3].toInt() and 0xff == 0) {
            pixels[idx] = src.r.coerceIn(0, 255).toByte()
            pixels[idx + 1] = src.g.coerceIn(0, 255).toByte()
            pixels[idx + 2] = src.b.coerceIn(0, 255).toByte()
            pixels[idx + 3] = src.a.coerceIn(0, 255).toByte()
            return
        }
        val da = pixels[idx + 3].toInt() and 0xff
        val sa = src.a
        val outA = sa + da * (255 - sa) / 255
        fun blend(sc: Int, dc: Int): Int = (sc * sa + dc * da * (255 - sa) / 255) / max(1, outA)
        pixels[idx] = blend(src.r, pixels[idx].toInt() and 0xff).toByte()
        pixels[idx + 1] = blend(src.g, pixels[idx + 1].toInt() and 0xff).toByte()
        pixels[idx + 2] = blend(src.b, pixels[idx + 2].toInt() and 0xff).toByte()
        pixels[idx + 3] = outA.coerceIn(0, 255).toByte()
    }

    fun writePng(file: File) {
        file.parentFile.mkdirs()
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a))
        writeChunk(out, "IHDR", ByteBuffer.allocate(13).order(ByteOrder.BIG_ENDIAN).apply {
            putInt(width)
            putInt(height)
            put(8.toByte()) // bit depth
            put(6.toByte()) // RGBA
            put(0.toByte())
            put(0.toByte())
            put(0.toByte())
        }.array())

        val raw = ByteArrayOutputStream()
        for (y in 0 until height) {
            raw.write(0) // filter type None
            raw.write(pixels, y * width * 4, width * 4)
        }
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        deflater.setInput(raw.toByteArray())
        deflater.finish()
        val compressed = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (!deflater.finished()) {
            val n = deflater.deflate(buffer)
            compressed.write(buffer, 0, n)
        }
        deflater.end()
        writeChunk(out, "IDAT", compressed.toByteArray())
        writeChunk(out, "IEND", ByteArray(0))
        file.writeBytes(out.toByteArray())
    }

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        out.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(data.size).array())
        out.write(typeBytes)
        out.write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        out.write(ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(crc.value.toInt()).array())
    }
}
