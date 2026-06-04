package samples.minimal

import io.github.s52.api.S52
import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value

fun main() {
    val feature = EncFeature(
        id = 1,
        objectClass = S57ObjectClass.DEPARE,
        primitive = PrimitiveType.Area,
        attributes = S57Attributes.of(
            S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
            S57Attribute.DRVAL2 to S57Value.Decimal(5.0)
        ),
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.00, 40.00),
                Coordinate(-73.99, 40.00),
                Coordinate(-73.99, 40.01),
                Coordinate(-74.00, 40.01),
                Coordinate(-74.00, 40.00)
            )
        )
    )

    val runtime = S52.defaultRuntime()
    val settings = S52.defaultSettings(safetyContourMeters = 6.0)
    val result = runtime.portrayValidated(listOf(feature), settings, S52.defaultContext(settings))

    check(result.isValid) { result.validation.diagnostics.joinToString("\n") }
    println("Generated ${result.commands.size} S-52 draw command(s).")
}
