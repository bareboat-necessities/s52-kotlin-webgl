package io.github.s52.core.engine

import io.github.s52.catalog.S57ObjectClass
import io.github.s52.core.csp.CspRegistry
import io.github.s52.core.csp.EmptyCspRegistry
import io.github.s52.core.draw.DisplayCategoryFilter
import io.github.s52.core.draw.DisplayPrioritySorter
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.draw.ViewingGroupFilter
import io.github.s52.core.instruction.S52Instruction
import io.github.s52.core.lookup.LookupMatch
import io.github.s52.core.lookup.LookupRecord
import io.github.s52.core.lookup.LookupTable
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext

class S52PortrayalEngine(
    private val lookupTable: LookupTable,
    private val cspRegistry: CspRegistry = EmptyCspRegistry
) {
    fun portray(
        features: List<EncFeature>,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52DrawCommand> {
        return features
            .flatMap { feature -> portrayFeature(feature, settings, context) }
            .filter { DisplayCategoryFilter.isVisible(it.category, settings.displayCategory) }
            .filter { ViewingGroupFilter.isVisible(it.viewingGroup, settings) }
            .sortedWith(DisplayPrioritySorter)
    }

    fun portrayFeature(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52DrawCommand> {
        return lookupTable.matchDetailed(feature, settings, context).flatMap { match ->
            expandInstructions(match, feature, settings, context)
                .mapNotNull { instruction -> instruction.toDrawCommand(feature, match.record, settings) }
        }
    }

    fun lookupMatches(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<LookupMatch> = lookupTable.matchDetailed(feature, settings, context)

    private fun expandInstructions(
        match: LookupMatch,
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext
    ): List<S52Instruction> {
        return match.record.instructions.flatMap { instruction ->
            when (instruction) {
                is S52Instruction.Conditional -> cspRegistry.evaluate(
                    instruction.cspName,
                    feature,
                    settings,
                    context
                )
                else -> listOf(instruction)
            }
        }
    }

    private fun S52Instruction.toDrawCommand(
        feature: EncFeature,
        record: LookupRecord,
        settings: MarinerSettings
    ): S52DrawCommand? {
        return when (this) {
            is S52Instruction.AreaColor -> S52DrawCommand.AreaFill(
                featureId = feature.id,
                geometry = feature.geometry,
                colorToken = colorToken,
                priority = record.displayPriority,
                viewingGroup = record.viewingGroup,
                category = record.displayCategory,
                overRadar = record.overRadar
            )
            is S52Instruction.AreaPattern -> S52DrawCommand.AreaPattern(
                featureId = feature.id,
                geometry = feature.geometry,
                patternName = name,
                parameters = parameters,
                priority = record.displayPriority,
                viewingGroup = record.viewingGroup,
                category = record.displayCategory,
                overRadar = record.overRadar
            )
            is S52Instruction.SimpleLine -> S52DrawCommand.LineSimple(
                featureId = feature.id,
                geometry = feature.geometry,
                style = style,
                width = width,
                colorToken = colorToken,
                priority = record.displayPriority,
                viewingGroup = record.viewingGroup,
                category = record.displayCategory,
                overRadar = record.overRadar
            )
            is S52Instruction.ComplexLine -> S52DrawCommand.LineComplex(
                featureId = feature.id,
                geometry = feature.geometry,
                lineStyleName = name,
                parameters = parameters,
                priority = record.displayPriority,
                viewingGroup = record.viewingGroup,
                category = record.displayCategory,
                overRadar = record.overRadar
            )
            is S52Instruction.Symbol -> S52DrawCommand.PointSymbol(
                featureId = feature.id,
                geometry = feature.geometry,
                symbolName = name,
                parameters = parameters,
                rotationDegrees = parameters.firstNotNullOfOrNull { it.toDoubleOrNull() },
                priority = record.displayPriority,
                viewingGroup = record.viewingGroup,
                category = record.displayCategory,
                overRadar = record.overRadar
            )
            is S52Instruction.Text -> textInstructionToDrawCommand(feature, record, settings)
            is S52Instruction.Conditional -> null
        }
    }

    private fun S52Instruction.Text.textInstructionToDrawCommand(
        feature: EncFeature,
        record: LookupRecord,
        settings: MarinerSettings
    ): S52DrawCommand? {
        if (feature.objectClass == S57ObjectClass.SOUNDG) {
            if (!settings.showSoundings) return null
            val depthLabel = S52TextResolver.resolveSoundingLabel(feature, textExpression) ?: return null
            return S52DrawCommand.Sounding(
                featureId = feature.id,
                geometry = feature.geometry,
                depthLabel = depthLabel,
                colorToken = rawArgs.lastOrNull()?.takeIf { it.isStableToken() } ?: "SNDG1",
                priority = record.displayPriority,
                viewingGroup = record.viewingGroup,
                category = record.displayCategory,
                overRadar = record.overRadar
            )
        }

        if (!settings.showText) return null
        val label = S52TextResolver.resolveText(feature, textExpression, rawArgs)
        if (label.isBlank()) return null
        return S52DrawCommand.Text(
            featureId = feature.id,
            geometry = feature.geometry,
            textExpression = label,
            rawArgs = rawArgs,
            textKind = kind,
            colorToken = rawArgs.lastOrNull()?.takeIf { it.isStableToken() },
            priority = record.displayPriority,
            viewingGroup = record.viewingGroup,
            category = record.displayCategory,
            overRadar = record.overRadar
        )
    }

    private fun String.isStableToken(): Boolean =
        isNotBlank() && all { it.isUpperCase() || it.isDigit() || it == '_' }
}
