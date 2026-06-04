package io.github.s52.core.performance

import io.github.s52.core.draw.S52DrawCommand

/**
 * Small deterministic LRU cache for portrayal command lists.
 *
 * The cache is intentionally simple and common-source friendly. It is best used
 * at application/runtime boundaries where the same feature set is portrayed
 * repeatedly while panning, repainting, or switching purely renderer-side state.
 */
class PortrayalCache(
    val maxEntries: Int = 64
) {
    init {
        require(maxEntries > 0) { "PortrayalCache maxEntries must be positive" }
    }

    private val entries = mutableMapOf<PortrayalRequestKey, List<S52DrawCommand>>()
    private val order = mutableListOf<PortrayalRequestKey>()

    private var hitCount: Long = 0
    private var missCount: Long = 0
    private var evictionCount: Long = 0

    fun getOrPut(
        key: PortrayalRequestKey,
        producer: () -> List<S52DrawCommand>
    ): List<S52DrawCommand> {
        val existing = entries[key]
        if (existing != null) {
            hitCount += 1
            touch(key)
            return existing
        }

        missCount += 1
        val produced = producer().toList()
        entries[key] = produced
        order += key
        trimToCapacity()
        return produced
    }

    fun clear() {
        entries.clear()
        order.clear()
    }

    fun stats(): PortrayalCacheStats = PortrayalCacheStats(
        maxEntries = maxEntries,
        size = entries.size,
        hits = hitCount,
        misses = missCount,
        evictions = evictionCount
    )

    private fun touch(key: PortrayalRequestKey) {
        order.remove(key)
        order += key
    }

    private fun trimToCapacity() {
        while (entries.size > maxEntries && order.isNotEmpty()) {
            val oldest = order.removeAt(0)
            if (entries.remove(oldest) != null) {
                evictionCount += 1
            }
        }
    }
}

data class PortrayalCacheStats(
    val maxEntries: Int,
    val size: Int,
    val hits: Long,
    val misses: Long,
    val evictions: Long
) {
    val requests: Long get() = hits + misses
    val hitRate: Double get() = if (requests == 0L) 0.0 else hits.toDouble() / requests.toDouble()
}
