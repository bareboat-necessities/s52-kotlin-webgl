package io.github.s52.csp

import io.github.s52.core.csp.MapCspRegistry

object DefaultCspRegistry {
    /** Phase 0 compatibility alias. */
    fun phase0(): MapCspRegistry = phase5Critical()

    /** Critical Phase 5 CSP set: DEPARE, DEPCNT, SOUNDG, WRECKS, OBSTRN, LIGHTS, TOPMAR. */
    fun phase5Critical(): MapCspRegistry = MapCspRegistry(CspId.criticalPhase5Procedures())
}
