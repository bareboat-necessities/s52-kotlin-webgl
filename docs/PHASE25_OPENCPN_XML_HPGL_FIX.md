# Phase 25 — OpenCPN XML HPGL importer fix

Phase 25 replaces the failed flat-stream assumption with an XML-first importer
for OpenCPN `chartsymbols.xml`.

OpenCPN parses:

- `color-table` nodes with `color` children
- `line-style` records with `name`, `color-ref`, `HPGL`, and `vector`
- `pattern` records with vector `HPGL`
- `symbol` records where `HPGL` is nested inside the `vector` node

The importer remains vector/scalable only. It does not import or depend on
`rastersymbols-*.png`.
