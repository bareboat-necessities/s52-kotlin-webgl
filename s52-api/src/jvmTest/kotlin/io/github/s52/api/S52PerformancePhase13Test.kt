package io.github.s52.api

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class S52PerformancePhase13Test {
    @Test
    fun cachedRuntimeHitsOnRepeatedEquivalentRequests() {
        val cached = S52.cachedRuntime(maxEntries = 2)
        val features = listOf(depthArea())

        val first = cached.portray(features)
        val second = cached.portray(listOf(depthArea()))

        assertEquals(first, second)
        val stats = cached.cacheStats()
        assertEquals(1, stats.hits)
        assertEquals(1, stats.misses)
        assertEquals(1, stats.size)
    }

    @Test
    fun performanceReportIncludesBatchAndCacheMetrics() {
        val cached = S52.defaultRuntime().cached(maxEntries = 2)
        val report = cached.performanceReport(listOf(depthArea(), sounding()))

        assertEquals(2, report.inputFeatureCount)
        assertTrue(report.outputCommandCount >= 2)
        assertTrue(report.batchReport.batchCount >= 1)
        assertEquals(1, report.cacheStats?.misses)
    }

    private fun depthArea(): EncFeature = EncFeature(
        id = 1,
        objectClass = S57ObjectClass.DEPARE,
        primitive = PrimitiveType.Area,
        attributes = S57Attributes.of(
            S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
            S57Attribute.DRVAL2 to S57Value.Decimal(4.0)
        ),
        geometry = EncGeometry.Polygon(
            outer = listOf(
                Coordinate(-74.0, 40.0),
                Coordinate(-73.9, 40.0),
                Coordinate(-73.9, 40.1),
                Coordinate(-74.0, 40.1),
                Coordinate(-74.0, 40.0)
            )
        )
    )

    private fun sounding(): EncFeature = EncFeature(
        id = 2,
        objectClass = S57ObjectClass.SOUNDG,
        primitive = PrimitiveType.Point,
        attributes = S57Attributes.of(S57Attribute.VALSOU to S57Value.Decimal(4.2)),
        geometry = EncGeometry.Point(Coordinate(-73.95, 40.05))
    )
}
