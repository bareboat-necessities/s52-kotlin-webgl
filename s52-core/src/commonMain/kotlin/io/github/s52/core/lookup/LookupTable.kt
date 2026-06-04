package io.github.s52.core.lookup

import io.github.s52.catalog.PrimitiveType
import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

class LookupTable(
    records: List<LookupRecord>
) {
    private val indexedRecords: List<IndexedRecord> = records.mapIndexed { index, record -> IndexedRecord(index, record) }
    private val byObjectAndPrimitive: Map<Key, List<IndexedRecord>> = indexedRecords
        .groupBy { Key(it.record.objectClass, it.record.primitive) }
        .mapValues { (_, recordsForKey) ->
            recordsForKey.sortedWith(
                compareByDescending<IndexedRecord> { it.record.attributeFilter.specificity }
                    .thenBy { it.record.sourceIndex }
                    .thenBy { it.index }
            )
        }

    fun records(): List<LookupRecord> = indexedRecords.map { it.record }

    fun candidates(objectClass: S57ObjectClass, primitive: PrimitiveType): List<LookupRecord> =
        byObjectAndPrimitive[Key(objectClass, primitive)].orEmpty().map { it.record }

    fun match(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<LookupRecord> = matchDetailed(feature, settings, context).map { it.record }

    fun matchDetailed(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<LookupMatch> {
        val key = Key(feature.objectClass, feature.primitive)
        return byObjectAndPrimitive[key].orEmpty()
            .asSequence()
            .filter { indexed -> indexed.record.matchesScale(feature, settings, context) }
            .filter { indexed -> indexed.record.attributeFilter.matches(feature) }
            .map { indexed -> LookupMatch(indexed.record, indexed.index) }
            .sortedWith(LookupMatchSorter)
            .toList()
    }

    fun explain(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): LookupExplanation {
        val key = Key(feature.objectClass, feature.primitive)
        val candidates = byObjectAndPrimitive[key].orEmpty()
        val rejected = candidates.mapNotNull { indexed ->
            val scaleOk = indexed.record.matchesScale(feature, settings, context)
            val attrsOk = indexed.record.attributeFilter.matches(feature)
            when {
                scaleOk && attrsOk -> null
                !scaleOk -> LookupRejection(indexed.record, indexed.index, LookupRejectionReason.Scale)
                else -> LookupRejection(indexed.record, indexed.index, LookupRejectionReason.AttributeFilter)
            }
        }
        return LookupExplanation(
            candidateCount = candidates.size,
            matches = matchDetailed(feature, settings, context),
            rejected = rejected
        )
    }

    private fun LookupRecord.matchesScale(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): Boolean {
        val scale = context.displayScale.takeIf { it > 0.0 } ?: settings.scale

        // S-57 SCAMIN-like visibility on normalized features.
        if (feature.scaleMin != null && scale > feature.scaleMin) return false
        if (feature.scaleMax != null && scale < feature.scaleMax) return false

        // Optional Presentation Library row-level scale constraints.
        if (minimumDisplayScale != null && scale < minimumDisplayScale) return false
        if (maximumDisplayScale != null && scale > maximumDisplayScale) return false
        return true
    }

    private data class Key(val objectClass: S57ObjectClass, val primitive: PrimitiveType)
    private data class IndexedRecord(val index: Int, val record: LookupRecord)
}

object LookupMatchSorter : Comparator<LookupMatch> {
    override fun compare(a: LookupMatch, b: LookupMatch): Int =
        compareValuesBy(
            a,
            b,
            { -it.attributeSpecificity },
            { it.record.sourceIndex },
            { it.recordIndex }
        )
}

data class LookupExplanation(
    val candidateCount: Int,
    val matches: List<LookupMatch>,
    val rejected: List<LookupRejection>
)

data class LookupRejection(
    val record: LookupRecord,
    val recordIndex: Int,
    val reason: LookupRejectionReason
)

enum class LookupRejectionReason {
    Scale,
    AttributeFilter
}
