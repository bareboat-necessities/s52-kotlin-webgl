package io.github.s52.api

import io.github.s52.core.csp.CspRegistry
import io.github.s52.core.draw.DrawCommandValidator
import io.github.s52.core.draw.S52DrawCommand
import io.github.s52.core.draw.S52DrawCommandTranscript
import io.github.s52.core.performance.PortrayalCache
import io.github.s52.core.performance.PortrayalPerformanceReport
import io.github.s52.core.engine.S52PortrayalEngine
import io.github.s52.core.lookup.LookupExplanation
import io.github.s52.core.model.EncFeature
import io.github.s52.core.settings.MarinerSettings
import io.github.s52.core.settings.PortrayalContext
import io.github.s52.csp.DefaultCspRegistry
import io.github.s52.preslib.PresLibPack

/**
 * Stable high-level runtime for applications that want S-52 portrayal without
 * wiring individual internal modules manually.
 *
 * The runtime owns the Presentation Library pack, CSP registry, and core
 * portrayal engine. It accepts already-normalized ENC-like features and returns
 * renderer-independent [S52DrawCommand] values.
 */
class S52Runtime private constructor(
    val presLib: PresLibPack,
    val cspRegistry: CspRegistry,
    val engine: S52PortrayalEngine
) {
    /** Portray normalized features into renderer-independent draw commands. */
    fun portray(
        features: List<EncFeature>,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): List<S52DrawCommand> = engine.portray(features, settings, context)

    /**
     * Portray features and immediately validate the generated command list.
     * This is useful for tests and for integration diagnostics before commands
     * are handed to a renderer.
     */
    fun portrayValidated(
        features: List<EncFeature>,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): S52PortrayalResult {
        val commands = portray(features, settings, context)
        return S52PortrayalResult(commands, DrawCommandValidator.validate(commands))
    }

    /** Return a deterministic transcript of the current portrayal output. */
    fun transcript(
        features: List<EncFeature>,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): String = S52DrawCommandTranscript.serialize(portray(features, settings, context))


    /** Build a cached wrapper for repeated repaint/portrayal cycles. */
    fun cached(maxEntries: Int = 64): S52CachedRuntime = S52CachedRuntime(this, PortrayalCache(maxEntries))

    /** Compute deterministic command batching and optional cache metrics. */
    fun performanceReport(
        features: List<EncFeature>,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): PortrayalPerformanceReport = PortrayalPerformanceReport.from(
        inputFeatureCount = features.size,
        commands = portray(features, settings, context)
    )

    /** Explain lookup-table candidate matching for one feature. */
    fun explainLookup(
        feature: EncFeature,
        settings: MarinerSettings = S52.defaultSettings(),
        context: PortrayalContext = S52.defaultContext(settings)
    ): LookupExplanation = engine.lookupMatches(feature, settings, context).let { matches ->
        presLib.lookupTable.explain(feature, settings, context).copy(matches = matches)
    }

    companion object {
        /**
         * Runtime backed by the checked-in synthetic Presentation Library pack.
         *
         * Official IHO Presentation Library assets are intentionally not
         * bundled. Downstream projects should use [from] with their generated
         * or locally supplied [PresLibPack] when they integrate official data.
         */
        fun synthetic(): S52Runtime = from(
            presLib = PresLibPack.phase2Synthetic(),
            cspRegistry = DefaultCspRegistry.phase6Complete()
        )

        /** Runtime backed by the generated OpenCPN Presentation Library pack. */
        fun openCpn(): S52Runtime = from(
            presLib = PresLibPack.openCpn(),
            cspRegistry = DefaultCspRegistry.phase6Complete()
        )

        /** Build a runtime from explicit Presentation Library and CSP inputs. */
        fun from(
            presLib: PresLibPack,
            cspRegistry: CspRegistry = DefaultCspRegistry.phase6Complete()
        ): S52Runtime = S52Runtime(
            presLib = presLib,
            cspRegistry = cspRegistry,
            engine = S52PortrayalEngine(presLib.lookupTable, cspRegistry)
        )
    }
}
