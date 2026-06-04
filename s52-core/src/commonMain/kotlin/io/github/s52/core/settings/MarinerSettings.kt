package io.github.s52.core.settings

data class MarinerSettings(
    val displayCategory: DisplayCategory = DisplayCategory.Standard,
    val palette: S52Palette = S52Palette.DayBright,
    val symbolStyle: SymbolStyle = SymbolStyle.Simplified,
    val boundaryStyle: BoundaryStyle = BoundaryStyle.Plain,
    val safetyDepthMeters: Double = 10.0,
    val safetyContourMeters: Double = 10.0,
    val shallowContourMeters: Double = 2.0,
    val deepContourMeters: Double = 30.0,
    val showText: Boolean = true,
    val showSoundings: Boolean = true,
    val showLightDescriptions: Boolean = true,
    val scale: Double = 50_000.0,
    /**
     * Optional positive viewing-group selection. When null, all groups allowed
     * by [displayCategory] are candidates. When non-null, commands outside the
     * set are hidden before rendering.
     */
    val enabledViewingGroups: Set<Int>? = null,
    /** Negative viewing-group selection applied after [enabledViewingGroups]. */
    val disabledViewingGroups: Set<Int> = emptySet()
)
