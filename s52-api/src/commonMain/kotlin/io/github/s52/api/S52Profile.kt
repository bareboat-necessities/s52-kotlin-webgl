package io.github.s52.api

import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.BoundaryStyle
import io.github.s52.core.settings.DisplayCategory
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.core.settings.S52Palette
import io.github.s52.core.settings.SymbolStyle

/**
 * Stable consumer-facing portrayal profile.
 *
 * A profile captures the mariner/display settings needed to make a portrayal
 * request reproducible across demos, tests, issue reports, and downstream
 * chart-engine integrations. It deliberately does not include chart data,
 * official Presentation Library assets, navigation state, AIS, GPS, or route
 * information.
 */
data class S52PortrayalProfile(
    val id: String,
    val displayName: String,
    val description: String,
    val settings: MarinerSettings
) {
    init {
        require(id.isNotBlank()) { "Profile id must not be blank." }
        require(displayName.isNotBlank()) { "Profile display name must not be blank." }
        require(settings.safetyDepthMeters >= 0.0) { "Safety depth must be non-negative." }
        require(settings.safetyContourMeters >= 0.0) { "Safety contour must be non-negative." }
        require(settings.shallowContourMeters >= 0.0) { "Shallow contour must be non-negative." }
        require(settings.deepContourMeters >= settings.shallowContourMeters) {
            "Deep contour must be greater than or equal to shallow contour."
        }
        require(settings.scale > 0.0) { "Display scale must be positive." }
    }

    fun defaultContext(compilationScale: Double = settings.scale): PortrayalContext = PortrayalContext(
        compilationScale = compilationScale,
        displayScale = settings.scale
    )

    fun request(
        features: List<EncFeature>,
        context: PortrayalContext = defaultContext()
    ): S52PortrayalRequest = S52PortrayalRequest(
        features = features,
        settings = settings,
        context = context
    )

    fun summary(): S52ProfileSummary = S52ProfileSummary(
        id = id,
        displayName = displayName,
        displayCategory = settings.displayCategory,
        palette = settings.palette,
        safetyDepthMeters = settings.safetyDepthMeters,
        safetyContourMeters = settings.safetyContourMeters,
        shallowContourMeters = settings.shallowContourMeters,
        deepContourMeters = settings.deepContourMeters,
        showText = settings.showText,
        showSoundings = settings.showSoundings,
        showLightDescriptions = settings.showLightDescriptions,
        scale = settings.scale
    )

    fun toProperties(): String = buildString {
        appendLine("id=$id")
        appendLine("displayName=$displayName")
        appendLine("displayCategory=${settings.displayCategory}")
        appendLine("palette=${settings.palette}")
        appendLine("symbolStyle=${settings.symbolStyle}")
        appendLine("boundaryStyle=${settings.boundaryStyle}")
        appendLine("safetyDepthMeters=${settings.safetyDepthMeters}")
        appendLine("safetyContourMeters=${settings.safetyContourMeters}")
        appendLine("shallowContourMeters=${settings.shallowContourMeters}")
        appendLine("deepContourMeters=${settings.deepContourMeters}")
        appendLine("showText=${settings.showText}")
        appendLine("showSoundings=${settings.showSoundings}")
        appendLine("showLightDescriptions=${settings.showLightDescriptions}")
        appendLine("scale=${settings.scale}")
        appendLine("enabledViewingGroups=${settings.enabledViewingGroups?.joinToString(",") ?: ""}")
        appendLine("disabledViewingGroups=${settings.disabledViewingGroups.joinToString(",")}")
    }
}

/** Compact immutable profile summary suitable for manifests, logs, and diagnostics. */
data class S52ProfileSummary(
    val id: String,
    val displayName: String,
    val displayCategory: DisplayCategory,
    val palette: S52Palette,
    val safetyDepthMeters: Double,
    val safetyContourMeters: Double,
    val shallowContourMeters: Double,
    val deepContourMeters: Double,
    val showText: Boolean,
    val showSoundings: Boolean,
    val showLightDescriptions: Boolean,
    val scale: Double
) {
    fun toMarkdown(): String = buildString {
        appendLine("# S-52 Portrayal Profile")
        appendLine()
        appendLine("- Id: $id")
        appendLine("- Name: $displayName")
        appendLine("- Display category: $displayCategory")
        appendLine("- Palette: $palette")
        appendLine("- Safety depth: $safetyDepthMeters m")
        appendLine("- Safety contour: $safetyContourMeters m")
        appendLine("- Shallow contour: $shallowContourMeters m")
        appendLine("- Deep contour: $deepContourMeters m")
        appendLine("- Show text: $showText")
        appendLine("- Show soundings: $showSoundings")
        appendLine("- Show light descriptions: $showLightDescriptions")
        appendLine("- Scale: 1:$scale")
    }
}

/** Curated built-in profile identifiers for demos, tests, and downstream examples. */
enum class S52ProfilePreset(val id: String, val displayName: String) {
    SafetyDay("safety-day", "Safety day"),
    PlanningDay("planning-day", "Planning day"),
    NightMinimal("night-minimal", "Night minimal"),
    DiagnosticsAll("diagnostics-all", "Diagnostics all")
}

