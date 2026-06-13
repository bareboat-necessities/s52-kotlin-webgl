# OpenCPN symbology import sample

Use a real OpenCPN `chartsymbols.xml` file to generate SVG images for every imported symbol, line style, pattern, and color.

```bash
gradle --no-daemon Check -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

Generated files are written under:

```text
build/s52-symbology-images/
```
