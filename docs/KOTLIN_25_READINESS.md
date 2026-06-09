# Kotlin 2.5 readiness notes

This project targets Kotlin Multiplatform and Kotlin/JS, so warnings that become errors in future Kotlin releases need to be removed before the toolchain upgrade.

## Data-class constructor warning cleanup

The catalog key types used private constructors with `data class`:

- `S57AttributeKey`
- `S57ObjectClassKey`

That pattern is intentionally avoided now. A data class with a private constructor still generates public data-class helpers such as `copy(...)`, which is the constructor/copy-visibility migration warning path Kotlin has been tightening. The key types are now regular classes with explicit `equals`, `hashCode`, and `toString` implementations.

## Current rule

Do not introduce project-owned `data class ... private constructor(...)` key/value types. Use one of these instead:

- a regular class with explicit equality when construction must be controlled by a companion factory;
- a public data class when `copy(...)` is intentionally part of the API;
- a value class when the type has a single canonical value and no derived standard enum field.

## Verification path

Run the normal full check with warnings visible:

```bash
gradle --no-daemon criticalCheck
```

Before any Kotlin upgrade, also grep for the pattern:

```bash
grep -R "data class .*private constructor" -n . --include='*.kt'
```

The expected result for project-owned Kotlin sources is no matches.
