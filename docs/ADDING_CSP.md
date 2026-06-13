# Adding a conditional symbology procedure

Conditional symbology procedures are the behavior side of S-52 portrayal. They should live in `s52-csp`, not inside object-class enums or renderers.

## 1. Add the CSP implementation

Create a file under `s52-csp/src/commonMain/kotlin/io/github/s52/csp/`:

```kotlin
class MyObjectCsp : ConditionalSymbologyProcedure {
    override val name: String = "MYOBJ"

    override fun evaluate(
        feature: EncFeature,
        settings: MarinerSettings,
        context: PortrayalContext,
        presLib: PresLibPack
    ): List<S52Instruction> {
        // Use typed attributes. Do not read raw strings.
        return listOf(S52Instruction.Symbol("MYOBJ01"))
    }
}
```

## 2. Register the CSP

Add it to `CspId` and the default registry builder:

```kotlin
MYOBJ("MYOBJ", MyObjectCsp())
```

## 3. Reference it from the Presentation Library pack

Lookup rows should call `CS(MYOBJ)` rather than duplicating conditional logic directly.

## 4. Add coverage validation

The static validator must report zero missing CSP references:

```text
Every CS(name) found in lookup rows must exist in CspRegistry.
```

## 5. Add command-level tests

Prefer tests that assert deterministic draw-command transcripts instead of screenshots. Pixel tests are useful later, but command tests are more stable and catch portrayal regressions earlier.

## 6. Keep the renderer clean

A CSP may emit `S52Instruction.Symbol`, `S52Instruction.AreaColor`, `S52Instruction.Text`, or other instructions. It must not call WebGL or produce GPU buffers.
