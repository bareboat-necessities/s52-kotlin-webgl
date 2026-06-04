# Phase 3: S-52 Instruction Parser

Phase 3 hardens the instruction parser used by the Presentation Library builder.
The parser is still intentionally renderer-independent: it turns S-52 instruction
strings into typed AST nodes and diagnostic metadata, not WebGL calls.

## Supported instruction families

The parser recognizes the S-52 Presentation Library instruction families used by
this project architecture:

- `SY(...)` point symbol
- `LS(...)` simple line
- `LC(...)` complex line
- `AC(...)` area color
- `AP(...)` area pattern
- `TX(...)` text
- `TE(...)` text
- `CS(...)` conditional symbology procedure

## Compatibility API

Existing callers can keep using:

```kotlin
val one = InstructionParser.parseOne("AC(DEPVS)")
val many = InstructionParser.parseSequence("AC(DEPVS);SY(BOYLAT01)")
```

These functions return `S52Instruction` values only.

## Detailed API

Generator, validator, and future golden-test code should use:

```kotlin
val sequence = InstructionParser.parseSequenceDetailed(
    "AC(DEPVS);LS(SOLD,2,CHBLK);TX(OBJNAM,'Main light')"
)

val ast = sequence.ast()
val normalized = sequence.normalized()
val firstRange = sequence.instructions.first().sourceRange
```

Each `ParsedInstruction` stores:

- original full source string
- whole-instruction range
- token range
- argument-list range
- individual argument ranges
- raw argument text
- normalized argument value
- typed AST node
- canonical formatting

## Canonical formatting

`InstructionFormatter` turns typed instructions into stable text:

```kotlin
val normalized = InstructionFormatter.formatSequence(ast)
```

This is meant for command-level golden tests and validation transcripts. It is
not intended to reproduce the official source formatting byte-for-byte.

## Dependency extraction

`InstructionReferenceCollector` scans typed instructions and extracts the
Presentation Library assets referenced by the instruction sequence:

- symbols
- line styles
- patterns
- color tokens
- CSP names

The Phase 2 validator now uses this collector instead of duplicating the logic.

## Current limitations

Phase 3 proves the parser and diagnostic architecture against the synthetic
Presentation Library pack. Official Presentation Library source assets are not
bundled, so full official coverage belongs to the later import/completeness
phases.
