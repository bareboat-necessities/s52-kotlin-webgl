package io.github.s52.tests.golden

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings

/** Synthetic Chart-1-like command-level golden fixture. */
object GoldenCases {
    fun all(): List<GoldenPortrayalCase> = listOf(
        depthSafety(),
        dangerSymbols(),
        otherOverlays(),
        visibilitySettings()
    )

    fun depthSafety(): GoldenPortrayalCase = GoldenPortrayalCase(
        id = "depth-safety",
        description = "DEPARE, DEPCNT, and SOUNDG react to safety contour/depth settings.",
        settings = MarinerSettings(
            safetyDepthMeters = 5.0,
            safetyContourMeters = 10.0,
            shallowContourMeters = 2.0,
            deepContourMeters = 30.0,
            showSoundings = true
        ),
        features = listOf(
            area(
                id = 1001,
                objectClass = S57ObjectClass.DEPARE,
                attributes = S57Attributes.of(
                    S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
                    S57Attribute.DRVAL2 to S57Value.Decimal(4.0)
                )
            ),
            line(
                id = 1002,
                objectClass = S57ObjectClass.DEPCNT,
                attributes = S57Attributes.of(S57Attribute.VALDCO to S57Value.Decimal(10.0))
            ),
            point(
                id = 1003,
                objectClass = S57ObjectClass.SOUNDG,
                attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.2))
            )
        )
    )

    fun dangerSymbols(): GoldenPortrayalCase = GoldenPortrayalCase(
        id = "danger-symbols",
        description = "Dangerous wreck and obstruction point CSPs produce danger symbols.",
        settings = MarinerSettings(safetyDepthMeters = 5.0),
        features = listOf(
            point(
                id = 2001,
                objectClass = S57ObjectClass.WRECKS,
                lon = -73.990,
                lat = 40.000,
                attributes = S57Attributes.of(S57Attribute.CATWRK to S57Value.Integer(2))
            ),
            point(
                id = 2002,
                objectClass = S57ObjectClass.OBSTRN,
                lon = -73.980,
                lat = 40.000,
                attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(2.0))
            )
        )
    )

    fun otherOverlays(): GoldenPortrayalCase = GoldenPortrayalCase(
        id = "other-overlays",
        description = "Other-category overlay CSPs produce stable command transcripts.",
        settings = MarinerSettings(displayCategory = DisplayCategory.Other, showText = true),
        features = listOf(
            area(
                id = 3001,
                objectClass = S57ObjectClass.M_COVR,
                attributes = S57Attributes.of(S57Attribute.CATCOV to S57Value.Integer(2))
            ),
            area(
                id = 3002,
                objectClass = S57ObjectClass.RESARE,
                attributes = S57Attributes.of(
                    S57Attribute.RESTRN to S57Value.ListValue(listOf(S57Value.Integer(1), S57Value.Integer(7)))
                )
            ),
            area(
                id = 3003,
                objectClass = S57ObjectClass.M_QUAL,
                attributes = S57Attributes.of(S57Attribute.CATZOC to S57Value.Integer(5))
            )
        )
    )

    fun visibilitySettings(): GoldenPortrayalCase = GoldenPortrayalCase(
        id = "visibility-settings",
        description = "Text and sounding visibility settings suppress only the expected commands.",
        settings = MarinerSettings(
            showText = false,
            showSoundings = false,
            showLightDescriptions = false
        ),
        features = listOf(
            point(
                id = 4001,
                objectClass = S57ObjectClass.LIGHTS,
                lon = -73.990,
                lat = 40.010,
                attributes = S57Attributes.of(
                    S57Attribute.OBJNAM to S57Value.Text("Main"),
                    S57Attribute.SECTR1 to S57Value.Decimal(20.0),
                    S57Attribute.SECTR2 to S57Value.Decimal(110.0)
                )
            ),
            point(
                id = 4002,
                objectClass = S57ObjectClass.SOUNDG,
                lon = -73.980,
                lat = 40.010,
                attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(3.2))
            )
        )
    )

    private fun point(
        id: Long,
        objectClass: S57ObjectClass,
        lon: Double = -73.995,
        lat: Double = 40.005,
        attributes: S57Attributes = S57Attributes.Empty
    ): EncFeature = EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = PrimitiveType.Point,
        attributes = attributes,
        geometry = EncGeometry.Point(Coordinate(lon, lat))
    )

    private fun line(
        id: Long,
        objectClass: S57ObjectClass,
        attributes: S57Attributes = S57Attributes.Empty
    ): EncFeature = EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = PrimitiveType.Line,
        attributes = attributes,
        geometry = EncGeometry.LineString(
            listOf(Coordinate(-74.0, 40.0), Coordinate(-73.99, 40.01))
        )
    )

    private fun area(
        id: Long,
        objectClass: S57ObjectClass,
        attributes: S57Attributes = S57Attributes.Empty
    ): EncFeature = EncFeature(
        id = id,
        objectClass = objectClass,
        primitive = PrimitiveType.Area,
        attributes = attributes,
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.0, 40.0),
                Coordinate(-73.99, 40.0),
                Coordinate(-73.99, 40.01),
                Coordinate(-74.0, 40.0)
            )
        )
    )
}
