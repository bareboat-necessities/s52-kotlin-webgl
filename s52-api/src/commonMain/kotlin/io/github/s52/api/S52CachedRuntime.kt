package io.github.s52.api

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.model.EncFeature
import io.github.s52.core.performance.PortrayalCache
import io.github.s52.core.performance.PortrayalCacheStats
import io.github.s52.core.performance.PortrayalPerformanceReport
import io.github.s52.core.performance.PortrayalRequestKey
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

/**
 * Public cached wrapper for applications that repeatedly portray unchanged
 * normalized features across repaint cycles.
 *
 * The cache key includes feature content, mariner settings, and portrayal
 * context. Renderer-only changes such as canvas size do not invalidate cached
 * commands because they are outside the portrayal boundary.
 */
class S52CachedRuntime(
    val runtime: S52Runtime,
    val cache: PortrayalCache = PortrayalCache()
) {
    fun portray(
        features: List<EncFeature>,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): List<S52DrawCommand> {
        val key = PortrayalRequestKey.from(features, settings, context)
        return cache.getOrPut(key) { runtime.portray(features, settings, context) }
    }

    fun portrayValidated(
        features: List<EncFeature>,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): S52PortrayalResult {
        val commands = portray(features, settings, context)
        return S52PortrayalResult(
            commands = commands,
            validation = io.github.s52.core.draw.DrawCommandValidator.validate(commands)
        )
    }

    fun performanceReport(
        features: List<EncFeature>,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): PortrayalPerformanceReport = PortrayalPerformanceReport.from(
        inputFeatureCount = features.size,
        commands = portray(features, settings, context),
        cacheStats = cache.stats()
    )

    fun cacheStats(): PortrayalCacheStats = cache.stats()

    fun clearCache() = cache.clear()
}
