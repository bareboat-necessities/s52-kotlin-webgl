package io.github.s52.core.draw

/**
 * Deterministic painter's-order sort for already-portrayed S-52 draw commands.
 *
 * S-52 display priority is the primary ordering key. The command kind order is
 * only a stable tie-breaker so area fills do not jump above text or symbols
 * when synthetic/incomplete lookup rows share a priority.
 */
object DisplayPrioritySorter : Comparator<S52DrawCommand> {
    override fun compare(a: S52DrawCommand, b: S52DrawCommand): Int =
        compareValuesBy(
            a,
            b,
            S52DrawCommand::priority,
            { it.commandKindOrder() },
            S52DrawCommand::viewingGroup,
            S52DrawCommand::featureId,
            { if (it.overRadar) 1 else 0 }
        )

    private fun S52DrawCommand.commandKindOrder(): Int = when (this) {
        is S52DrawCommand.AreaFill -> 0
        is S52DrawCommand.AreaPattern -> 1
        is S52DrawCommand.LineSimple -> 2
        is S52DrawCommand.LineComplex -> 3
        is S52DrawCommand.PointSymbol -> 4
        is S52DrawCommand.Text -> 5
    }
}
