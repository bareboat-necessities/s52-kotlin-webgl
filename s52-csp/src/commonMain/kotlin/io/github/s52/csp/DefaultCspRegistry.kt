package io.github.s52.csp

import io.github.s52.core.csp.MapCspRegistry

object DefaultCspRegistry {
    /** compatibility alias. */
    fun phase0(): MapCspRegistry = complete()

    /** Critical CSP set: DEPARE, DEPCNT, SOUNDG, WRECKS, OBSTRN, LIGHTS, TOPMAR. */
    fun critical(): MapCspRegistry = MapCspRegistry(CspId.criticalProcedures())

    /** registry with all CSPs referenced by the synthetic generated Presentation Library pack. */
    fun complete(): MapCspRegistry = MapCspRegistry(CspId.completeProcedures())

    /** Registry with OpenCPN-numbered CSP names referenced by `chartsymbols.xml`. */
    fun openCpn(): MapCspRegistry = MapCspRegistry(
        CspId.completeProcedures() + OpenCpnCspProcedures.procedures()
    )
}
