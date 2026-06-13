package io.github.s52.demo

import io.github.s52.api.S52GalleryBuilder
import io.github.s52.api.S52GalleryRequest
import io.github.s52.api.S52GallerySection
import io.github.s52.api.S52OpenCpnDiagnostics
import io.github.s52.api.S52VisualRegressionFixtures
import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.instruction.InstructionKind
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack
import io.github.s52.render.webgl.WebGlS52Renderer
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

fun main() {
    val canvas = document.getElementById("chart") as HTMLCanvasElement
    val status = document.getElementById("status") as HTMLElement

    fun renderCurrentRoute() {
        val route = window.location.hash.removePrefix("#").lowercase()
        val useOpenCpn = route.startsWith("opencpn-") || route == "opencpn"
        val presLib = if (useOpenCpn) PresLibPack.openCpn() else PresLibPack.s52LibCompat()
        val settings = MarinerSettings()
        val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        val renderer = WebGlS52Renderer(canvas, presLib) { renderCurrentRoute() }
        val sectionHash = if (useOpenCpn) "#" + route.removePrefix("opencpn-").ifBlank { "symbols" } else window.location.hash
        val section = S52GallerySection.fromHash(sectionHash)

        when {
            route == "opencpn-diagnostics" -> {
                val report = S52OpenCpnDiagnostics.report(presLib, DefaultCspRegistry.openCpn())
                val commands = textPanel(report.toPlainText(maxItems = 10).lines().take(14))
                val stats = renderer.render(commands, settings)
                status.textContent = report.toPlainText(maxItems = 10) + "\n" + stats + "\nRoutes: #opencpn-symbols #opencpn-lines #opencpn-patterns #opencpn-lookups #opencpn-diagnostics"
            }
            route == "opencpn-lookups" -> {
                val report = S52OpenCpnDiagnostics.report(presLib, DefaultCspRegistry.openCpn())
                val lines = listOf(
                    "OpenCPN lookup summary",
                    "lookups=${report.lookupCount}",
                    "presentationTables=${report.presentationTableCounts.entries.sortedBy { it.key }.joinToString { it.key + "=" + it.value }}",
                    "primitives=${report.primitiveCounts.entries.sortedBy { it.key }.joinToString { it.key + "=" + it.value }}",
                    "displayCategories=${report.displayCategoryCounts.entries.sortedBy { it.key }.joinToString { it.key + "=" + it.value }}",
                    "referencedSymbols=${report.referencedSymbols.size} referencedLineStyles=${report.referencedLineStyles.size} referencedPatterns=${report.referencedPatterns.size}",
                    "referencedCsps=${report.referencedCsps.size} unresolvedCsps=${report.unresolvedCsps.size}"
                )
                val stats = renderer.render(textPanel(lines), settings)
                status.textContent = lines.joinToString("\n") + "\n" + stats + "\nRoutes: #opencpn-symbols #opencpn-lines #opencpn-patterns #opencpn-regression #opencpn-diagnostics"
            }
            route == "opencpn-regression" -> {
                val commands = S52VisualRegressionFixtures.phase33Commands(includeLabels = true)
                val stats = renderer.render(commands, settings)
                status.textContent = "OpenCPN phase 33 visual regression fixture: ${commands.size} commands, $stats. Expected checks: concave area hole, HPGL pattern tiles without rounded preview boxes, TOPMAR/WRECK/OBSTRN/LIGHTS/QUESMRK symbols."
            }
            section == S52GallerySection.Chart -> {
                val engine = S52PortrayalEngine(presLib.lookupTable, if (useOpenCpn) DefaultCspRegistry.openCpn() else DefaultCspRegistry.phase6Complete())
                val commands = engine.portray(phase20SyntheticFeatures(), settings, context)
                val stats = renderer.render(commands, settings)
                status.textContent = "${if (useOpenCpn) "OpenCPN" else "Compat"} chart demo: ${commands.size} commands, $stats. Routes: #chart #symbols #opencpn-symbols #opencpn-colors #opencpn-lookups #opencpn-regression #opencpn-diagnostics"
            }
            else -> {
                val gallery = S52GalleryBuilder.build(presLib, S52GalleryRequest(section = section))
                val stats = renderer.render(gallery.commands, settings)
                status.textContent = "${if (useOpenCpn) "OpenCPN " else ""}${gallery.title}: ${gallery.assetCommandCount} asset commands, ${gallery.totalCommandCount} total commands, $stats. Routes: #chart #symbols #opencpn-symbols #opencpn-lines #opencpn-patterns #opencpn-colors #opencpn-lookups #opencpn-regression #opencpn-diagnostics"
            }
        }
    }

    renderCurrentRoute()
    window.onhashchange = { _ -> renderCurrentRoute() }
}


