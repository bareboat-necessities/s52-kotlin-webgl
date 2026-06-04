package io.github.s52.core.performance

import io.github.s52.core.draw.DrawCommandKind
import io.github.s52.core.draw.DisplayPrioritySorter
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.settings.DisplayCategory

/** Groups renderer-independent commands into stable renderer-friendly batches. */
object DrawCommandBatcher {
    fun batches(commands: List<S52DrawCommand>): List<DrawCommandBatch> {
        return commands
            .groupBy { it.batchKey() }
            .map { (key, grouped) -> DrawCommandBatch(key, grouped.sortedWith(DisplayPrioritySorter)) }
            .sortedWith(
                compareBy<DrawCommandBatch> { it.key.priority }
                    .thenBy { it.key.kind.order }
                    .thenBy { it.key.token }
                    .thenBy { it.key.viewingGroup }
                    .thenBy { if (it.key.overRadar) 1 else 0 }
            )
    }

    fun report(commands: List<S52DrawCommand>): DrawBatchReport {
        val batches = batches(commands)
        val commandsByKind = commands
            .groupBy { it.kind }
            .mapValues { (_, values) -> values.size }
            .toOrderedKindMap()
        val batchesByKind = batches
            .groupBy { it.key.kind }
            .mapValues { (_, values) -> values.size }
            .toOrderedKindMap()
        return DrawBatchReport(
            commandCount = commands.size,
            batchCount = batches.size,
            commandsByKind = commandsByKind,
            batchesByKind = batchesByKind
        )
    }

    private fun S52DrawCommand.batchKey(): DrawBatchKey = DrawBatchKey(
        kind = kind,
        token = batchToken(),
        priority = priority,
        viewingGroup = viewingGroup,
        category = category,
        overRadar = overRadar
    )

    private fun Map<DrawCommandKind, Int>.toOrderedKindMap(): Map<DrawCommandKind, Int> = entries
        .sortedBy { it.key.order }
        .fold(linkedMapOf<DrawCommandKind, Int>()) { acc, entry ->
            acc[entry.key] = entry.value
            acc
        }

    private fun S52DrawCommand.batchToken(): String = when (this) {
        is S52DrawCommand.AreaFill -> colorToken
        is S52DrawCommand.AreaPattern -> patternName
        is S52DrawCommand.LineSimple -> "$style|$width|$colorToken"
        is S52DrawCommand.LineComplex -> lineStyleName
        is S52DrawCommand.PointSymbol -> symbolName
        is S52DrawCommand.Text -> colorToken ?: textKind.token
        is S52DrawCommand.Sounding -> colorToken
    }
}

data class DrawBatchKey(
    val kind: DrawCommandKind,
    val token: String,
    val priority: Int,
    val viewingGroup: Int,
    val category: DisplayCategory,
    val overRadar: Boolean
)

data class DrawCommandBatch(
    val key: DrawBatchKey,
    val commands: List<S52DrawCommand>
)

data class DrawBatchReport(
    val commandCount: Int,
    val batchCount: Int,
    val commandsByKind: Map<DrawCommandKind, Int>,
    val batchesByKind: Map<DrawCommandKind, Int>
) {
    val averageCommandsPerBatch: Double get() = if (batchCount == 0) 0.0 else commandCount.toDouble() / batchCount.toDouble()
}
