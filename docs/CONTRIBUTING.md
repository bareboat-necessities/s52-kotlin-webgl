# Contributing

## Development workflow

Run the full current phase check before submitting changes:

```bash
gradle phase14Check
```

For focused work, run the module test first, then the phase check:

```bash
gradle :s52-core:jvmTest
gradle :s52-csp:jvmTest
gradle :s52-preslib:jvmTest
gradle :s52-tests:jvmTest
```

## Project rules

- Do not add S-57 parsing to this repository.
- Do not add navigation, AIS, GPS, alarm, or route-management logic.
- Do not put draw functions inside `S57ObjectClass`.
- Do not put ENC semantics in the WebGL renderer.
- Do not commit official Presentation Library source assets unless redistribution rights are clear.
- Add command-level tests before pixel-level tests.

## Adding catalogue entries

Catalogue enums should remain generated-style. Keep acronym, numeric code, primitive compatibility, and attribute value kind metadata together. If a future full generator replaces the checked-in subset, the public typed names should remain stable where possible.

## Adding lookup rows

Lookup rows should enter through `s52-preslib` source/generator models rather than being scattered through CSP code. A lookup row should describe when instructions apply. A CSP should describe conditional behavior invoked by `CS(...)`.

## Adding CSP behavior

See [`ADDING_CSP.md`](ADDING_CSP.md). Every CSP addition should include:

- typed attribute access
- synthetic fixture coverage
- command-level golden or validation coverage
- static completeness validation

## Renderer changes

Renderer changes should accept only `S52DrawCommand`, `PresLibPack`, palette/settings/context values, and viewport state. A renderer must not branch on `S57ObjectClass`.

## Documentation changes

When adding a public concept, update the relevant phase document and link it from [`PHASES.md`](PHASES.md) or this contributor guide.
