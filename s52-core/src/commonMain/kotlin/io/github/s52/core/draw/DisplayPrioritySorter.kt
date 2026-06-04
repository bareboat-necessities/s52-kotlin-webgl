package io.github.s52.core.draw

object DisplayPrioritySorter : Comparator<S52DrawCommand> {
    override fun compare(a: S52DrawCommand, b: S52DrawCommand): Int =
        compareValuesBy(a, b, S52DrawCommand::priority, S52DrawCommand::viewingGroup, S52DrawCommand::featureId)
}
