package io.github.s52.preslib.esri.export

import io.github.s52.preslib.esri.importer.EsriCustomSymbolMapParser
import io.github.s52.preslib.esri.importer.EsriSourceLayout
import io.github.s52.preslib.esri.importer.EsriSvgCategory
import io.github.s52.preslib.esri.svg.EsriGeneratedPaint
import io.github.s52.preslib.esri.svg.EsriGeneratedSvgMesh
import io.github.s52.preslib.esri.svg.EsriSvgDocument
import io.github.s52.preslib.esri.svg.EsriSvgMeshGenerator
import io.github.s52.preslib.esri.svg.EsriSvgParser
import io.github.s52.preslib.esri.svg.EsriSvgViewBox
import io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib
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
 * Exports an ESRI/INT1 symbology artifact with the *OpenCPN atlas contract*.
 *
 * The exported object list is no longer driven by whatever SVG files happen to
 * exist in the ESRI repository.  OpenCPN remains the coverage oracle: every
 * OpenCPN symbol, line style, and pattern name from OpenCpnGeneratedPresLib is
 * emitted with the same external name.  Each OpenCPN slot is then resolved to
 * the best available ESRI SVG by explicit alias, CustomSymbolMap object rule,
 * exact file-name match, or semantic token match.  Unmatched slots are
 * emitted as unresolved/blank placeholders instead of copying generic fallback
 * art into many unrelated OpenCPN names.
 *
 * That means downstream CI/release artifacts can be compared 1:1 against the
 * OpenCPN atlas by name/count/order while still reviewing the ESRI rendering.
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

        val openCpnPack = OpenCpnGeneratedPresLib.sourcePack()
        val openCpnSymbols = openCpnPack.symbols.map { it.name }.distinct()
        val openCpnLines = openCpnPack.lineStyles.map { it.name }.distinct()
        val openCpnPatterns = openCpnPack.patterns.map { it.name }.distinct()

        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val symbolsOut = outputDir.resolve("symbols").apply { mkdirs() }
        val linesOut = outputDir.resolve("lines").apply { mkdirs() }
        val patternsOut = outputDir.resolve("patterns").apply { mkdirs() }
        val enhancedOut = outputDir.resolve("enhanced-svg").apply { mkdirs() }

        val resolver = EsriOpenCpnAtlasResolver(layout, rootAliasDir = detectAliasDir(sourceDir))

        val symbolSlots = openCpnSymbols.map { resolver.resolve(it, EsriSvgCategory.POINT) }
        val lineSlots = openCpnLines.map { resolver.resolve(it, EsriSvgCategory.LINE) }
        val patternSlots = openCpnPatterns.map { resolver.resolve(it, EsriSvgCategory.PATTERN) }
        val objectSlots = buildObjectSlots(openCpnPack.lookupRecords, resolver)

        val objectsOut = outputDir.resolve("objects").apply { mkdirs() }
        copyResolvedSvgSlots(symbolSlots, symbolsOut)
        copyResolvedSvgSlots(lineSlots, linesOut)
        copyResolvedSvgSlots(patternSlots, patternsOut)
        copyResolvedObjectSlots(objectSlots, objectsOut)
        writeEnhancedSvgSlots(symbolSlots, enhancedOut.resolve("symbols").apply { mkdirs() })
        writeEnhancedSvgSlots(lineSlots, enhancedOut.resolve("lines").apply { mkdirs() })
        writeEnhancedSvgSlots(patternSlots, enhancedOut.resolve("patterns").apply { mkdirs() })
        writeEnhancedObjectSlots(objectSlots, enhancedOut.resolve("objects").apply { mkdirs() })

        val pointResults = symbolSlots.map { slot -> loadPointAsset(slot) }
        val renderable = pointResults.filterIsInstance<AtlasPointAsset>()
        val failures = pointResults.filterIsInstance<AtlasPointFailure>()

        val atlasLayout = AtlasLayout(symbolSlots.size, CellPx)
        val palettes = listOf(EsriAtlasPalette.DAY, EsriAtlasPalette.DUSK, EsriAtlasPalette.DARK)
        palettes.forEach { palette ->
            val image = EsriRgbaImage(atlasLayout.widthPx, atlasLayout.heightPx)
            renderable.forEach { asset ->
                val col = asset.slotIndex % atlasLayout.columns
                val row = asset.slotIndex / atlasLayout.columns
                drawAsset(image, asset, col * CellPx, row * CellPx, palette)
            }
            image.writePng(outputDir.resolve("symbol-atlas-${palette.fileSuffix}.png"))
        }

        writeManifest(
            outputDir.resolve("manifest.properties"),
            sourceDir,
            symbolSlots,
            lineSlots,
            patternSlots,
            objectSlots,
            renderable,
            failures,
            atlasLayout
        )
        writeIndex(outputDir.resolve("index.html"), symbolSlots, lineSlots, patternSlots, objectSlots, renderable, failures, atlasLayout)
        writeAtlasScreenshotPage(outputDir.resolve("atlas-screenshot.html"), atlasLayout)
        writeReport(
            reportDir.resolve("esri-opencpn-atlas-match.json"),
            outputDir,
            symbolSlots,
            lineSlots,
            patternSlots,
            objectSlots,
            renderable,
            failures,
            atlasLayout
        )
        writeCsv(reportDir.resolve("esri-opencpn-atlas-match.csv"), symbolSlots + lineSlots + patternSlots)
        writeObjectCsv(reportDir.resolve("esri-opencpn-object-match.csv"), objectSlots)

        if (symbolSlots.isEmpty()) {
            System.err.println("OpenCPN generated symbology has no symbols; cannot build ESRI matched atlas")
            exitProcess(1)
        }
        if (failures.isNotEmpty()) {
            System.err.println("WARNING: ESRI matched atlas has ${failures.size} OpenCPN symbol slot(s) not rasterized. See ${reportDir.resolve("esri-opencpn-atlas-match.json")}")
        }
        val unresolvedSlots = (symbolSlots + lineSlots + patternSlots).count { it.matchKind == MatchKind.UNRESOLVED }
        if (unresolvedSlots > 0) {
            System.err.println("WARNING: ESRI matched atlas has $unresolvedSlots unresolved OpenCPN slot(s). No generic fallback SVGs were substituted; add explicit aliases in s52/esri/*.tsv to close them.")
        }
        println("Exported OpenCPN-name-compatible ESRI atlas to ${outputDir.path}; OpenCPN symbols=${symbolSlots.size}, rendered=${renderable.size}, lines=${lineSlots.size}, patterns=${patternSlots.size}")
    }

    private fun detectAliasDir(sourceDir: File): File {
        val candidates = listOfNotNull(
            sourceDir.parentFile,
            File("../s52/esri"),
            File("s52/esri"),
            File("../../s52/esri")
        )
        return candidates.firstOrNull { it.resolve("esri-symbol-aliases.tsv").isFile || it.resolve("esri-line-aliases.tsv").isFile || it.resolve("esri-pattern-aliases.tsv").isFile }
            ?: candidates.first()
    }

    private fun copyResolvedSvgSlots(slots: List<OpenCpnEsriSlot>, outputDir: File) {
        slots.forEach { slot ->
            val fileName = "${safeFileName(slot.openCpnName)}.svg"
            val target = outputDir.resolve(fileName)
            val source = slot.esriFile
            if (source != null && source.isFile) {
                val metadata = "OpenCPN slot: ${slot.openCpnName}; ESRI source: ${slot.esriName}; match: ${slot.matchKind.name}; reason: ${slot.reason}"
                target.writeText(esriSvgCopyAsStandaloneXml(source.readText(Charsets.UTF_8), metadata), Charsets.UTF_8)
            } else {
                target.writeText(unresolvedSvg(slot.openCpnName, slot.reason), Charsets.UTF_8)
            }
        }
    }

    private fun copyResolvedObjectSlots(slots: List<OpenCpnEsriObjectSlot>, outputDir: File) {
        slots.forEach { objectSlot ->
            val target = outputDir.resolve("${safeFileName(objectSlot.objectAcronym)}.svg")
            val source = objectSlot.assetSlot.esriFile
            if (source != null && source.isFile) {
                val metadata = "OpenCPN object: ${objectSlot.objectAcronym}; primitive: ${objectSlot.primitive}; OpenCPN instruction asset: ${objectSlot.openCpnAssetName}; ESRI source: ${objectSlot.assetSlot.esriName}; match: ${objectSlot.assetSlot.matchKind.name}; reason: ${objectSlot.assetSlot.reason}"
                target.writeText(esriSvgCopyAsStandaloneXml(source.readText(Charsets.UTF_8), metadata), Charsets.UTF_8)
            } else {
                target.writeText(unresolvedSvg(objectSlot.objectAcronym, objectSlot.assetSlot.reason), Charsets.UTF_8)
            }
        }
    }

    private fun writeEnhancedSvgSlots(slots: List<OpenCpnEsriSlot>, outputDir: File) {
        slots.forEach { slot ->
            val target = outputDir.resolve("${safeFileName(slot.openCpnName)}.svg")
            target.writeText(enhancedSvgForSlot(slot), Charsets.UTF_8)
        }
    }

    private fun writeEnhancedObjectSlots(slots: List<OpenCpnEsriObjectSlot>, outputDir: File) {
        slots.forEach { objectSlot ->
            val target = outputDir.resolve("${safeFileName(objectSlot.objectAcronym)}.svg")
            target.writeText(enhancedSvgForSlot(objectSlot.assetSlot.copy(openCpnName = objectSlot.objectAcronym)), Charsets.UTF_8)
        }
    }

    private fun enhancedSvgForSlot(slot: OpenCpnEsriSlot): String {
        val source = slot.esriFile
        return if (source != null && source.isFile) {
            enhancedEsriSvgForOpenCpn(
                sourceXml = source.readText(Charsets.UTF_8),
                openCpnName = slot.openCpnName,
                category = slot.category.name,
                esriName = slot.esriName.orEmpty(),
                matchKind = slot.matchKind.name,
                reason = slot.reason
            )
        } else {
            enhancedPlaceholderSvg(slot.openCpnName, slot.category.name, slot.reason)
        }
    }

    internal fun enhancedEsriSvgForOpenCpn(
        sourceXml: String,
        openCpnName: String,
        category: String,
        esriName: String,
        matchKind: String,
        reason: String
    ): String {
        val colors = enhancedColors(openCpnName, category)
        val metadata = "OpenCPN enhanced slot: $openCpnName; ESRI source: $esriName; match: $matchKind; reason: $reason"
        var body = esriSvgCopyAsStandaloneXml(sourceXml, metadata).trimEnd()
        body = body.replaceFirst(
            Regex("""<svg\b""", RegexOption.IGNORE_CASE),
            "<svg data-opencpn-name=\"${html(openCpnName)}\" data-enhanced-svg=\"true\""
        )
        body = replacePaint(body, "fill", colors.primary)
        body = replacePaint(body, "stroke", colors.outline)
        body = body.replace(
            Regex("""<path\b(?![^>]*(?:fill|style)=)""", RegexOption.IGNORE_CASE),
            "<path fill=\"${colors.primary}\""
        )
        body = body.replace(
            Regex("""<path\b(?![^>]*(?:stroke|style)=)""", RegexOption.IGNORE_CASE),
            "<path stroke=\"${colors.outline}\" stroke-width=\"0.7\""
        )
        val viewBox = parseSvgViewBox(body) ?: EsriSvgViewBox(0.0, 0.0, 72.0, 72.0)
        val overlay = enhancedOverlay(openCpnName, category, colors, viewBox)
        return body.replaceBeforeClosingSvg(overlay)
    }

    internal fun enhancedPlaceholderSvg(name: String, category: String, reason: String): String {
        val colors = enhancedColors(name, category)
        val chipX = 52 + (stableHash(name) % 9)
        val chipY = 10 + ((stableHash(name) / 11) % 9)
        return """
            <svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" viewBox="0 0 72 72" data-opencpn-name="${html(name)}" data-match-kind="UNRESOLVED" data-enhanced-svg="true">
              <title>${html(name)} enhanced unresolved ESRI mapping</title>
              <desc>${html(reason)}</desc>
              <circle cx="36" cy="36" r="24" fill="none" stroke="${colors.outline}" stroke-width="3" stroke-dasharray="7 4"/>
              <path d="M21 48 L36 18 L51 48 Z" fill="${colors.primary}" fill-opacity="0.72" stroke="${colors.outline}" stroke-width="2"/>
              <circle cx="$chipX" cy="$chipY" r="5" fill="${colors.accent}" stroke="${colors.outline}" stroke-width="1"/>
            </svg>
        """.trimIndent()
    }

    private fun replacePaint(svg: String, property: String, color: String): String {
        val attr = Regex("""($property\s*=\s*["'])(?!none["'])[^"']+(["'])""", setOf(RegexOption.IGNORE_CASE))
        val style = Regex("""($property\s*:\s*)(?!none(?:;|$))[^;"']+""", setOf(RegexOption.IGNORE_CASE))
        return style.replace(attr.replace(svg) { it.groupValues[1] + color + it.groupValues[2] }) { it.groupValues[1] + color }
    }

    private fun enhancedOverlay(name: String, category: String, colors: EnhancedColors, viewBox: EsriSvgViewBox): String {
        val marker = viewBox.width.coerceAtMost(viewBox.height) * 0.085
        val x = viewBox.minX + viewBox.width - marker * (1.8 + (stableHash(name) % 3) * 0.35)
        val y = viewBox.minY + marker * (1.6 + ((stableHash(name) / 7) % 3) * 0.35)
        val stroke = (viewBox.width.coerceAtMost(viewBox.height) * 0.018).coerceAtLeast(0.4)
        val categoryShape = when (category.uppercase(Locale.US)) {
            "LINE" -> """<path d="M${fmt(x - marker)} ${fmt(y)} H${fmt(x + marker)}" stroke="${colors.accent}" stroke-width="${fmt(stroke * 2.2)}" stroke-linecap="round"/>"""
            "PATTERN" -> """<rect x="${fmt(x - marker)}" y="${fmt(y - marker)}" width="${fmt(marker * 2)}" height="${fmt(marker * 2)}" fill="${colors.accent}" stroke="${colors.outline}" stroke-width="${fmt(stroke)}"/>"""
            else -> """<circle cx="${fmt(x)}" cy="${fmt(y)}" r="${fmt(marker)}" fill="${colors.accent}" stroke="${colors.outline}" stroke-width="${fmt(stroke)}"/>"""
        }
        return """
          <g id="opencpn-enhancement" fill="none" pointer-events="none">
            <rect x="${fmt(viewBox.minX + stroke)}" y="${fmt(viewBox.minY + stroke)}" width="${fmt(viewBox.width - stroke * 2)}" height="${fmt(viewBox.height - stroke * 2)}" rx="${fmt(marker * 0.65)}" ry="${fmt(marker * 0.65)}" stroke="${colors.category}" stroke-width="${fmt(stroke)}" stroke-opacity="0.55"/>
            $categoryShape
          </g>
        """.trimIndent()
    }

    private fun String.replaceBeforeClosingSvg(fragment: String): String =
        replace(Regex("""</svg>\s*$""", RegexOption.IGNORE_CASE), "$fragment\n</svg>")

    private fun parseSvgViewBox(svg: String): EsriSvgViewBox? {
        val raw = Regex("""viewBox\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(svg)?.groupValues?.get(1) ?: return null
        val values = raw.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toDoubleOrNull() }
        return if (values.size == 4 && values[2] > 0.0 && values[3] > 0.0) EsriSvgViewBox(values[0], values[1], values[2], values[3]) else null
    }

    private fun enhancedColors(name: String, category: String): EnhancedColors {
        val upper = name.uppercase(Locale.US)
        val base = when {
            "LIGHT" in upper || upper.startsWith("LIT") -> "#ffd21f" to "#b05a00"
            "WRECK" in upper -> "#111111" to "#2a7fff"
            "OBSTR" in upper || "DANGER" in upper -> "#d1007a" to "#111111"
            "BOY" in upper || "BUOY" in upper -> if (stableHash(name) % 2 == 0) "#e21b2d" to "#0b7f3a" else "#0b7f3a" to "#e21b2d"
            "BCN" in upper || "BEACON" in upper -> "#f2c400" to "#111111"
            "TOP" in upper -> "#111111" to "#f2c400"
            "ACH" in upper || "ANCH" in upper -> "#8b4bd6" to "#111111"
            "CBL" in upper || "CABLE" in upper -> "#7a4a21" to "#111111"
            "DEP" in upper || "DRG" in upper || "SOUND" in upper -> "#5da9e9" to "#003f73"
            "SAND" in upper || "SBDARE" in upper -> "#c9a76a" to "#7a5a1a"
            else -> hashColor(name) to "#111111"
        }
        val categoryColor = when (category.uppercase(Locale.US)) {
            "LINE" -> "#8a5a00"
            "PATTERN" -> "#006d77"
            else -> "#324a8f"
        }
        return EnhancedColors(primary = base.first, accent = base.second, outline = "#111111", category = categoryColor)
    }

    private fun hashColor(value: String): String {
        val hue = stableHash(value) % 360
        val c = 0.62
        val x = c * (1.0 - kotlin.math.abs((hue / 60.0) % 2.0 - 1.0))
        val (r1, g1, b1) = when (hue / 60) {
            0 -> Triple(c, x, 0.0)
            1 -> Triple(x, c, 0.0)
            2 -> Triple(0.0, c, x)
            3 -> Triple(0.0, x, c)
            4 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        val m = 0.28
        fun ch(v: Double) = ((v + m) * 255).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0')
        return "#${ch(r1)}${ch(g1)}${ch(b1)}"
    }

    private fun stableHash(value: String): Int = value.fold(0) { acc, ch -> (acc * 31 + ch.code) and 0x7fffffff }

    private fun fmt(value: Double): String = String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')


    private fun unresolvedSvg(name: String, reason: String): String = """
        <svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" viewBox="0 0 72 72" data-opencpn-name="${html(name)}" data-match-kind="UNRESOLVED">
          <title>${html(name)} unresolved ESRI mapping</title>
          <desc>${html(reason)}</desc>
        </svg>
    """.trimIndent()


    /**
     * Make copied ESRI SVGs browser-loadable standalone XML.
     *
     * The previous exporter prepended an XML comment before the source bytes.
     * Many ESRI SVG files begin with an XML declaration, and XML declarations
     * are only legal as the first construct in a document.  A prepended comment
     * therefore made otherwise-good SVGs malformed.  We strip source XML
     * declarations, sanitize our metadata comment, and leave the original
     * <svg> payload untouched.
     */
    internal fun esriSvgCopyAsStandaloneXml(sourceXml: String, metadata: String): String {
        val withoutBom = sourceXml.removePrefix("\uFEFF")
        val withoutDeclarations = xmlDeclarationRegex.replace(withoutBom, "")
        val body = withoutDeclarations.trimStart()
        val comment = sanitizeXmlComment(metadata)
        return "<!-- $comment -->\n" + body.trimEnd() + "\n"
    }

    private fun sanitizeXmlComment(value: String): String = value
        .replace("--", "- -")
        .let { if (it.endsWith("-")) "$it " else it }

    private val xmlDeclarationRegex = Regex("""(?is)<\?xml\s+[^>]*\?>""")

    internal fun unresolvedSvgForTest(name: String, reason: String): String = unresolvedSvg(name, reason)

    internal fun matchKindNamesForTest(): List<String> = MatchKind.values().map { it.name }

    private fun loadPointAsset(slot: OpenCpnEsriSlot): AtlasPointResult {
        val primary = slot.esriFile
        return if (primary != null && primary.isFile) {
            parsePoint(slot, primary)
        } else {
            AtlasPointFailure(slot.openCpnName, slot.esriName ?: "", slot.reason)
        }
    }

    private fun parsePoint(slot: OpenCpnEsriSlot, svg: File): AtlasPointResult {
        return try {
            val document = EsriSvgParser.parse(svg, EsriSvgCategory.POINT.name)
            if (!document.isSubsetSupported) {
                AtlasPointFailure(slot.openCpnName, svg.invariantSeparatorsPath, "unsupported SVG subset in ${svg.name}")
            } else if (document.viewBox == null || !document.viewBox.isValid) {
                AtlasPointFailure(slot.openCpnName, svg.invariantSeparatorsPath, "missing or invalid viewBox in ${svg.name}")
            } else {
                val meshes = EsriSvgMeshGenerator.generate(document).filter { it.isRenderable }
                if (meshes.isEmpty()) {
                    AtlasPointFailure(slot.openCpnName, svg.invariantSeparatorsPath, "no renderable mesh in ${svg.name}")
                } else {
                    AtlasPointAsset(slot.slotIndex, slot.openCpnName, svg.name, svg.invariantSeparatorsPath, slot.matchKind, slot.reason, false, document, meshes)
                }
            }
        } catch (exc: Exception) {
            AtlasPointFailure(slot.openCpnName, svg.invariantSeparatorsPath, exc.message ?: exc::class.simpleName.orEmpty())
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
        symbols: List<OpenCpnEsriSlot>,
        lines: List<OpenCpnEsriSlot>,
        patterns: List<OpenCpnEsriSlot>,
        objects: List<OpenCpnEsriObjectSlot>,
        renderable: List<AtlasPointAsset>,
        failures: List<AtlasPointFailure>,
        atlasLayout: AtlasLayout
    ) {
        val all = symbols + lines + patterns
        file.writeText(buildString {
            appendLine("kind=esri-opencpn-matched-symbology-images")
            appendLine("edition=esri-nautical-chart-symbols-opencpn-name-compatible")
            appendLine("generatedAt=${Instant.now()}")
            appendLine("sourceDir=${sourceDir.invariantSeparatorsPath}")
            appendLine("synthetic=false")
            appendLine("svgSource=true")
            appendLine("runtimePath=generated-kotlin-vector-mesh")
            appendLine("enhancedSvgSet=enhanced-svg")
            appendLine("enhancedSvgPalette=opencpn-inspired-day")
            appendLine("atlasContract=opencpn-symbol-name-compatible")
            appendLine("opencpnCoverageOracle=OpenCpnGeneratedPresLib")
            appendLine("pngSymbolAtlases=3")
            appendLine("symbols=${symbols.size}")
            appendLine("lines=${lines.size}")
            appendLine("patterns=${patterns.size}")
            appendLine("objects=${objects.size}")
            appendLine("objectContract=opencpn-lookup-object-compatible")
            appendLine("atlasSymbols=${symbols.size}")
            appendLine("atlasRendered=${renderable.size}")
            appendLine("atlasSkipped=${failures.size}")
            appendLine("semanticFallbacks=0")
            appendLine("categoryFallbacks=0")
            appendLine("unresolvedMatches=${all.count { it.matchKind == MatchKind.UNRESOLVED }}")
            appendLine("unresolvedObjects=${objects.count { it.assetSlot.matchKind == MatchKind.UNRESOLVED }}")
            appendLine("aliasMatches=${all.count { it.matchKind == MatchKind.ALIAS }}")
            appendLine("customSymbolMapMatches=${all.count { it.matchKind == MatchKind.CUSTOM_SYMBOL_MAP }}")
            appendLine("exactNameMatches=${all.count { it.matchKind == MatchKind.EXACT_NAME }}")
            appendLine("semanticMatches=${all.count { it.matchKind == MatchKind.SEMANTIC_TOKEN }}")
            appendLine("atlasCellPx=$CellPx")
            appendLine("atlasColumns=${atlasLayout.columns}")
            appendLine("atlasRows=${atlasLayout.rows}")
            appendLine("atlasWidthPx=${atlasLayout.widthPx}")
            appendLine("atlasHeightPx=${atlasLayout.heightPx}")
        })
    }

    private fun writeIndex(
        file: File,
        symbols: List<OpenCpnEsriSlot>,
        lines: List<OpenCpnEsriSlot>,
        patterns: List<OpenCpnEsriSlot>,
        objects: List<OpenCpnEsriObjectSlot>,
        renderable: List<AtlasPointAsset>,
        failures: List<AtlasPointFailure>,
        atlasLayout: AtlasLayout
    ) {
        file.writeText(buildString {
            appendLine("<!doctype html>")
            appendLine("<html><head><meta charset=\"utf-8\"><title>ESRI OpenCPN-matched S-52 Symbology Images</title>")
            appendLine("<style>body{font-family:system-ui,sans-serif;margin:24px} img{max-width:100%;height:auto;border:1px solid #ccc} code{background:#f5f5f5;padding:2px 4px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(132px,1fr));gap:10px}.card{border:1px solid #ddd;padding:8px;overflow:hidden}.card img{width:72px;height:72px;object-fit:contain;border:0}.card.broken{background:#fff2f2;border-color:#d55}.card.broken:after{content:'SVG failed to load';display:block;color:#a00;font-size:12px}.warn{color:#9a5200}</style>")
            appendLine("</head><body>")
            appendLine("<h1>ESRI / INT1 symbology image export, OpenCPN-name compatible</h1>")
            appendLine("<p>This artifact uses OpenCPN generated symbology as the coverage oracle. Every exported SVG file keeps the OpenCPN symbol, line, or pattern name, while its drawing comes from the best-matched ESRI SVG source.</p>")
            appendLine("<p>The <code>enhanced-svg/</code> directory is the intermediate portrayal-input set: ESRI geometry recolored with OpenCPN/S-52-inspired day colors, category accents, and deterministic identity marks so otherwise monochrome ESRI symbols stay distinct in the OpenCPN atlas contract.</p>")
            appendLine("<ul>")
            appendLine("<li>OpenCPN point-symbol slots: ${symbols.size}</li>")
            appendLine("<li>OpenCPN line-style slots: ${lines.size}</li>")
            appendLine("<li>OpenCPN pattern slots: ${patterns.size}</li>")
            appendLine("<li>OpenCPN lookup objects: ${objects.size}</li>")
            appendLine("<li>Atlas-rendered point slots: ${renderable.size}</li>")
            appendLine("<li>Rasterization failures: ${failures.size}</li>")
            appendLine("<li>Atlas grid: ${atlasLayout.columns} × ${atlasLayout.rows}, cell ${CellPx}px</li>")
            appendLine("<li>Unresolved symbol/line/pattern slots: ${(symbols + lines + patterns).count { it.matchKind == MatchKind.UNRESOLVED }}</li>")
            appendLine("<li>Unresolved object slots: ${objects.count { it.assetSlot.matchKind == MatchKind.UNRESOLVED }}</li>")
            appendLine("</ul>")
            appendLine("<h2>Symbol atlases</h2>")
            appendLine("<h3>Day</h3><img src=\"symbol-atlas-day.png\" alt=\"ESRI day symbol atlas, OpenCPN-compatible names\">")
            appendLine("<h3>Dusk</h3><img src=\"symbol-atlas-dusk.png\" alt=\"ESRI dusk symbol atlas, OpenCPN-compatible names\">")
            appendLine("<h3>Dark</h3><img src=\"symbol-atlas-dark.png\" alt=\"ESRI dark symbol atlas, OpenCPN-compatible names\">")
            appendLine("<h2>First 500 OpenCPN lookup objects</h2><div class=\"grid\">")
            objects.take(500).forEach { objectSlot ->
                val extra = if (objectSlot.assetSlot.matchKind == MatchKind.UNRESOLVED) "<br><span class=\"warn\">unresolved</span>" else ""
                appendLine("<div class=\"card\"><img src=\"objects/${html(urlPathSegment(safeFileName(objectSlot.objectAcronym)))}.svg\" alt=\"${html(objectSlot.objectAcronym)}\" loading=\"lazy\" onerror=\"this.closest('.card').classList.add('broken')\"><br><code>${html(objectSlot.objectAcronym)}</code><br><small>${html(objectSlot.primitive)} / ${html(objectSlot.openCpnAssetName)}</small>$extra</div>")
            }
            appendLine("</div>")
            if (objects.size > 500) appendLine("<p>Only first 500 object previews shown; all ${objects.size} OpenCPN lookup object SVG files are present under <code>objects/</code>.</p>")
            appendLine("<h2>First 500 OpenCPN-named point symbols</h2><div class=\"grid\">")
            symbols.take(500).forEach { slot ->
                val extra = when (slot.matchKind) {
                    MatchKind.UNRESOLVED -> "<br><span class=\"warn\">unresolved</span>"
                    else -> ""
                }
                val esriPreviewName = html(slot.esriName ?: "unresolved")
                appendLine("<div class=\"card\"><img src=\"symbols/${html(urlPathSegment(safeFileName(slot.openCpnName)))}.svg\" alt=\"${html(slot.openCpnName)}\" loading=\"lazy\" onerror=\"this.closest('.card').classList.add('broken')\"><br><code>${html(slot.openCpnName)}</code><br><small>$esriPreviewName</small>$extra</div>")
            }
            appendLine("</div>")
            if (symbols.size > 500) appendLine("<p>Only first 500 point SVG previews shown; all ${symbols.size} OpenCPN-named SVG files are present under <code>symbols/</code>.</p>")
            appendLine("</body></html>")
        })
    }

    private fun writeAtlasScreenshotPage(file: File, atlasLayout: AtlasLayout) {
        file.writeText(buildString {
            appendLine("<!doctype html>")
            appendLine("<html><head><meta charset=\"utf-8\"><title>ESRI symbol atlas browser screenshot</title>")
            appendLine("<style>body{font-family:system-ui,sans-serif;margin:24px;background:#fff;color:#111} .atlas{margin:0 0 28px} img{display:block;width:${atlasLayout.widthPx}px;height:${atlasLayout.heightPx}px;max-width:none;border:1px solid #ccc} h1{margin-top:0} h2{margin:0 0 8px}</style>")
            appendLine("</head><body>")
            appendLine("<h1>ESRI / INT1 OpenCPN-compatible symbol atlas</h1>")
            appendLine("<p>Browser-rendered screenshot source for the uploaded ESRI atlas PNG artifact. Atlas grid: ${atlasLayout.columns} × ${atlasLayout.rows}, cell ${CellPx}px.</p>")
            appendLine("<section id=\"symbol-atlases\" aria-label=\"ESRI symbol atlases\">")
            appendLine("<div class=\"atlas\"><h2>Day</h2><img src=\"symbol-atlas-day.png\" alt=\"ESRI day symbol atlas, OpenCPN-compatible names\"></div>")
            appendLine("<div class=\"atlas\"><h2>Dusk</h2><img src=\"symbol-atlas-dusk.png\" alt=\"ESRI dusk symbol atlas, OpenCPN-compatible names\"></div>")
            appendLine("<div class=\"atlas\"><h2>Dark</h2><img src=\"symbol-atlas-dark.png\" alt=\"ESRI dark symbol atlas, OpenCPN-compatible names\"></div>")
            appendLine("</section>")
            appendLine("</body></html>")
        })
    }

    private fun writeReport(
        file: File,
        outputDir: File,
        symbols: List<OpenCpnEsriSlot>,
        lines: List<OpenCpnEsriSlot>,
        patterns: List<OpenCpnEsriSlot>,
        objects: List<OpenCpnEsriObjectSlot>,
        renderable: List<AtlasPointAsset>,
        failures: List<AtlasPointFailure>,
        atlasLayout: AtlasLayout
    ) {
        file.writeText(buildString {
            appendLine("{")
            appendLine("  \"outputDir\": \"${json(outputDir.invariantSeparatorsPath)}\",")
            appendLine("  \"contract\": \"opencpn-name-compatible\",")
            appendLine("  \"enhancedSvgSetDir\": \"${json(outputDir.resolve("enhanced-svg").invariantSeparatorsPath)}\",")
            appendLine("  \"enhancedSvgSetFileCount\": ${symbols.size + lines.size + patterns.size + objects.size},")
            appendLine("  \"opencpnSymbolCount\": ${symbols.size},")
            appendLine("  \"opencpnLineCount\": ${lines.size},")
            appendLine("  \"opencpnPatternCount\": ${patterns.size},")
            appendLine("  \"opencpnObjectCount\": ${objects.size},")
            appendLine("  \"atlasRenderedCount\": ${renderable.size},")
            appendLine("  \"atlasSkippedCount\": ${failures.size},")
            appendLine("  \"categoryFallbackCount\": 0,")
            appendLine("  \"unresolvedSlotCount\": ${(symbols + lines + patterns).count { it.matchKind == MatchKind.UNRESOLVED }},")
            appendLine("  \"unresolvedObjectCount\": ${objects.count { it.assetSlot.matchKind == MatchKind.UNRESOLVED }},")
            appendLine("  \"atlas\": {\"cellPx\": $CellPx, \"columns\": ${atlasLayout.columns}, \"rows\": ${atlasLayout.rows}, \"widthPx\": ${atlasLayout.widthPx}, \"heightPx\": ${atlasLayout.heightPx}},")
            appendLine("  \"mappings\": [")
            val all = symbols + lines + patterns
            all.forEachIndexed { index, slot ->
                append("    {\"category\": \"${slot.category.name}\", \"openCpnName\": \"${json(slot.openCpnName)}\", \"outputSvg\": \"${json(slot.outputSubdir)}/${json(safeFileName(slot.openCpnName))}.svg\", \"esriName\": \"${json(slot.esriName ?: "")}\", \"matchKind\": \"${slot.matchKind.name}\", \"reason\": \"${json(slot.reason)}\"}")
                if (index != all.lastIndex) append(',')
                appendLine()
            }
            appendLine("  ],")
            appendLine("  \"objects\": [")
            objects.forEachIndexed { index, objectSlot ->
                append("    {\"objectAcronym\": \"${json(objectSlot.objectAcronym)}\", \"primitive\": \"${json(objectSlot.primitive)}\", \"outputSvg\": \"objects/${json(safeFileName(objectSlot.objectAcronym))}.svg\", \"openCpnAssetName\": \"${json(objectSlot.openCpnAssetName)}\", \"esriName\": \"${json(objectSlot.assetSlot.esriName ?: "")}\", \"matchKind\": \"${objectSlot.assetSlot.matchKind.name}\"}")
                if (index != objects.lastIndex) append(',')
                appendLine()
            }
            appendLine("  ],")
            appendLine("  \"rasterizationFailures\": [")
            failures.forEachIndexed { index, failure ->
                append("    {\"name\": \"${json(failure.name)}\", \"relativePath\": \"${json(failure.relativePath)}\", \"reason\": \"${json(failure.reason)}\"}")
                if (index != failures.lastIndex) append(',')
                appendLine()
            }
            appendLine("  ]")
            appendLine("}")
        })
    }

    private fun writeCsv(file: File, slots: List<OpenCpnEsriSlot>) {
        file.writeText(buildString {
            appendLine("category,opencpn_name,output_svg,esri_source,match_kind,reason")
            slots.forEach { slot ->
                appendLine(listOf(slot.category.name, slot.openCpnName, "${slot.outputSubdir}/${safeFileName(slot.openCpnName)}.svg", slot.esriName ?: "", slot.matchKind.name, slot.reason).joinToString(",") { csv(it) })
            }
        })
    }

    private fun writeObjectCsv(file: File, slots: List<OpenCpnEsriObjectSlot>) {
        file.writeText(buildString {
            appendLine("object_acronym,primitive,output_svg,opencpn_instruction_asset,esri_source,match_kind,source_instruction")
            slots.forEach { slot ->
                appendLine(listOf(slot.objectAcronym, slot.primitive, "objects/${safeFileName(slot.objectAcronym)}.svg", slot.openCpnAssetName, slot.assetSlot.esriName ?: "", slot.assetSlot.matchKind.name, slot.instruction).joinToString(",") { csv(it) })
            }
        })
    }

    private fun buildObjectSlots(
        lookups: List<io.github.s52.preslib.source.SourceLookupRecord>,
        resolver: EsriOpenCpnAtlasResolver
    ): List<OpenCpnEsriObjectSlot> {
        val byObject = linkedMapOf<String, MutableList<io.github.s52.preslib.source.SourceLookupRecord>>()
        lookups.forEach { lookup ->
            byObject.getOrPut(lookup.objectClassKey.acronym) { mutableListOf() }.add(lookup)
        }
        return byObject.entries.mapIndexed { index, (acronym, rows) ->
            val chosen = rows.firstOrNull { it.primitive == io.github.s52.catalog.PrimitiveType.Point && extractPresentationRefs(it.instruction).any { ref -> ref.category == EsriSvgCategory.POINT } }
                ?: rows.firstOrNull { it.primitive == io.github.s52.catalog.PrimitiveType.Line && extractPresentationRefs(it.instruction).any { ref -> ref.category == EsriSvgCategory.LINE } }
                ?: rows.firstOrNull { it.primitive == io.github.s52.catalog.PrimitiveType.Area && extractPresentationRefs(it.instruction).any { ref -> ref.category == EsriSvgCategory.PATTERN || ref.category == EsriSvgCategory.POINT } }
                ?: rows.first()
            val refs = extractPresentationRefs(chosen.instruction)
            val preferred = refs.firstOrNull { ref ->
                when (chosen.primitive) {
                    io.github.s52.catalog.PrimitiveType.Point -> ref.category == EsriSvgCategory.POINT
                    io.github.s52.catalog.PrimitiveType.Line -> ref.category == EsriSvgCategory.LINE || ref.category == EsriSvgCategory.POINT
                    io.github.s52.catalog.PrimitiveType.Area -> ref.category == EsriSvgCategory.PATTERN || ref.category == EsriSvgCategory.POINT || ref.category == EsriSvgCategory.LINE
                    io.github.s52.catalog.PrimitiveType.Collection -> true
                }
            } ?: refs.firstOrNull()
            val category = preferred?.category ?: when (chosen.primitive) {
                io.github.s52.catalog.PrimitiveType.Point -> EsriSvgCategory.POINT
                io.github.s52.catalog.PrimitiveType.Line -> EsriSvgCategory.LINE
                io.github.s52.catalog.PrimitiveType.Area -> EsriSvgCategory.PATTERN
                io.github.s52.catalog.PrimitiveType.Collection -> EsriSvgCategory.POINT
            }
            val openCpnAssetName = preferred?.name ?: acronym
            val assetSlot = resolver.resolve(openCpnAssetName, category)
            OpenCpnEsriObjectSlot(index, acronym, chosen.primitive.name, openCpnAssetName, chosen.instruction, assetSlot)
        }
    }

    private fun extractPresentationRefs(instruction: String): List<OpenCpnPresentationRef> {
        val out = mutableListOf<OpenCpnPresentationRef>()
        fun collect(regex: Regex, category: EsriSvgCategory) {
            regex.findAll(instruction).forEach { match ->
                val name = match.groupValues[1].trim()
                if (name.isNotBlank()) out += OpenCpnPresentationRef(name, category)
            }
        }
        collect(Regex("SY\\(([^)]+)\\)"), EsriSvgCategory.POINT)
        collect(Regex("LC\\(([^)]+)\\)"), EsriSvgCategory.LINE)
        collect(Regex("LS\\(([^,\\)]+)"), EsriSvgCategory.LINE)
        collect(Regex("AP\\(([^)]+)\\)"), EsriSvgCategory.PATTERN)
        return out.distinct()
    }

    private fun safeFileName(value: String): String = value.map { ch ->
        if (ch.isLetterOrDigit() || ch == '_' || ch == '-' || ch == '.' || ch == '$') ch else '_'
    }.joinToString("").ifBlank { "unnamed" }

    private fun html(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun urlPathSegment(value: String): String = buildString {
        value.toByteArray(Charsets.UTF_8).forEach { raw ->
            val b = raw.toInt() and 0xff
            val ch = b.toChar()
            if ((b in 'A'.code..'Z'.code) || (b in 'a'.code..'z'.code) || (b in '0'.code..'9'.code) || ch == '-' || ch == '_' || ch == '.' || ch == '$') {
                append(ch)
            } else {
                append('%')
                append(b.toString(16).uppercase(Locale.US).padStart(2, '0'))
            }
        }
    }

    private fun json(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}

private class EsriOpenCpnAtlasResolver(
    private val layout: EsriSourceLayout,
    rootAliasDir: File
) {
    private val allSvgs: List<File> = layout.svgFiles()
    private val byCategory: Map<EsriSvgCategory, List<File>> = EsriSvgCategory.values().associateWith { category ->
        allSvgs.filter { layout.svgCategory(it) == category }.sortedBy { it.name.lowercase(Locale.US) }
    }
    private val byName: Map<String, File> = allSvgs.flatMap { file ->
        listOf(file.name.lowercase(Locale.US) to file, file.nameWithoutExtension.lowercase(Locale.US) to file)
    }.toMap()
    private val aliases: Map<EsriSvgCategory, Map<String, String>> = mapOf(
        EsriSvgCategory.POINT to readAlias(rootAliasDir.resolve("esri-symbol-aliases.tsv")),
        EsriSvgCategory.LINE to readAlias(rootAliasDir.resolve("esri-line-aliases.tsv")),
        EsriSvgCategory.PATTERN to readAlias(rootAliasDir.resolve("esri-pattern-aliases.tsv"))
    )
    private val directByObject: Map<String, List<String>> = readCustomSymbolMapDirectSymbols(layout.customSymbolMap)

    fun resolve(openCpnName: String, category: EsriSvgCategory): OpenCpnEsriSlot {
        val slotIndex = nextIndex(category)
        val key = canonicalOpenCpnKey(openCpnName)
        aliases[category].orEmpty()[key]?.let { candidate ->
            findSvg(candidate, category)?.let { return slot(slotIndex, openCpnName, category, it, MatchKind.ALIAS, "explicit alias $candidate") }
        }
        findSvg(openCpnName, category)?.let { return slot(slotIndex, openCpnName, category, it, MatchKind.EXACT_NAME, "same source file/base name") }
        val prefix = openCpnObjectPrefix(openCpnName)
        directByObject[prefix]?.firstNotNullOfOrNull { findSvg(it, category) ?: findSvg(it, EsriSvgCategory.POINT) }?.let {
            return slot(slotIndex, openCpnName, category, it, MatchKind.CUSTOM_SYMBOL_MAP, "CustomSymbolMap object rule for $prefix")
        }
        bestSemanticMatch(openCpnName, prefix, category)?.let {
            return slot(slotIndex, openCpnName, category, it, MatchKind.SEMANTIC_TOKEN, "semantic token match for $prefix")
        }
        return slot(slotIndex, openCpnName, category, null, MatchKind.UNRESOLVED, "no exact/direct/semantic ESRI match; generic category fallback disabled")
    }

    private val categoryCounters = mutableMapOf<EsriSvgCategory, Int>()
    private fun nextIndex(category: EsriSvgCategory): Int {
        val value = categoryCounters.getOrDefault(category, 0)
        categoryCounters[category] = value + 1
        return value
    }

    private fun slot(index: Int, openCpnName: String, category: EsriSvgCategory, file: File?, kind: MatchKind, reason: String): OpenCpnEsriSlot =
        OpenCpnEsriSlot(index, openCpnName, category, file, file?.name, kind, reason)

    private fun findSvg(name: String, category: EsriSvgCategory): File? {
        val trimmed = name.trim().removePrefix("SY(").removeSuffix(")")
        if (trimmed.isBlank() || trimmed.startsWith("GENERATED_")) return null
        val candidates = listOf(trimmed, "$trimmed.svg", trimmed.removeSuffix(".svg"))
            .flatMap { listOf(it, it.lowercase(Locale.US), it.uppercase(Locale.US)) }
        candidates.forEach { candidate ->
            byName[candidate.lowercase(Locale.US)]?.let { file ->
                if (category == EsriSvgCategory.UNKNOWN || layout.svgCategory(file) == category) return file
            }
        }
        return null
    }

    private fun bestSemanticMatch(openCpnName: String, prefix: String, category: EsriSvgCategory): File? {
        val tokens = semanticTokens(openCpnName, prefix)
        if (tokens.isEmpty()) return null
        val candidates = byCategory[category].orEmpty().ifEmpty { allSvgs }
        return candidates
            .map { file -> file to score(file.nameWithoutExtension, tokens, prefix) }
            .filter { it.second > 0 }
            .maxWithOrNull(compareBy<Pair<File, Int>> { it.second }.thenBy { it.first.name.length })
            ?.first
    }

    private fun score(base: String, tokens: List<String>, prefix: String): Int {
        val normalized = normalize(base)
        var score = 0
        tokens.forEach { token ->
            if (normalized.contains(normalize(token))) score += 10
        }
        val p = normalize(prefix)
        if (p.isNotBlank() && normalized.contains(p)) score += 25
        return score
    }

    private fun semanticTokens(openCpnName: String, prefix: String): List<String> {
        val p = prefix.uppercase(Locale.US)
        val generic = buildList {
            val n = openCpnName.lowercase(Locale.US)
            if ("wreck" in n) add("wreck")
            if ("boy" in n || "buoy" in n) add("buoy")
            if ("bcn" in n || "beacon" in n) add("beacon")
            if ("light" in n || "lit" in n) add("light")
            if ("cable" in n || "cbl" in n) add("cable")
            if ("sand" in n) add("sand")
        }
        val mapped = when {
            p.startsWith("ACHARE") -> listOf("anchorage", "anchor")
            p.startsWith("BCN") || p.startsWith("RTPBCN") -> listOf("beacon", "stake", "perch")
            p.startsWith("BOYCON") -> listOf("conical", "buoy")
            p.startsWith("BOYSPH") -> listOf("spherical", "buoy")
            p.startsWith("BOY") -> listOf("buoy")
            p.startsWith("BRIDGE") -> listOf("bridge")
            p.startsWith("CBL") -> listOf("cable")
            p.startsWith("COALNE") -> listOf("coast", "coastline")
            p.startsWith("DEPARE") -> listOf("depth", "depare")
            p.startsWith("DRGARE") -> listOf("dragged", "drgare")
            p.startsWith("LIGHTS") || p.startsWith("LIT") -> listOf("light")
            p.startsWith("LNDARE") -> listOf("land", "lndare")
            p.startsWith("LNDELV") -> listOf("elevation", "lndelv")
            p.startsWith("OBSTRN") -> listOf("obstruction")
            p.startsWith("RESARE") -> listOf("restricted", "restriction", "resare")
            p.startsWith("SBDARE") -> listOf("sand", "seabed", "sbdare")
            p.startsWith("SOUNDG") -> listOf("sounding")
            p.startsWith("TOPMAR") || p.startsWith("TOPSHP") -> listOf("topmark", "top")
            p.startsWith("WRECKS") -> listOf("wreck")
            else -> emptyList()
        }
        return (mapped + generic).distinct()
    }

    private fun readAlias(file: File): Map<String, String> {
        if (!file.isFile) return emptyMap()
        return file.readLines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val p = line.split('\t')
                if (p.size < 2 || p[0].startsWith("opencpn_")) null else canonicalOpenCpnKey(p[0]) to p[1].trim()
            }
            .toMap()
    }

    private fun readCustomSymbolMapDirectSymbols(file: File): Map<String, List<String>> {
        if (!file.isFile) return emptyMap()
        return try {
            val result = linkedMapOf<String, MutableList<String>>()
            val map = EsriCustomSymbolMapParser.parse(file)
            map.features.forEach { feature ->
                val symbols = feature.conditions.mapNotNull { it.symbolName?.trim()?.takeIf(String::isNotBlank) }.distinct()
                if (symbols.isEmpty()) return@forEach
                feature.objects
                    .map { it.trim().uppercase(Locale.US) }
                    .filter { it.isNotBlank() }
                    .forEach { obj -> result.getOrPut(obj) { mutableListOf() }.addAll(symbols) }
            }
            result.mapValues { (_, values) -> values.distinct() }
        } catch (exc: Exception) {
            emptyMap()
        }
    }
}

internal fun canonicalOpenCpnKey(value: String): String = value.trim().removeSuffix(".svg").uppercase(Locale.US)
internal fun openCpnObjectPrefix(value: String): String = value.takeWhile { it.isLetter() }.uppercase(Locale.US)
internal fun normalize(value: String): String = value.lowercase(Locale.US).filter { it.isLetterOrDigit() }

private enum class MatchKind { ALIAS, CUSTOM_SYMBOL_MAP, EXACT_NAME, SEMANTIC_TOKEN, UNRESOLVED }

private data class OpenCpnPresentationRef(val name: String, val category: EsriSvgCategory)

private data class EnhancedColors(val primary: String, val accent: String, val outline: String, val category: String)

private data class OpenCpnEsriObjectSlot(
    val objectIndex: Int,
    val objectAcronym: String,
    val primitive: String,
    val openCpnAssetName: String,
    val instruction: String,
    val assetSlot: OpenCpnEsriSlot
)

private data class OpenCpnEsriSlot(
    val slotIndex: Int,
    val openCpnName: String,
    val category: EsriSvgCategory,
    val esriFile: File?,
    val esriName: String?,
    val matchKind: MatchKind,
    val reason: String
) {
    val outputSubdir: String = when (category) {
        EsriSvgCategory.POINT -> "symbols"
        EsriSvgCategory.LINE -> "lines"
        EsriSvgCategory.PATTERN -> "patterns"
        EsriSvgCategory.UNKNOWN -> "unknown"
    }
}

private sealed interface AtlasPointResult
private data class AtlasPointAsset(
    val slotIndex: Int,
    val name: String,
    val esriName: String,
    val relativePath: String,
    val matchKind: MatchKind,
    val reason: String,
    val fallbackRender: Boolean,
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
            put(8.toByte())
            put(6.toByte())
            put(0.toByte())
            put(0.toByte())
            put(0.toByte())
        }.array())

        val raw = ByteArrayOutputStream()
        for (y in 0 until height) {
            raw.write(0)
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
