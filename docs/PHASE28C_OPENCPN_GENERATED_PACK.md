# Phase 28C — Generated OpenCPN source pack

Phase 28C exposes the corrected `s52/opencpn` portrayal payload as a generated
Kotlin `commonMain` source pack.

## Added

- `OpenCpnGeneratedPresLib.sourcePack()`
- `OpenCpnGeneratedPresLib.pack()`
- `PresLibPack.openCpn()`
- `S52Runtime.openCpn()`
- `S52.openCpnRuntime()`
- `S52.openCpn()`
- Gradle task `:s52-preslib:generateOpenCpnPresLib`

## Preserved data

The generated source pack keeps the OpenCPN asset inventory in Kotlin/JS usable
form:

- 3,057 lookup records
- 1,093 symbols
- 57 line styles
- 30 patterns
- 5 color tables with 63 colors each
- raster atlas coordinates for bitmap assets
- raw HPGL strings for vector assets
- OpenCPN lookup table name, display-priority label, radar-priority label, and raw `attrib-code` strings

## Non-goals

Phase 28C does not switch the browser demo to OpenCPN mode by default and does
not yet implement full OpenCPN lookup filtering or WebGL rendering. Those remain
Phase 29 and Phase 30 work.
