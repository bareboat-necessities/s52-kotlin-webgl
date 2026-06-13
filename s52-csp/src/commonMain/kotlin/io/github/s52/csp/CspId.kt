package io.github.s52.csp

import io.github.s52.core.csp.ConditionalSymbologyProcedure

/**
 * Phase 6 CSP identifiers and their starter implementations.
 *
 * The enum stores behavior for S-52 procedure identifiers, not for S-57 object
 * classes. This keeps S-57 catalogues data-only while allowing complete static
 * coverage checks for imported lookup tables.
 */
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
    TOPMAR("TOPMAR", TopmarkCsp()),

    ACHARE("ACHARE", GenericCautionAreaCsp("ACHARE", "APACHR01")),
    RESARE("RESARE", RestrictedAreaCsp()),
    PRCARE("PRCARE", GenericCautionAreaCsp("PRCARE", "CAUTION01")),
    TESARE("TESARE", GenericCautionAreaCsp("TESARE", "CAUTION01")),
    FAIRWY("FAIRWY", GenericCautionAreaCsp("FAIRWY", "FAIRWY01")),
    DRGARE("DRGARE", DredgedAreaCsp()),
    SBDARE("SBDARE", SeabedAreaCsp()),
    M_QUAL("M_QUAL", QualityOfDataCsp()),
    DATCVR("DATCVR", DataCoverageCsp());

    companion object {
        private val critical = setOf("DEPARE", "DEPCNT", "SOUNDG", "WRECKS", "OBSTRN", "LIGHTS", "TOPMAR")

        fun criticalProcedures(): List<ConditionalSymbologyProcedure> =
            entries.filter { it.s52Name in critical }.map { it.procedure }

        fun criticalNames(): Set<String> = critical

        fun completeProcedures(): List<ConditionalSymbologyProcedure> = entries.map { it.procedure }

        fun completeNames(): Set<String> = entries.mapTo(mutableSetOf()) { it.s52Name }
    }
}