/** Dependency-free profile catalogue with deterministic order and stable ids. */
object S52ProfileCatalog {
    val safetyDay: S52PortrayalProfile = S52PortrayalProfile(
        id = S52ProfilePreset.SafetyDay.id,
        displayName = S52ProfilePreset.SafetyDay.displayName,
        description = "Standard day-palette safety profile with text and soundings enabled.",
        settings = MarinerSettings(
            displayCategory = DisplayCategory.Standard,
            palette = S52Palette.DayBright,
            symbolStyle = SymbolStyle.Simplified,
            boundaryStyle = BoundaryStyle.Plain,
            safetyDepthMeters = 10.0,
            safetyContourMeters = 10.0,
            shallowContourMeters = 2.0,
            deepContourMeters = 30.0,
            showText = true,
            showSoundings = true,
            showLightDescriptions = true,
            scale = 50_000.0
        )
    )

    val planningDay: S52PortrayalProfile = S52PortrayalProfile(
        id = S52ProfilePreset.PlanningDay.id,
        displayName = S52ProfilePreset.PlanningDay.displayName,
        description = "Other-category day profile useful for reviewing overlays and non-standard display groups.",
        settings = MarinerSettings(
            displayCategory = DisplayCategory.Other,
            palette = S52Palette.DayBright,
            symbolStyle = SymbolStyle.Simplified,
            boundaryStyle = BoundaryStyle.Symbolized,
            safetyDepthMeters = 10.0,
            safetyContourMeters = 10.0,
            shallowContourMeters = 2.0,
            deepContourMeters = 30.0,
            showText = true,
            showSoundings = true,
            showLightDescriptions = true,
            scale = 50_000.0
        )
    )

    val nightMinimal: S52PortrayalProfile = S52PortrayalProfile(
        id = S52ProfilePreset.NightMinimal.id,
        displayName = S52ProfilePreset.NightMinimal.displayName,
        description = "Night palette profile with optional clutter reduced for UI smoke tests and examples.",
        settings = MarinerSettings(
            displayCategory = DisplayCategory.Standard,
            palette = S52Palette.Night,
            symbolStyle = SymbolStyle.Simplified,
            boundaryStyle = BoundaryStyle.Plain,
            safetyDepthMeters = 10.0,
            safetyContourMeters = 10.0,
            shallowContourMeters = 2.0,
            deepContourMeters = 30.0,
            showText = false,
            showSoundings = false,
            showLightDescriptions = false,
            scale = 50_000.0
        )
    )

    val diagnosticsAll: S52PortrayalProfile = S52PortrayalProfile(
        id = S52ProfilePreset.DiagnosticsAll.id,
        displayName = S52ProfilePreset.DiagnosticsAll.displayName,
        description = "Verbose profile intended for command transcript and support diagnostics.",
        settings = MarinerSettings(
            displayCategory = DisplayCategory.Other,
            palette = S52Palette.DayBright,
            symbolStyle = SymbolStyle.PaperChart,
            boundaryStyle = BoundaryStyle.Symbolized,
            safetyDepthMeters = 5.0,
            safetyContourMeters = 10.0,
            shallowContourMeters = 2.0,
            deepContourMeters = 30.0,
            showText = true,
            showSoundings = true,
            showLightDescriptions = true,
            scale = 25_000.0
        )
    )

    val all: List<S52PortrayalProfile> = listOf(
        safetyDay,
        planningDay,
        nightMinimal,
        diagnosticsAll
    )

    val default: S52PortrayalProfile = safetyDay

    private val byId: Map<String, S52PortrayalProfile> = all.associateBy { it.id }

    fun fromPreset(preset: S52ProfilePreset): S52PortrayalProfile = require(preset.id)

    fun find(id: String): S52PortrayalProfile? = byId[id]

    fun require(id: String): S52PortrayalProfile = find(id)
        ?: error("Unknown S-52 profile id '$id'. Known profiles: ${all.joinToString { it.id }}")

    fun markdownCatalog(): String = buildString {
        appendLine("# S-52 Built-in Portrayal Profiles")
        appendLine()
        all.forEach { profile ->
            appendLine("## ${profile.displayName}")
            appendLine()
            appendLine("- Id: `${profile.id}`")
            appendLine("- Description: ${profile.description}")
            appendLine("- Display category: ${profile.settings.displayCategory}")
            appendLine("- Palette: ${profile.settings.palette}")
            appendLine("- Scale: 1:${profile.settings.scale}")
            appendLine("- Text: ${profile.settings.showText}")
            appendLine("- Soundings: ${profile.settings.showSoundings}")
            appendLine()
        }
    }
}

fun S52PortrayalSession.portray(
    features: List<EncFeature>,
    profile: S52PortrayalProfile = S52ProfileCatalog.default,
    context: PortrayalContext = profile.defaultContext()
): S52PortrayalResult = portray(profile.request(features, context))

fun S52PortrayalSession.diagnosticBundle(
    features: List<EncFeature>,
    profile: S52PortrayalProfile = S52ProfileCatalog.default,
    name: String = "s52-kotlin-webgl-diagnostics",
    transcriptPreviewLineLimit: Int = S52DiagnosticBundle.DEFAULT_TRANSCRIPT_PREVIEW_LINES,
    context: PortrayalContext = profile.defaultContext()
): S52DiagnosticBundle = diagnosticBundle(
    request = profile.request(features, context),
    name = name,
    transcriptPreviewLineLimit = transcriptPreviewLineLimit
)
