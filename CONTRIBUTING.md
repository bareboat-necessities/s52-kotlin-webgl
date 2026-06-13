# Contributing

Contributions should preserve the project boundary:

```text
normalized ENC-like feature model in
S-52 portrayal commands out
optional WebGL renderer
```

Do not add S-57 parsing, S-63 decryption, AIS, GPS, route planning, alarms, or certified-ECDIS claims to this repository.

## Development checks

For renderer work, also run the browser demo build through the existing checks.

## Presentation Library assets

Do not commit official IHO Presentation Library source assets unless redistribution rights are clear. Prefer importer/generator code and synthetic fixtures.

## Tests

New behavior should normally include one or more of:

- static completeness validation
- command-level golden transcript tests
- validation fixture tests
- renderer smoke tests

## Style

Keep S-57 object identity separate from S-52 portrayal behavior. Object classes and attributes should remain typed catalogue values; CSPs and instruction evaluators own behavior.
