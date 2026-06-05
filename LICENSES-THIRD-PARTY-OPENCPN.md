# OpenCPN symbology compatibility notice

Phase 22 adds support for importing OpenCPN `chartsymbols.xml` symbology data.

OpenCPN repository metadata shows GPL licensing, and the repository contains
GPL-2.0, GPL-3.0, LGPL-2, and LGPL-3 license texts. The top-level repository
page labels the project with GPL-2.0 and the README states OpenCPN is GPL'ed.
To keep this project compatible with redistributing and deriving work from the
OpenCPN symbology data path, this project license has been changed from MIT to
GPL-2.0-or-later.

This project does not vendor OpenCPN raster atlases in Phase 22. It imports
vector/scalable symbology from a user-supplied `chartsymbols.xml` file.
