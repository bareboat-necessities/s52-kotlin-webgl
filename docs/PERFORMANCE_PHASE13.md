# Phase 13 — Performance pass

Phase 13 adds the first explicit performance layer without changing the public portrayal boundary.

The project still follows the same flow:

```text
EncFeature -> S52PortrayalEngine -> S52DrawCommand -> optional renderer
```

Phase 13 makes that flow easier to reuse in high-frequency repaint loops.

## Completed deliverables

- Added stable content-based `PortrayalRequestKey` for feature/settings/context requests.
- Added `PortrayalCache`, a small deterministic common-source LRU cache for portrayed command lists.
- Added `S52CachedRuntime` as the public cached facade around `S52Runtime`.
- Added `S52.defaultRuntime().cached(...)` and `S52.cachedRuntime(...)` convenience entry points.
- Added `DrawCommandBatcher` to group commands by renderer-stable batch keys.
- Added `DrawBatchReport` and `PortrayalPerformanceReport` for deterministic tests and application diagnostics.
- Extended WebGL render stats with command batch count and average commands per batch.
- Added JVM tests for cache hits, cache eviction, stable request keys, batch grouping, and public API performance reporting.
- Updated CI to run `phase13Check`.

## What is intentionally not done yet

Phase 13 does not claim final high-performance rendering. It establishes measurable performance surfaces:

- repeated portrayal calls can be cached safely;
- command volume and batch volume are observable;
- renderers can later use batch keys for real GPU instancing and atlas grouping;
- applications can decide whether a setting change is a portrayal change or only a renderer repaint.

## Cache boundary

The cache key includes:

- normalized feature content;
- object class and primitive;
- attributes;
- geometry;
- feature scale limits;
- mariner settings;
- portrayal context.

Renderer-only details such as canvas size are not part of the key because they do not affect `S52DrawCommand` output.

## Example

```kotlin
val cached = S52.defaultRuntime().cached(maxEntries = 128)
val commands = cached.portray(features, settings, context)
val report = cached.performanceReport(features, settings, context)

println(report.batchReport.batchCount)
println(cached.cacheStats().hitRate)
```

## Future optimization hooks

The next practical improvements are:

- real WebGL instancing by `DrawBatchKey`;
- symbol atlas packing and reuse;
- pattern atlas packing and reuse;
- text/SDF glyph atlas reuse;
- viewport culling before WebGL upload;
- CSP allocation profiling and targeted pooling;
- optional incremental portrayal for changed features only.
