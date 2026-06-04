package io.github.s52.demo

import io.github.s52.api.S52GalleryBuilder
import io.github.s52.api.S52GalleryRequest
import io.github.s52.api.S52GallerySection
import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
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
        val presLib = PresLibPack.s52LibCompat()
        val settings = MarinerSettings()
        val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        val renderer = WebGlS52Renderer(canvas, presLib)
        val section = S52GallerySection.fromHash(window.location.hash)

        if (section == S52GallerySection.Chart) {
            val engine = S52PortrayalEngine(presLib.lookupTable, DefaultCspRegistry.phase6Complete())
            val commands = engine.portray(phase20SyntheticFeatures(), settings, context)
            val stats = renderer.render(commands, settings)
            status.textContent = "Chart demo: ${commands.size} commands, $stats. Routes: #chart #symbols #lines #patterns #colors #all"
        } else {
            val gallery = S52GalleryBuilder.build(presLib, S52GalleryRequest(section = section))
            val stats = renderer.render(gallery.commands, settings)
            status.textContent = "${gallery.title}: ${gallery.assetCommandCount} asset commands, ${gallery.totalCommandCount} total commands, $stats. Routes: #chart #symbols #lines #patterns #colors #all"
        }
    }

    renderCurrentRoute()
    window.onhashchange = { _ -> renderCurrentRoute() }
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
