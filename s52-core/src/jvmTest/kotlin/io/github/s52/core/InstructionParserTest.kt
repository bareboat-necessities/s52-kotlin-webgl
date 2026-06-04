package io.github.s52.core

import io.github.s52.core.instruction.InstructionParser
import io.github.s52.core.instruction.S52Instruction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class InstructionParserTest {
    @Test
    fun parsesBasicInstructionSequence() {
        val parsed = InstructionParser.parseSequence("AC(DEPVS);LS(SOLD,2,CHBLK);SY(BOYLAT01)")

        assertEquals(3, parsed.size)
        assertEquals(S52Instruction.AreaColor("DEPVS"), parsed[0])
        assertIs<S52Instruction.SimpleLine>(parsed[1])
        assertEquals(S52Instruction.Symbol("BOYLAT01"), parsed[2])
    }

    @Test
    fun parsesConditionalInstruction() {
        val parsed = InstructionParser.parseOne("CS(DEPARE)")
        assertEquals(S52Instruction.Conditional("DEPARE"), parsed)
    }
}
