# OpenCPN symbology import

The OpenCPN-compatible import path consumes `chartsymbols.xml` plus the bundled
raster atlases under `s52/opencpn/` and generates text/SVG/PNG artifacts for
repeatable CI validation.

The clean-check script validates the bundled inputs and the committed Kotlin/JS
lock file before running Gradle:

```bash
bash scripts/critical-clean-check.sh
```

To test with a different OpenCPN `chartsymbols.xml`, use either:

```bash
gradle --no-daemon criticalCheck -Popencpn.chartsymbols=/path/to/chartsymbols.xml
```

or:

```bash
export OPENCPN_CHARTSYMBOLS_XML_FILE=/path/to/chartsymbols.xml
gradle --no-daemon criticalCheck
```

The imported symbology path requires GPL-2.0-or-later compatibility. Generated
outputs are for development/regression inspection only, not for navigation.
