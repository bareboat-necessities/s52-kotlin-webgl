# Browser / WebGL example

The `demo` module is the runnable browser example. It shows the intended boundary:

```text
S52Runtime.portray(...)
        ↓
List<S52DrawCommand>
        ↓
WebGlS52Renderer.render(...)
```

The WebGL renderer consumes commands and a Presentation Library pack. It does not parse S-57 and it does not know what S-57 object classes mean.

Build the demo with:

```bash
gradle :demo:browserDevelopmentWebpack
```

Run the full project gate with:

```bash
gradle phase14Check
```
