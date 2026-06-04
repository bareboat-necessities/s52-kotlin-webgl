# s52-kotlin-webgl

A Kotlin Multiplatform / Kotlin-JS project for an S-52 marine chart portrayal library with an optional WebGL2 rendering backend.

This repository is intentionally **not** a chartplotter. It does not parse S-57, decrypt S-63, manage routes, ingest GPS/NMEA, show AIS, or implement navigation alarms. Its responsibility is:

```text
Typed ENC-like feature model in
        ↓
S-52 lookup / instruction / CSP portrayal
        ↓
Renderer-independent draw commands out
        ↓
Optional WebGL2 renderer
```

## Safety

Experimental. Not type-approved ECDIS. Not for navigation.

## Phase 3 status

Phase 3 is complete in this increment:

- Replaced the minimal instruction parser with a quote-aware S-52 instruction parser
- Kept the compatibility API: `InstructionParser.parseOne` and `parseSequence` still return AST nodes
- Added detailed parsing API with source ranges, raw instruction text, token ranges, and argument ranges
- Added canonical instruction formatting for round-trip/golden tests
- Added `InstructionReferenceCollector` for symbol, line-style, pattern, color-token, and CSP references
- Added typed `TextSpec` for `TX(...)` and `TE(...)` instructions
- Added early validation of malformed input, unsupported instruction kinds, and invalid simple-line widths
- Added parser coverage tests for the generated Phase 2 synthetic Presentation Library pack
- CI now runs `phase3Check`

The project still uses a synthetic Presentation Library pack. Official IHO Presentation Library source assets are not bundled.

## Build

This project is configured for Gradle 8.14.5 and Kotlin 2.3.21.

```bash
gradle phase3Check
```

The CI workflow installs Gradle and Java 21, then runs the same task.

## Modules

```text
s52-catalog       typed S-57 object/attribute catalogue subset; generated later
s52-core          core model, instructions, lookup, CSP interface, portrayal engine
s52-preslib       Presentation Library source model, builder, validation, generator, registries
s52-csp           CSP implementations; early examples/placeholders
s52-render-webgl  JS/WebGL2 renderer for S52DrawCommand
demo              Kotlin/JS browser demo
```

## Legal boundary

Official IHO Presentation Library assets should be treated as external input unless redistribution rights are clear. Phase 2 provides the generator/builder/validation structure so a developer can provide local standards-derived assets and generate runtime tables without committing restricted source material.

## Next step

Begin Phase 4: implement indexed lookup matching, attribute-filter compilation, viewing-group filtering, and final display ordering hardening.
