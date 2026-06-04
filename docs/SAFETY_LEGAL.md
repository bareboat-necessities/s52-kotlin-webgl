# Safety and legal boundary

## Not for navigation

This project is experimental. It is not a type-approved ECDIS. It must not be used as the sole basis for navigation, collision avoidance, route monitoring, safety alarms, or voyage planning.

The repository provides software architecture, typed portrayal models, synthetic tests, and an optional WebGL renderer. It does not provide certification, chart correctness guarantees, official Presentation Library assets, hydrographic update management, or mariner safety workflows.

## Official Presentation Library assets

The default pack in this repository is synthetic. It is only intended for tests and examples.

Do not commit official IHO Presentation Library source assets, official symbol libraries, or restricted standards-derived data unless you have confirmed redistribution rights. The project is designed so those assets can be supplied by a local/private generator step instead.

Recommended downstream pattern:

```text
local official/source assets
        ↓
private generator step
        ↓
generated PresLibPack artifact
        ↓
application runtime
```

## Certification wording

Use language such as:

- S-52-style portrayal engine
- ENC portrayal library
- experimental marine chart renderer
- command-level S-52 validation harness

Avoid language such as:

- certified ECDIS
- approved for navigation
- compliant ECDIS replacement
- official IHO Presentation Library bundle

## Runtime responsibility

Applications using this project remain responsible for:

- parsing S-57/S-101 or other source data correctly
- applying chart updates
- managing scale, compilation scale, and source metadata
- validating official portrayal data if they use it
- handling mariner settings correctly
- issuing their own safety disclaimers
- complying with applicable chart-data and standards licenses
