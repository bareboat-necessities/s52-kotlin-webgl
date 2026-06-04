package io.github.s52.api

import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.BoundaryStyle
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.core.settings.S52Palette
import io.github.s52.core.settings.SymbolStyle

/**
 * Public convenience entry point for S-52 portrayal.
 *
 * Applications that need one straightforward integration path can call
 * [defaultRuntime] and [S52Runtime.portray]. Advanced applications can still
 * construct lower-level modules directly.
 */
object S52 {
    val version: S52Version = S52Version.Current

    /** Build the default Phase 14 runtime using the synthetic pack and complete CSP registry. */
    fun defaultRuntime(): S52Runtime = S52Runtime.synthetic()

    /** Stable default mariner settings used by examples and tests. */
    fun defaultSettings(
        displayCategory: DisplayCategory = DisplayCategory.Standard,
        palette: S52Palette = S52Palette.DayBright,
        safetyDepthMeters: Double = 10.0,
        safetyContourMeters: Double = 10.0,
        shallowContourMeters: Double = 2.0,
        deepContourMeters: Double = 30.0,
        showText: Boolean = true,
        showSoundings: Boolean = true,
        showLightDescriptions: Boolean = true,
        scale: Double = 50_000.0,
        symbolStyle: SymbolStyle = SymbolStyle.Simplified,
        boundaryStyle: BoundaryStyle = BoundaryStyle.Plain,
        enabledViewingGroups: Set<Int>? = null,
        disabledViewingGroups: Set<Int> = emptySet()
    ): MarinerSettings = MarinerSettings(
        displayCategory = displayCategory,
        palette = palette,
        safetyDepthMeters = safetyDepthMeters,
        safetyContourMeters = safetyContourMeters,
        shallowContourMeters = shallowContourMeters,
        deepContourMeters = deepContourMeters,
        showText = showText,
        showSoundings = showSoundings,
        showLightDescriptions = showLightDescriptions,
        scale = scale,
        symbolStyle = symbolStyle,
        boundaryStyle = boundaryStyle,
        enabledViewingGroups = enabledViewingGroups,
        disabledViewingGroups = disabledViewingGroups
    )

    /** Build a default portrayal context from mariner settings. */
    fun defaultContext(
        settings: MarinerSettings = defaultSettings(),
        viewportId: String = "default"
    ): PortrayalContext = PortrayalContext(
        compilationScale = settings.scale,
        displayScale = settings.scale,
        viewportId = viewportId
    )

    /** Cached convenience runtime for repeated repaint cycles. */
    fun cachedRuntime(maxEntries: Int = 64): S52CachedRuntime = defaultRuntime().cached(maxEntries)

    /** One-call convenience portrayal using the default runtime. */
    fun portray(
        features: List<EncFeature>,
        settings: MarinerSettings = defaultSettings(),
        context: PortrayalContext = defaultContext(settings)
    ): List<S52DrawCommand> = defaultRuntime().portray(features, settings, context)
}
