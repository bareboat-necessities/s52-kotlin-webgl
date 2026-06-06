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

        fun s52LibCompat(): PresLibPack = io.github.s52.preslib.s52lib.S52LibCompatPresLib.pack()

        /**
         * Runtime pack generated from the corrected OpenCPN portrayal payload
         * under `s52/opencpn`. Phase 28C exposes the complete imported data;
         * later phases upgrade lookup filtering and WebGL rendering semantics.
         */
        fun openCpn(): PresLibPack = io.github.s52.preslib.opencpn.generated.OpenCpnGeneratedPresLib.pack()
    }
}
