package samples.transcript

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
    val sounding = EncFeature(
        id = 7,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(4.2)),
        geometry = EncGeometry.Point(Coordinate(-73.95, 40.05))
    )

    val runtime = S52.defaultRuntime()
    val settings = S52.defaultSettings(safetyDepthMeters = 6.0, showSoundings = true)
    val transcript = runtime.transcript(listOf(sounding), settings, S52.defaultContext(settings))

    println(transcript)
}
