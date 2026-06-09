# Highest-impact improvements plan

Created: 2026-06-09
Baseline branch: `main`
Baseline commit observed through GitHub connector: `f43da47a2586b1561d002c88cd6e9c842ff2db6b`

This plan turns the current Phase 22/OpenCPN work into a reliable baseline for downstream S-57 chartplotter integration. The goal is to make the advertised full check reproducible from a clean checkout, replace placeholder geometry/rendering shortcuts, and add tests that catch the earlier “all symbols are triangles/red circles/empty” regressions.

## Current baseline observations

- `README.md` says the latest full check is `phase22Check` and documents OpenCPN `chartsymbols.xml` as the real symbology source.
- CI and release workflows run `gradle --no-daemon phase22Check` with `OPENCPN_CHARTSYMBOLS_XML_FILE` pointing at `s52/opencpn/chartsymbols.xml`.
- `s52-api:exportOpenCpnSymbologyImages` already has a bundled-file fallback to `s52/opencpn/chartsymbols.xml` when no property/env override is supplied.
- The committed `s52/opencpn/chartsymbols.xml` currently needs validation as a real non-empty XML payload; CI currently only checks file existence before the Gradle run.
- `EncGeometry.Polygon` already models holes, but the WebGL area fill renderer triangulates only the outer ring using a fan from vertex 0.
- Area-pattern tiling also samples only the outer ring for point-in-polygon checks, so holes are not respected.
- Text draw commands currently carry raw `TX`/`TE` expressions; soundings use the raw expression as the displayed depth label.
- Existing exporter tests check useful properties such as SVG presence, color tokens, and non-placeholder bitmap rendering, but they are not yet exact golden transcript or screenshot tests.

## Workstream 1 — make `phase22Check` reliable from a clean checkout

Priority: highest. This is the first gate because all other changes need a trustworthy build/test command.

Implementation steps:

1. Reproduce the failure in a clean checkout-like GitHub Actions job or local CI-equivalent environment:
   - remove `.gradle`, `.kotlin`, `build`, and Kotlin/JS generated stores;
   - run `gradle --no-daemon phase22Check` with no external `OPENCPN_CHARTSYMBOLS_XML_FILE` and no `-Popencpn.chartsymbols`;
   - run the same command with the bundled asset path to expose whether failure is asset-content, lockfile, or task wiring.
2. Fix Kotlin/Yarn lock determinism:
   - regenerate `kotlin-js-store/yarn.lock` using the current Gradle/Kotlin toolchain;
   - commit any Kotlin/JS lock store files that Gradle expects;
   - add a CI step that fails if `phase22Check` mutates the lock state.
3. Validate bundled OpenCPN assets by content, not just existence:
   - check that `s52/opencpn/chartsymbols.xml` is non-empty and parseable;
   - check that imported symbol count is at least `S52SymbologyImageExporter.MinimumRealSymbolCount`;
   - check the three raster atlases are valid PNGs.
4. Align direct API, Gradle task, README, and CI behavior:
   - `gradle --no-daemon phase22Check` should work from a clean checkout if bundled OpenCPN assets are committed;
   - if an external XML is required, README must not imply a clean checkout is enough;
   - prefer bundled asset fallback for the advertised command and external override for developer experiments.

Acceptance criteria:

- `gradle --no-daemon phase22Check` passes from a clean checkout with no local environment variables.
- CI checks that no lockfile changes are produced by the build.
- CI fails before Gradle if bundled assets are empty, malformed, or suspiciously tiny.
- README’s first build command is the exact command CI uses.

## Workstream 2 — replace fan triangulation with real polygon triangulation

Priority: high. This directly affects rendered chart correctness.

Implementation steps:

1. Introduce a renderer-independent triangulation utility, for example `s52-core` or a small common source in `s52-render-webgl`:
   - input: `EncGeometry.Polygon` with outer ring and holes;
   - output: triangle vertices or indexed triangles in projected coordinates;
   - normalization: remove duplicate closing vertex, drop zero-length edges, enforce ring orientation, reject self-intersections with diagnostics.
2. Implement robust triangulation:
   - start with ear clipping for simple concave polygons;
   - add hole bridging by connecting each hole to the outer ring before ear clipping, or use a small vendored earcut-style implementation compatible with Kotlin/JS;
   - use deterministic tie-breaking so golden tests are stable.
3. Replace current fan triangulation in `AreaFillRenderer`.
4. Reuse the same geometry classification for pattern clipping/tiling so holes are respected by both bitmap and vector patterns.
5. Add diagnostics for unsupported/self-intersecting polygons instead of silently drawing wrong triangles.

Acceptance criteria:

- Convex polygon output remains stable.
- Concave polygon fills correctly without leaking outside the polygon.
- Donut polygon leaves the hole empty.
- Multiple holes are supported.
- Area fills and area patterns use consistent inside/outside semantics.

## Workstream 3 — exact golden command tests for representative OpenCPN cases

Priority: high. These tests prevent future “non-empty but wrong” regressions.

Implementation steps:

1. Add a canonical draw-command transcript serializer:
   - stable command ordering;
   - stable numeric formatting;
   - feature id, object class, primitive, lookup source, command kind, symbol/line/pattern/text payload, priority, viewing group, category, over-radar flag.
2. Create fixtures from representative OpenCPN/S-52 cases:
   - point symbol with bitmap fallback and HPGL vector path;
   - line style with repeated pattern;
   - area fill;
   - area pattern;
   - sounding;
   - named object;
   - light with derived text;
   - conditional symbology/CSP case.
