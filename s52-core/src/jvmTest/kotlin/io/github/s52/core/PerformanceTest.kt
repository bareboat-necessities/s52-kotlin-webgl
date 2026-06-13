package io.github.s52.core

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57Attribute
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.geometry.Coordinate
import io.github.s52.core.geometry.EncGeometry
import io.github.s52.core.model.EncFeature
import io.github.s52.core.model.S57Attributes
import io.github.s52.core.model.S57Value
import io.github.s52.core.performance.DrawCommandBatcher
import io.github.s52.core.performance.PortrayalCache
import io.github.s52.core.performance.PortrayalRequestKey
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerformanceTest {
    @Test
    fun requestKeyUsesFeatureContentRatherThanListIdentity() {
        val settings = MarinerSettings()
        val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        val first = listOf(depthArea(id = 1))
        val rebuilt = listOf(depthArea(id = 1))

        assertEquals(
            PortrayalRequestKey.from(first, settings, context),
            PortrayalRequestKey.from(rebuilt, settings, context)
        )
    }

    @Test
    fun portrayalCacheTracksHitsMissesAndEvictions() {
        val cache = PortrayalCache(maxEntries = 1)
        val settings = MarinerSettings()
        val context = PortrayalContext(compilationScale = 50_000.0, displayScale = 50_000.0)
        val key1 = PortrayalRequestKey.from(listOf(depthArea(id = 1)), settings, context)
        val key2 = PortrayalRequestKey.from(listOf(depthArea(id = 2)), settings, context)

        cache.getOrPut(key1) { listOf(areaCommand(1)) }
        cache.getOrPut(key1) { error("second call should hit cache") }
        cache.getOrPut(key2) { listOf(areaCommand(2)) }

        val stats = cache.stats()
        assertEquals(1, stats.size)
        assertEquals(1, stats.hits)
        assertEquals(2, stats.misses)
        assertEquals(1, stats.evictions)
    }

    @Test
    fun drawCommandBatcherGroupsCommandsByRendererStableKey() {
        val commands = listOf(
            areaCommand(1),
            areaCommand(2),
            areaCommand(3, color = "LANDA"),
            lineCommand(4)
        )

        val report = DrawCommandBatcher.report(commands)
        assertEquals(4, report.commandCount)
        assertEquals(3, report.batchCount)
        assertTrue(report.averageCommandsPerBatch > 1.0)
    }

    private fun depthArea(id: Long): EncFeature = EncFeature(
        id = id,
        objectClass = S57ObjectClass.DEPARE,
        primitive = PrimitiveType.Area,
        attributes = S57Attributes.of(
            S57Attribute.DRVAL1 to S57Value.Decimal(0.0),
            S57Attribute.DRVAL2 to S57Value.Decimal(4.0)
        ),
        geometry = polygon()
    )

    private fun areaCommand(id: Long, color: String = "DEPVS"): S52DrawCommand.AreaFill = S52DrawCommand.AreaFill(
        featureId = id,
        geometry = polygon(),
        colorToken = color,
        priority = 1,
        viewingGroup = 21010,
        category = DisplayCategory.Standard,
        overRadar = false
    )

    private fun lineCommand(id: Long): S52DrawCommand.LineSimple = S52DrawCommand.LineSimple(
        featureId = id,
        geometry = EncGeometry.LineString(listOf(Coordinate(-74.0, 40.0), Coordinate(-73.9, 40.1))),
        style = "SOLD",
        width = 1.0,
        colorToken = "CHBLK",
        priority = 2,
        viewingGroup = 31010,
        category = DisplayCategory.Standard,
        overRadar = false
    )

    private fun polygon(): EncGeometry.Polygon = EncGeometry.Polygon(
        outer = listOf(
            Coordinate(-74.0, 40.0),
            Coordinate(-73.9, 40.0),
            Coordinate(-73.9, 40.1),
            Coordinate(-74.0, 40.1),
            Coordinate(-74.0, 40.0)
        )
    )
}
