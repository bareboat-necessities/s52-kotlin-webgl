package io.github.s52.demo

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
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement

fun main() {
    val canvas = document.getElementById("chart") as HTMLCanvasElement
    val status = document.getElementById("status") as HTMLElement

    val presLib = PresLibPack.phase2Synthetic()
    val settings = MarinerSettings()
    val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
    val engine = S52PortrayalEngine(presLib.lookupTable, DefaultCspRegistry.phase6Complete())
    val commands = engine.portray(phase8SyntheticFeatures(), settings, context)
    val stats = WebGlS52Renderer(canvas, presLib).render(commands, settings)

    status.textContent = "Phase 8 WebGL2 renderer: ${commands.size} commands, $stats"
}

private fun phase8SyntheticFeatures(): List<EncFeature> = listOf(
    EncFeature(
        id = 1,
        objectClass = S57ObjectClass.DEPARE,
        primitive = PrimitiveType.Area,
        attributes = S57Attributes.of(
            S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
            S57Attribute.DRVAL2 to S57Value.Decimal(4.0)
        ),
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.10, 39.90),
                Coordinate(-73.78, 39.90),
                Coordinate(-73.78, 40.20),
                Coordinate(-74.10, 40.20),
                Coordinate(-74.10, 39.90)
            )
        )
    ),
    EncFeature(
        id = 2,
        objectClass = S57ObjectClass.LNDARE,
        primitive = PrimitiveType.Area,
        attributes = S57Attributes.Empty,
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.10, 40.12),
                Coordinate(-73.78, 40.12),
                Coordinate(-73.78, 40.20),
                Coordinate(-74.10, 40.20),
                Coordinate(-74.10, 40.12)
            )
        )
    ),
    EncFeature(
        id = 3,
        objectClass = S57ObjectClass.COALNE,
        primitive = PrimitiveType.Line,
        attributes = S57Attributes.Empty,
        geometry = EncGeometry.LineString(
            coordinates = listOf(
                Coordinate(-74.10, 40.12),
                Coordinate(-74.00, 40.16),
                Coordinate(-73.90, 40.14),
                Coordinate(-73.78, 40.12)
            )
        )
    ),
    EncFeature(
        id = 4,
        objectClass = S57ObjectClass.DEPCNT,
        primitive = PrimitiveType.Line,
        attributes = S57Attributes.of(S57Attribute.VALDCO to S57Value.Decimal(10.0)),
        geometry = EncGeometry.LineString(
            coordinates = listOf(
                Coordinate(-74.08, 40.02),
                Coordinate(-73.96, 40.06),
                Coordinate(-73.82, 40.04)
            )
        )
    ),
    EncFeature(
        id = 5,
        objectClass = S57ObjectClass.BOYLAT,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.Empty,
        geometry = EncGeometry.Point(Coordinate(-73.95, 40.05))
    ),
    EncFeature(
        id = 6,
        objectClass = S57ObjectClass.LIGHTS,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(
            S57Attribute.SECTR1 to S57Value.Decimal(15.0),
            S57Attribute.SECTR2 to S57Value.Decimal(90.0)
        ),
        geometry = EncGeometry.Point(Coordinate(-73.87, 40.10))
    ),
    EncFeature(
        id = 7,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(6.4)),
        geometry = EncGeometry.Point(Coordinate(-74.02, 40.00))
    ),
    EncFeature(
        id = 8,
        objectClass = S57ObjectClass.WRECKS,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.0)),
        geometry = EncGeometry.Point(Coordinate(-73.86, 39.98))
    )
)