3. Store expected transcript files under `s52-tests/src/jvmTest/resources/golden/...`.
4. Add a test helper that can refresh goldens only behind an explicit Gradle property such as `-Ps52.updateGoldens=true`.
5. Fail tests on exact transcript drift.

Acceptance criteria:

- Tests assert exact transcript contents, not just command counts or non-empty categories.
- Golden updates are deliberate and reviewable in diffs.
- Tests catch symbol-name drift, color-token drift, text-expression drift, and command-order drift.

## Workstream 4 — resolve S-52 text expressions into actual labels

Priority: high. This is required for usable charts.

Implementation steps:

1. Add `S52TextResolver` in `s52-core`.
2. Use existing typed attribute APIs instead of raw string parsing where possible.
3. Implement the first resolver table:
   - soundings: resolve numeric depth attributes into formatted depth labels using display settings;
   - object names: resolve `OBJNAM`/national object-name variants;
   - lights: resolve light color, characteristic, period, height/range when available;
   - generic attribute-derived text for common `TX` and `TE` expressions.
4. Keep both resolved and source expression data in draw commands:
   - add `resolvedText` or replace `textExpression` only after a compatibility decision;
   - keep `rawArgs` and `TextSpec` for diagnostics.
5. Add missing-attribute behavior:
   - return no command for empty optional labels;
   - emit deterministic placeholder only in diagnostics/tests, not in production chart output.

Acceptance criteria:

- SOUNDG features render actual depth labels, not the raw `TX`/`TE` expression string.
- Object-name features render object names when text is enabled.
- Light features render a useful light description from attributes.
- Existing `showSoundings` and `showText` settings still gate output.

## Workstream 5 — renderer and golden image tests

Priority: high after triangulation/text changes are underway.

Implementation steps:

1. Add pure JVM geometry tests for triangulation and pattern inside/outside behavior.
2. Add JS/browser renderer smoke tests for WebGL command rendering where CI can support it.
3. Add JVM SVG/exporter golden tests for deterministic renderer-adjacent output:
   - concave polygon;
   - polygon with one hole;
   - polygon with two holes;
   - bitmap pattern clipped to polygon;
   - vector pattern clipped to polygon;
   - representative point symbols.
4. For image tests, use a deterministic software render path first, then browser screenshots later:
   - compare SVG text/golden DOM for exact structure;
   - compare PNG dimensions plus perceptual/hash checks where raster output is involved.
5. Store baselines under `s52-tests/src/jvmTest/resources/render-golden/...`.

Acceptance criteria:

- A concave fill regression fails tests.
- A hole-fill regression fails tests.
- Pattern leakage into holes fails tests.
- Empty symbol or generic triangle fallback regressions fail tests.

## Workstream 6 — Kotlin 2.5 readiness and data-class constructor warnings

Priority: medium-high. Do before the toolchain upgrade becomes blocking.

Implementation steps:

1. Run the current build with warnings visible and collect all Kotlin data-class constructor warnings.
2. Fix data classes with non-property constructor parameters or ambiguous default-property patterns.
3. Prefer explicit companion factories for derived fields instead of constructor side effects.
4. Add a Kotlin compiler warning gate where practical:
   - at minimum document current warnings and fail CI for newly introduced warnings;
   - later move toward `allWarningsAsErrors` once generated/third-party warning noise is controlled.
5. Test with the latest Kotlin 2.4.x/2.5 precheck branch before changing `libs.versions.toml`.

Acceptance criteria:

- Current Kotlin build has no project-owned data-class constructor warnings.
- Kotlin version upgrade dry-run does not introduce blocking constructor issues.
- CI has a repeatable warning visibility path.

## Workstream 7 — README and command alignment

Priority: medium, but include with Workstream 1 because it affects first user experience.

Implementation steps:

1. Put the supported clean-checkout command first:

   ```bash
   gradle --no-daemon phase22Check
   ```

2. Document external OpenCPN XML override as optional:

   ```bash
   gradle --no-daemon phase22Check -Popencpn.chartsymbols=/path/to/chartsymbols.xml
   ```

3. Document generated artifacts and where to open the browser gallery.
4. Document the safety boundary and asset-license boundary in the same section.
5. Remove or clarify stale Phase 21 wording that conflicts with Phase 22 behavior.

Acceptance criteria:

- A new developer can copy the first README command and get the same CI check.
- README, CI, Gradle task descriptions, and docs/OPENCPN_PHASE22.md do not contradict each other.

## Recommended implementation order

1. `phase22Check` clean-checkout reliability and README command alignment.
2. Exact command transcript serializer and first goldens.
3. Polygon triangulation utility and pure geometry tests.
4. Area fill and area pattern renderer integration.
5. Text resolver for soundings, names, lights, and common attribute expressions.
6. Renderer/SVG/image golden tests.
7. Kotlin 2.5 warning cleanup and warning gate.

## Commit strategy

Use small, reviewable commits on `main` or short-lived branches:

1. `fix: make phase22 check reproducible from clean checkout`
2. `test: add exact portrayal transcript goldens`
3. `feat: add deterministic polygon triangulation with holes`
4. `fix: respect polygon holes in area fills and patterns`
5. `feat: resolve s52 text expressions into labels`
6. `test: add renderer goldens for polygons patterns and symbols`
7. `chore: clean kotlin constructor warnings and update docs`

Each implementation commit should include tests that fail on the current behavior and pass after the change.