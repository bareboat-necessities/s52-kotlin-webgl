package io.github.s52.core

import io.github.s52.core.instruction.InstructionFormatter
import io.github.s52.core.instruction.InstructionParseException
import io.github.s52.core.instruction.InstructionParser
import io.github.s52.core.instruction.InstructionReferenceCollector
import io.github.s52.core.instruction.S52Instruction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

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

    @Test
    fun detailedParsePreservesSourceRangesAndNormalizes() {
        val sequence = InstructionParser.parseSequenceDetailed(" AC(DEPVS) ; LS(SOLD, 2, CHBLK) ; TX(OBJNAM,'Light, main') ")

        assertEquals(3, sequence.instructions.size)
        assertEquals("AC(DEPVS);LS(SOLD,2,CHBLK);TX(OBJNAM,\"Light, main\")", sequence.normalized())
        assertEquals("AC(DEPVS)", sequence.instructions[0].raw)
        assertEquals("LS", sequence.instructions[1].token.uppercase())
        assertEquals(listOf("OBJNAM", "Light, main"), (sequence.instructions[2].instruction as S52Instruction.Text).rawArgs)
        assertTrue(sequence.instructions[2].arguments[1].wasQuoted)
    }

    @Test
    fun parserIsQuoteAwareForCommasAndSemicolons() {
        val parsed = InstructionParser.parseSequence("TX(OBJNAM,\"comma, semicolon; paren()\");SY(BOYLAT01)")

        assertEquals(2, parsed.size)
        val text = assertIs<S52Instruction.Text>(parsed[0])
        assertEquals("comma, semicolon; paren()", text.rawArgs[1])
        assertEquals(S52Instruction.Symbol("BOYLAT01"), parsed[1])
    }

    @Test
    fun formatterRoundTripsCanonicalInstructions() {
        val source = "SY(BOYLAT01);LS(SOLD,2,CHBLK);LC(COALNE01);AP(APACHR01);CS(DEPARE)"
        val parsed = InstructionParser.parseSequence(source)
        val normalized = InstructionFormatter.formatSequence(parsed)

        assertEquals(source, normalized)
        assertEquals(parsed, InstructionParser.parseSequence(normalized))
    }

    @Test
    fun referenceCollectorFindsPresentationLibraryDependencies() {
        val refs = InstructionReferenceCollector.collect(
            InstructionParser.parseSequence("AC(DEPVS);LS(SOLD,2,CHBLK);LC(COALNE01);AP(APACHR01);SY(BOYLAT01);CS(DEPARE)")
        )

        assertEquals(setOf("BOYLAT01"), refs.symbols)
        assertEquals(setOf("SOLD", "COALNE01"), refs.lineStyles)
        assertEquals(setOf("APACHR01"), refs.patterns)
        assertEquals(setOf("DEPVS", "CHBLK"), refs.colorTokens)
        assertEquals(setOf("DEPARE"), refs.csps)
    }

    @Test
    fun malformedInputReportsSourceRange() {
        val failure = assertFailsWith<InstructionParseException> {
            InstructionParser.parseSequence("AC(DEPVS);XX(BAD)")
        }

        assertTrue(failure.message!!.contains("Unsupported instruction kind"))
        assertEquals("XX", failure.sourceRange!!.slice("AC(DEPVS);XX(BAD)"))
    }

    @Test
    fun invalidLineWidthFailsEarly() {
        assertFailsWith<InstructionParseException> {
            InstructionParser.parseOne("LS(SOLD,wide,CHBLK)")
        }
    }
}
