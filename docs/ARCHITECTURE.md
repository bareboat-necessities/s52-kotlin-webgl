# Architecture

`s52-kotlin-webgl` is a portrayal library. It receives already-normalized ENC-like features and returns renderer-independent S-52 draw commands. Parsing S-57, decrypting S-63, managing chart catalogues, routes, AIS, GPS, alarms, and storage are outside the repository boundary.

## Layering

```text
Application / chart engine
        │
        │ normalized EncFeature values
        ▼
s52-api
        │ public facade, defaults, validation, caching
        ▼
s52-core
        │ feature model, instructions, lookup matching, draw commands
        ▼
s52-csp + s52-preslib
        │ conditional symbology + Presentation Library pack
        ▼
S52DrawCommand list
        │
        ├── application-owned renderer
        └── s52-render-webgl optional WebGL2 renderer
```

## Module responsibilities

### `s52-catalog`

Typed object and attribute identifiers. Known S-57 acronyms such as `DEPARE`, `SOUNDG`, and `WRECKS` are enums, not free strings. This keeps lookup matching and CSP code type-safe.

### `s52-core`

The core portrayal boundary. It owns:

- `EncFeature` and `EncGeometry`
- `S57Attributes` and `S57Value`
- S-52 instruction parsing
- lookup matching, display-category filtering, viewing-group filtering, and display ordering
- `ConditionalSymbologyProcedure` interface
- renderer-independent `S52DrawCommand`
- deterministic command transcripts
- performance helpers such as request keys, cache, and command batching

It does **not** depend on WebGL, DOM, browser APIs, or official Presentation Library source files.

### `s52-preslib`

Presentation Library source model, builder, generated synthetic pack, generator, and static completeness validation. The checked-in pack is synthetic and exists for tests/examples. Official source assets should be supplied externally by downstream users or private build pipelines.

### `s52-csp`

Conditional symbology procedure implementations. CSPs are behavior and belong here, not inside `S57ObjectClass` enums.

### `s52-api`

Stable public facade. Most applications should start with:

```kotlin
val runtime = S52.defaultRuntime()
val commands = runtime.portray(features, settings, context)
```

Advanced integrations can use `S52Runtime.from(presLib, cspRegistry)` to wire generated/local Presentation Library packs.

### `s52-render-webgl`

Optional JS/WebGL2 renderer. It renders `S52DrawCommand` and must not contain ENC semantics. If a renderer needs to ask what `WRECKS` means, the boundary has been violated.

### `s52-tests`

Reusable command-level golden tests and S-64/Chart-1-style validation fixture parsing.

## Data flow

1. A chart engine parses S-57 or another source into normalized `EncFeature` values.
2. `S52Runtime` passes those features into `S52PortrayalEngine`.
3. `LookupTable` selects candidate lookup records by object class, primitive, attributes, scale, display category, and viewing group.
4. `S52Instruction` values are evaluated. `CS(...)` instructions call the registered CSP.
5. Instructions become `S52DrawCommand` values.
6. Commands are sorted, filtered, validated, serialized, cached, batched, or rendered.

## Design rule

Keep these boundaries strict:

```text
S57ObjectClass: identity and catalogue metadata only
LookupRecord: rule selection
CSP: object-specific conditional behavior
S52Instruction: Presentation Library instruction semantics
S52DrawCommand: renderer-independent output
WebGL: drawing only
```
