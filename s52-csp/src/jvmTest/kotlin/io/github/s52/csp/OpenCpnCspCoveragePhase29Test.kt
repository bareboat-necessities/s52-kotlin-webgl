package io.github.s52.csp

import io.github.s52.core.instruction.InstructionReferenceCollector
import io.github.s52.preslib.PresLibPack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OpenCpnCspCoveragePhase29Test {
    @Test
    fun openCpnRegistryCoversAllGeneratedOpenCpnCspReferences() {
        val pack = PresLibPack.openCpn()
        val referenced = InstructionReferenceCollector
            .collect(pack.lookupTable.records().flatMap { it.instructions })
            .csps
            .mapTo(mutableSetOf()) { it.uppercase() }
        val implemented = DefaultCspRegistry.openCpn().names()

        assertEquals(emptySet(), referenced - implemented)
        assertTrue(referenced.containsAll(OpenCpnCspProcedures.names()))
    }
}
