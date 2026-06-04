package io.github.s52.csp

import io.github.s52.core.csp.MapCspRegistry

object DefaultCspRegistry {
    /** Phase 0 compatibility alias. */
    fun phase0(): MapCspRegistry = phase6Complete()

    /** Critical Phase 5 CSP set: DEPARE, DEPCNT, SOUNDG, WRECKS, OBSTRN, LIGHTS, TOPMAR. */
    fun phase5Critical(): MapCspRegistry = MapCspRegistry(CspId.criticalPhase5Procedures())

    /** Phase 6 registry with all CSPs referenced by the synthetic generated Presentation Library pack. */
    fun phase6Complete(): MapCspRegistry = MapCspRegistry(CspId.completePhase6Procedures())
}
