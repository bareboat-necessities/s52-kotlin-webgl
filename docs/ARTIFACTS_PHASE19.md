# Phase 19 — Portable portrayal artifact bundles

Phase 19 adds a small, dependency-free export layer to the public `s52-api` module.
It packages the output of the existing facade into stable named text artifacts that
can be attached to GitHub issues, uploaded from CI, copied from browser demos, or
stored by downstream chart-engine tests.

The artifact layer is still renderer-independent and still uses the synthetic
Presentation Library fixture bundled with this repository. It does **not** add
S-57 parsing, S-63, GPS, AIS, route management, navigation alarms, or any
ECDIS certification claim.

Experimental. Not type-approved ECDIS. Not for navigation.

## Public API

New public types:

- `S52Artifact`
- `S52ArtifactBundle`
- `S52ArtifactExportOptions`
- `S52ArtifactExporter`

New convenience APIs:

```kotlin
val bundle = S52.synthetic().artifactBundle(
    features = features,
    profile = S52ProfileCatalog.safetyDay,
    name = "issue-123"
)
```

For custom settings:

```kotlin
val request = S52PortrayalRequest(features, settings, context)
val bundle = S52.synthetic().artifactBundle(
    request = request,
    name = "custom-request"
)
```

## Default artifact files

The default export contains text-only artifacts:

```text
bundle-index.md
bundle-index.properties
index.md
manifest.md
diagnostics.md
diagnostics.properties
profile.md
profile.properties
static-completeness.md
command-validation.md
commands.jsonl
commands-preview.txt
```

The exact set depends on `S52ArtifactExportOptions`. The `compact` option keeps
summary diagnostics and a transcript preview, but omits the full transcript and
large validation reports.

## Why this phase exists

Earlier phases produced correct internal objects:

```text
EncFeature -> S52PortrayalEngine -> S52DrawCommand -> transcript
```

Phase 19 makes the same data easy to hand off as named files without forcing
callers to adopt a file-system, zip, JSON, or logging dependency in `commonMain`.

## Boundaries

The artifact exporter does not write files. It returns an in-memory
`S52ArtifactBundle`; browser apps, JVM apps, or CI jobs decide where those files
are saved or uploaded.
