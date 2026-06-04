package io.github.s52.preslib

import io.github.s52.core.instruction.InstructionFormatter
import io.github.s52.core.instruction.InstructionParser
import io.github.s52.core.instruction.InstructionReferenceCollector
import io.github.s52.preslib.generated.GeneratedPhase2PresLib
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstructionCoverageTest {
    @Test
    fun everyGeneratedSourceLookupInstructionParsesWithDetailedMetadata() {
        val source = GeneratedPhase2PresLib.sourcePack()
        val sequences = source.lookupRecords.map { record -> InstructionParser.parseSequenceDetailed(record.instruction) }

        assertEquals(source.lookupRecords.size, sequences.size)
        assertTrue(sequences.all { it.instructions.isNotEmpty() })
        sequences.forEach { sequence ->
            val reparsed = InstructionParser.parseSequence(sequence.normalized())
            assertEquals(sequence.ast(), reparsed)
        }
    }

    @Test
    fun generatedPackReferencesAreCollectedFromTypedInstructions() {
        val pack = GeneratedPhase2PresLib.pack()
        val instructions = pack.lookupTable.records().flatMap { it.instructions }
        val refs = InstructionReferenceCollector.collect(instructions)

        assertEquals(setOf("BOYLAT01", "BOYCAR01"), refs.symbols)
        assertEquals(setOf("COALNE01"), refs.lineStyles)
        assertEquals(emptySet(), refs.patterns)
        assertTrue(setOf("LANDA").all { it in refs.colorTokens })
        assertEquals(
            setOf(
                "ACHARE", "DATCVR", "DEPCNT", "DEPARE", "DRGARE", "FAIRWY", "LIGHTS",
                "M_QUAL", "OBSTRN", "PRCARE", "RESARE", "SBDARE", "SOUNDG", "TESARE",
                "TOPMAR", "WRECKS"
            ),
            refs.csps
        )
        assertEquals(
            instructions,
            InstructionParser.parseSequence(InstructionFormatter.formatSequence(instructions))
        )
    }
}
