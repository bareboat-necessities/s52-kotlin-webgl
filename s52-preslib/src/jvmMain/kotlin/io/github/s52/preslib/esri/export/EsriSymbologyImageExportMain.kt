package io.github.s52.preslib.esri.export

import io.github.s52.preslib.esri.importer.EsriSourceLayout
import io.github.s52.preslib.esri.importer.EsriSvgCategory
import io.github.s52.preslib.esri.svg.EsriGeneratedPaint
import io.github.s52.preslib.esri.svg.EsriGeneratedSvgMesh
import io.github.s52.preslib.esri.svg.EsriSvgDocument
import io.github.s52.preslib.esri.svg.EsriSvgMeshGenerator
import io.github.s52.preslib.esri.svg.EsriSvgParser
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
 * exact file-name match, semantic token match, or finally a category fallback.
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

        val renderFallback = resolver.firstRenderablePointFallback()
        val pointResults = symbolSlots.map { slot -> loadPointAsset(slot, renderFallback) }
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
        val semanticFallbacks = (symbolSlots + lineSlots + patternSlots).count { it.matchKind == MatchKind.CATEGORY_FALLBACK }
        if (semanticFallbacks > 0) {
            System.err.println("WARNING: ESRI matched atlas used $semanticFallbacks category fallback mapping(s). Add explicit aliases in s52/esri/*.tsv to close them.")
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
            val fileName = "${slot.openCpnName}.svg"
            val target = outputDir.resolve(fileName)
            val source = slot.esriFile
            if (source != null && source.isFile) {
                val comment = "<!-- OpenCPN slot: ${slot.openCpnName}; ESRI source: ${slot.esriName}; match: ${slot.matchKind.name}; reason: ${slot.reason} -->\n"
                target.writeText(comment + source.readText(), Charsets.UTF_8)
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
                val comment = "<!-- OpenCPN object: ${objectSlot.objectAcronym}; primitive: ${objectSlot.primitive}; OpenCPN instruction asset: ${objectSlot.openCpnAssetName}; ESRI source: ${objectSlot.assetSlot.esriName}; match: ${objectSlot.assetSlot.matchKind.name}; reason: ${objectSlot.assetSlot.reason} -->\n"
                target.writeText(comment + source.readText(), Charsets.UTF_8)
            } else {
                target.writeText(unresolvedSvg(objectSlot.objectAcronym, objectSlot.assetSlot.reason), Charsets.UTF_8)
            }
        }
    }

    private fun unresolvedSvg(name: String, reason: String): String = """
        <svg xmlns="http://www.w3.org/2000/svg" width="72" height="72" viewBox="0 0 72 72">
          <rect x="1" y="1" width="70" height="70" fill="none" stroke="#cc0066" stroke-width="2"/>
          <path d="M16 16L56 56M56 16L16 56" stroke="#cc0066" stroke-width="4" stroke-linecap="round"/>
          <text x="36" y="68" text-anchor="middle" font-size="6" fill="#cc0066">${html(name.take(16))}</text>
          <desc>${html(reason)}</desc>
        </svg>
    """.trimIndent()

    private fun loadPointAsset(slot: OpenCpnEsriSlot, fallback: File?): AtlasPointResult {
        val primary = slot.esriFile
        val first = if (primary != null && primary.isFile) parsePoint(slot, primary, false) else null
        if (first is AtlasPointAsset) return first
        if (fallback != null && fallback.isFile && fallback != primary) {
            val fallbackSlot = slot.copy(
                esriFile = fallback,
                esriName = fallback.name,
                matchKind = MatchKind.RENDER_FALLBACK,
                reason = "${slot.reason}; primary could not be rasterized: ${(first as? AtlasPointFailure)?.reason ?: "missing primary"}"
            )
            val second = parsePoint(fallbackSlot, fallback, true)
            if (second is AtlasPointAsset) return second
        }
        return first ?: AtlasPointFailure(slot.openCpnName, slot.esriName ?: "", slot.reason)
    }

    private fun parsePoint(slot: OpenCpnEsriSlot, svg: File, fallbackRender: Boolean): AtlasPointResult {
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
                    AtlasPointAsset(slot.slotIndex, slot.openCpnName, svg.name, svg.invariantSeparatorsPath, slot.matchKind, slot.reason, fallbackRender, document, meshes)
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
            appendLine("semanticFallbacks=${all.count { it.matchKind == MatchKind.CATEGORY_FALLBACK }}")
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
            appendLine("<style>body{font-family:system-ui,sans-serif;margin:24px} img{max-width:100%;height:auto;border:1px solid #ccc} code{background:#f5f5f5;padding:2px 4px}.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(132px,1fr));gap:10px}.card{border:1px solid #ddd;padding:8px;overflow:hidden}.card img{width:72px;height:72px;object-fit:contain;border:0}.warn{color:#9a5200}</style>")
            appendLine("</head><body>")
            appendLine("<h1>ESRI / INT1 symbology image export, OpenCPN-name compatible</h1>")
            appendLine("<p>This artifact uses OpenCPN generated symbology as the coverage oracle. Every exported SVG file keeps the OpenCPN symbol, line, or pattern name, while its drawing comes from the best-matched ESRI SVG source.</p>")
            appendLine("<ul>")
            appendLine("<li>OpenCPN point-symbol slots: ${symbols.size}</li>")
            appendLine("<li>OpenCPN line-style slots: ${lines.size}</li>")
            appendLine("<li>OpenCPN pattern slots: ${patterns.size}</li>")
            appendLine("<li>OpenCPN lookup objects: ${objects.size}</li>")
            appendLine("<li>Atlas-rendered point slots: ${renderable.size}</li>")
            appendLine("<li>Rasterization failures: ${failures.size}</li>")
            appendLine("<li>Atlas grid: ${atlasLayout.columns} × ${atlasLayout.rows}, cell ${CellPx}px</li>")
            appendLine("</ul>")
            appendLine("<h2>Symbol atlases</h2>")
            appendLine("<h3>Day</h3><img src=\"symbol-atlas-day.png\" alt=\"ESRI day symbol atlas, OpenCPN-compatible names\">")
            appendLine("<h3>Dusk</h3><img src=\"symbol-atlas-dusk.png\" alt=\"ESRI dusk symbol atlas, OpenCPN-compatible names\">")
            appendLine("<h3>Dark</h3><img src=\"symbol-atlas-dark.png\" alt=\"ESRI dark symbol atlas, OpenCPN-compatible names\">")
            appendLine("<h2>First 500 OpenCPN lookup objects</h2><div class=\"grid\">")
            objects.take(500).forEach { objectSlot ->
                appendLine("<div class=\"card\"><img src=\"objects/${html(safeFileName(objectSlot.objectAcronym))}.svg\" alt=\"${html(objectSlot.objectAcronym)}\"><br><code>${html(objectSlot.objectAcronym)}</code><br><small>${html(objectSlot.primitive)} / ${html(objectSlot.openCpnAssetName)}</small></div>")
            }
            appendLine("</div>")
            if (objects.size > 500) appendLine("<p>Only first 500 object previews shown; all ${objects.size} OpenCPN lookup object SVG files are present under <code>objects/</code>.</p>")
            appendLine("<h2>First 500 OpenCPN-named point symbols</h2><div class=\"grid\">")
            symbols.take(500).forEach { slot ->
                val extra = if (slot.matchKind == MatchKind.CATEGORY_FALLBACK) "<br><span class=\"warn\">fallback</span>" else ""
                val esriPreviewName = html(slot.esriName ?: "unresolved")
                appendLine("<div class=\"card\"><img src=\"symbols/${html(slot.openCpnName)}.svg\" alt=\"${html(slot.openCpnName)}\"><br><code>${html(slot.openCpnName)}</code><br><small>$esriPreviewName</small>$extra</div>")
            }
            appendLine("</div>")
            if (symbols.size > 500) appendLine("<p>Only first 500 point SVG previews shown; all ${symbols.size} OpenCPN-named SVG files are present under <code>symbols/</code>.</p>")
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
            appendLine("  \"opencpnSymbolCount\": ${symbols.size},")
            appendLine("  \"opencpnLineCount\": ${lines.size},")
            appendLine("  \"opencpnPatternCount\": ${patterns.size},")
            appendLine("  \"opencpnObjectCount\": ${objects.size},")
            appendLine("  \"atlasRenderedCount\": ${renderable.size},")
            appendLine("  \"atlasSkippedCount\": ${failures.size},")
            appendLine("  \"atlas\": {\"cellPx\": $CellPx, \"columns\": ${atlasLayout.columns}, \"rows\": ${atlasLayout.rows}, \"widthPx\": ${atlasLayout.widthPx}, \"heightPx\": ${atlasLayout.heightPx}},")
            appendLine("  \"mappings\": [")
            val all = symbols + lines + patterns
            all.forEachIndexed { index, slot ->
                append("    {\"category\": \"${slot.category.name}\", \"openCpnName\": \"${json(slot.openCpnName)}\", \"outputSvg\": \"${json(slot.outputSubdir)}/${json(slot.openCpnName)}.svg\", \"esriName\": \"${json(slot.esriName ?: "")}\", \"matchKind\": \"${slot.matchKind.name}\", \"reason\": \"${json(slot.reason)}\"}")
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
                appendLine(listOf(slot.category.name, slot.openCpnName, "${slot.outputSubdir}/${slot.openCpnName}.svg", slot.esriName ?: "", slot.matchKind.name, slot.reason).joinToString(",") { csv(it) })
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
        val fallback = categoryFallback(category)
        return slot(slotIndex, openCpnName, category, fallback, MatchKind.CATEGORY_FALLBACK, "no exact/direct/semantic match; category fallback")
    }

    private val categoryCounters = mutableMapOf<EsriSvgCategory, Int>()
    private fun nextIndex(category: EsriSvgCategory): Int {
        val value = categoryCounters.getOrDefault(category, 0)
        categoryCounters[category] = value + 1
        return value
    }

    fun firstRenderablePointFallback(): File? {
        val preferred = listOf("Q80_Beacon.svg", "Q20b_Conical_buoy.svg", "P1_Light.svg")
            .firstNotNullOfOrNull { findSvg(it, EsriSvgCategory.POINT) }
        if (preferred != null) return preferred
        return byCategory[EsriSvgCategory.POINT].orEmpty().firstOrNull()
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

    private fun categoryFallback(category: EsriSvgCategory): File? {
        val preferred = when (category) {
            EsriSvgCategory.POINT -> listOf("Q80_Beacon.svg", "Q20b_Conical_buoy.svg", "P1_Light.svg")
            EsriSvgCategory.LINE -> listOf("M1_NavigationLine.svg", "L30_Cable.svg", "N2_1_RestrictedArea.svg")
            EsriSvgCategory.PATTERN -> listOf("J1_Sand.svg", "N2_1_RestrictedArea.svg")
            EsriSvgCategory.UNKNOWN -> emptyList()
        }.firstNotNullOfOrNull { findSvg(it, category) }
        return preferred ?: byCategory[category].orEmpty().firstOrNull()
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
        val result = linkedMapOf<String, MutableList<String>>()
        val attr = Regex("""([A-Za-z0-9_:-]+)\s*=\s*['\"]([^'\"]+)['\"]""")
        Regex("""<[^>]+>""").findAll(file.readText()).forEach { match ->
            val attrs = attr.findAll(match.value).associate { it.groupValues[1] to it.groupValues[2] }
            val objects = attrs["objects"] ?: attrs["object"] ?: attrs["acronym"] ?: attrs["acronyms"] ?: return@forEach
            val symbol = attrs["symbolName"] ?: return@forEach
            objects.split(',').map { it.trim().uppercase(Locale.US) }.filter { it.isNotBlank() }.forEach { obj ->
                result.getOrPut(obj) { mutableListOf() }.add(symbol)
            }
        }
        return result
    }
}

internal fun canonicalOpenCpnKey(value: String): String = value.trim().removeSuffix(".svg").uppercase(Locale.US)
internal fun openCpnObjectPrefix(value: String): String = value.takeWhile { it.isLetter() }.uppercase(Locale.US)
internal fun normalize(value: String): String = value.lowercase(Locale.US).filter { it.isLetterOrDigit() }

private enum class MatchKind { ALIAS, CUSTOM_SYMBOL_MAP, EXACT_NAME, SEMANTIC_TOKEN, CATEGORY_FALLBACK, RENDER_FALLBACK }

private data class OpenCpnPresentationRef(val name: String, val category: EsriSvgCategory)

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
