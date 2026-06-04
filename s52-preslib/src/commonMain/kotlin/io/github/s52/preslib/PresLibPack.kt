package io.github.s52.preslib

import io.github.s52.core.lookup.LookupTable

/** Runtime Presentation Library registries used by the portrayal engine. */
data class PresLibPack(
    val lookupTable: LookupTable,
    val colors: ColorTables,
    val symbols: SymbolRegistry,
    val lineStyles: LineStyleRegistry,
    val patterns: PatternRegistry
) {
    companion object {
        /**
         * Phase 0 compatibility alias. The pack now comes from the Phase 2
         * generated-style source pipeline rather than being assembled by hand
         * directly in this class.
         */
        fun phase0Minimal(): PresLibPack = phase2Synthetic()

        fun phase2Synthetic(): PresLibPack = io.github.s52.preslib.generated.GeneratedPhase2PresLib.pack()
    }
}
