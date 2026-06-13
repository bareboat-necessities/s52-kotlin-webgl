# S-52 portrayal profiles

`S52ProfileCatalog` contains deterministic public profiles for demos, tests,
regression reports, and downstream integrations that need repeatable display
settings without constructing `MarinerSettings` manually.

Current presets include:

- `safetyDay`
- `planningDay`
- `nightMinimal`
- `diagnosticsAll`

Each `S52PortrayalProfile` can create a default `PortrayalContext`, build an
`S52PortrayalRequest`, and export a markdown/properties summary for diagnostics
or artifact bundles.

Profiles are developer presets only. They are not certified ECDIS settings and
are not for navigation.
