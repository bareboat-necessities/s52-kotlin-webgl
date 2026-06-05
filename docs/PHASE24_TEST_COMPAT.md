# Phase 24 — readiness-test compatibility fix

Phase 24 fixes readiness-test regressions caused by moving CI and release
workflows forward to the Phase 22 OpenCPN symbology import pipeline.

Older Phase 15–19 readiness tests intentionally assert that historical phase
markers and safety-boundary text remain visible in docs/workflows. Phase 24 keeps
those markers visible while retaining `phase22Check` as the active build.

## Active build

```bash
gradle --no-daemon phase22Check
```

## Compatibility markers preserved

- `phase15Check`
- `phase16Check`
- `phase17Check`
- `phase18Check`
- `phase19Check`
- `phase15SourceArchive`
- `phase16SourceArchive`
- `phase17SourceArchive`

## Safety boundary

Experimental. Not type-approved ECDIS. Not for navigation.
