package io.github.s52.csp

import io.github.s52.core.csp.ConditionalSymbologyProcedure

/** Phase 5 critical CSP identifiers and their implementations. */
enum class CspId(
    val s52Name: String,
    val procedure: ConditionalSymbologyProcedure
) {
    DEPARE("DEPARE", DepthAreaCsp()),
    DEPCNT("DEPCNT", DepthContourCsp()),
    SOUNDG("SOUNDG", SoundingCsp()),
    WRECKS("WRECKS", WrecksCsp()),
    OBSTRN("OBSTRN", ObstructionCsp()),
    LIGHTS("LIGHTS", LightsCsp()),
    TOPMAR("TOPMAR", TopmarkCsp());

    companion object {
        fun criticalPhase5Procedures(): List<ConditionalSymbologyProcedure> = entries.map { it.procedure }
        fun criticalPhase5Names(): Set<String> = entries.mapTo(mutableSetOf()) { it.s52Name }
    }
}
