# Phase 15 — Release readiness and handoff

Status: **complete**.

Phase 15 turns the Phase 11 technical prototype into a repository that can be handed off, tagged, archived, and consumed by downstream projects without changing the core architecture.

This phase does **not** claim ECDIS certification and does **not** add official IHO Presentation Library assets. The project remains an experimental S-52 portrayal library with a synthetic Presentation Library fixture and an external-assets boundary.

## Goals

- Make the repository self-auditing before release.
- Add a tag-based release workflow.
- Add a source-archive Gradle task.
- Document release safety, legal, and handoff boundaries.
- Keep the public technical boundary unchanged:

```text
EncFeature -> S52PortrayalEngine -> S52DrawCommand -> optional WebGL renderer
```

## New Gradle tasks

```bash
gradle phase15Check
```

Runs all previous checks through `phase11Check`, then verifies Phase 15 repository release-readiness requirements.

```bash
gradle phase15ReleaseAudit
```

Checks that required release/handoff documents and workflows exist and that the README still contains the safety boundary.

```bash
gradle phase15SourceArchive
```

Creates a source archive under `build/distributions/` while excluding build outputs and local Gradle state.

## Release workflow

A new GitHub Actions workflow lives at:

```text
.github/workflows/release.yml
```

It runs on tags matching:

```text
v*
```

The workflow:

1. Checks out the repository.
2. Sets up JDK 21.
3. Sets up Gradle caching.
4. Runs `phase15Check`.
5. Runs `phase15SourceArchive`.
6. Uploads the archive as a workflow artifact.

It intentionally does not publish to Maven Central or npm yet. That should wait until the official external Presentation Library import path and public API policy are finalized.

## Handoff checklist

Before creating a public tag:

- Confirm `README.md` still says the project is experimental and not for navigation.
- Confirm no official restricted Presentation Library source assets were committed.
- Run `gradle phase15Check` locally or in CI.
- Review `docs/PHASES.md` for current phase status.
- Review `docs/S64_VALIDATION_PHASE11.md` for validation limitations.
- Review `docs/STATIC_COMPLETENESS_PHASE9.md` for synthetic pack completeness coverage.
- Review `samples/integration/minimal-core/README.md` for downstream integration shape.

## What Phase 15 intentionally does not do

- It does not implement missing Phases 12–14.
- It does not certify the renderer or portrayal engine.
- It does not bundle official IHO assets.
- It does not publish packages to external registries.
- It does not make browser rendering pixel-perfect against a certified ECDIS.

## Definition of done

- `phase15Check` exists and is wired into CI.
- `phase15ReleaseAudit` exists and checks required release files.
- `phase15SourceArchive` exists.
- Tag-based `release.yml` exists.
- Release/handoff docs exist.
- A minimal downstream integration sample exists.
- Safety and legal boundaries remain explicit.