private fun textPanel(lines: List<String>): List<S52DrawCommand> = lines.mapIndexed { index, line ->
    S52DrawCommand.Text(
        featureId = 90_000L + index,
        geometry = EncGeometry.Point(Coordinate(-74.08, 40.18 - index * 0.012)),
        textExpression = line.take(96),
        rawArgs = listOf(line),
        textKind = InstructionKind.TX,
        colorToken = "CHBLK",
        priority = 100,
        viewingGroup = 99999,
        category = DisplayCategory.Other,
        overRadar = true
    )
}

private fun phase20SyntheticFeatures(): List<EncFeature> = listOf(
    EncFeature(1, S57ObjectClass.DEPARE, PrimitiveType.Area, S57Attributes.of(S57Attribute.DRVAL1 to S57Value.Decimal(0.0), S57Attribute.DRVAL2 to S57Value.Decimal(4.0)), EncGeometry.Polygon(listOf(Coordinate(-74.10,39.90),Coordinate(-73.78,39.90),Coordinate(-73.78,40.20),Coordinate(-74.10,40.20),Coordinate(-74.10,39.90)))) ,
    EncFeature(2, S57ObjectClass.LNDARE, PrimitiveType.Area, S57Attributes.Empty, EncGeometry.Polygon(listOf(Coordinate(-74.10,40.12),Coordinate(-73.78,40.12),Coordinate(-73.78,40.20),Coordinate(-74.10,40.20),Coordinate(-74.10,40.12)))) ,
    EncFeature(3, S57ObjectClass.COALNE, PrimitiveType.Line, S57Attributes.Empty, EncGeometry.LineString(listOf(Coordinate(-74.10,40.12),Coordinate(-74.00,40.16),Coordinate(-73.90,40.14),Coordinate(-73.78,40.12)))) ,
    EncFeature(4, S57ObjectClass.DEPCNT, PrimitiveType.Line, S57Attributes.of(S57Attribute.VALDCO to S57Value.Decimal(10.0)), EncGeometry.LineString(listOf(Coordinate(-74.08,40.02),Coordinate(-73.96,40.06),Coordinate(-73.82,40.04)))) ,
    EncFeature(5, S57ObjectClass.BOYLAT, PrimitiveType.Point, S57Attributes.Empty, EncGeometry.Point(Coordinate(-73.95,40.05))) ,
    EncFeature(6, S57ObjectClass.LIGHTS, PrimitiveType.Point, S57Attributes.of(S57Attribute.SECTR1 to S57Value.Decimal(15.0), S57Attribute.SECTR2 to S57Value.Decimal(90.0)), EncGeometry.Point(Coordinate(-73.87,40.10))) ,
    EncFeature(7, S57ObjectClass.SOUNDG, PrimitiveType.Point, S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(6.4)), EncGeometry.Point(Coordinate(-74.02,40.00))) ,
    EncFeature(8, S57ObjectClass.WRECKS, PrimitiveType.Point, S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.0)), EncGeometry.Point(Coordinate(-73.86,39.98)))
)
