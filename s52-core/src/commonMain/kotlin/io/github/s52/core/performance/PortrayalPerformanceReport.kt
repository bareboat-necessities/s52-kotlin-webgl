package io.github.s52.core.performance

import io.github.s52.core.draw.S52DrawCommand

/** Lightweight, deterministic report used by tests and applications to watch command volume. */
data class PortrayalPerformanceReport(
    val inputFeatureCount: Int,
    val outputCommandCount: Int,
    val batchReport: DrawBatchReport,
    val cacheStats: PortrayalCacheStats? = null
) {
    companion object {
        fun from(
            inputFeatureCount: Int,
            commands: List<S52DrawCommand>,
            cacheStats: PortrayalCacheStats? = null
        ): PortrayalPerformanceReport = PortrayalPerformanceReport(
            inputFeatureCount = inputFeatureCount,
            outputCommandCount = commands.size,
            batchReport = DrawCommandBatcher.report(commands),
            cacheStats = cacheStats
        )
    }
}
