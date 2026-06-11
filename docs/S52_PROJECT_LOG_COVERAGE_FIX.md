# S-52 project-log coverage fix

This incremental patch addresses the high-volume warnings seen when the S-57
project routes NOAA cell `US5NYCDF` features into the OpenCPN-backed S-52 pack.

## Catalogue widening

The typed S-57 catalogue now includes the object classes that appeared as
`adapter s52.unsupported_object_class` in the project log:

- `ACHBRT`, `BUAARE`, `CBLARE`, `CTNARE`, `DRYDOC`, `HRBFAC`, `LNDRGN`,
  `PIPARE`, `SLOTOP`, and `UNSARE`.

Primitive support was widened for feature classes that OpenCPN lookup rows or
the project log use with point/line/area variants, including `SBDARE`, `SLCONS`,
`MAGVAR`, `BUISGL`, and `LNDARE`.

## Attribute widening

The S-57 attribute catalogue now recognizes the attributes that appeared as
`adapter s52.unsupported_attribute`:

- `CATAIR`, `CATSEA`, `CATSPM`, `CATSIL`, `CATSLC`, `NATSUR`, and `TRAFIC`.

This lets downstream adapters preserve those values so OpenCPN attrib-code
filters can match the generated lookup table.

## Runtime atlas loading

The OpenCPN raster atlases are embedded into generated Kotlin as data URIs.
Host applications no longer need to serve `s52/opencpn/rastersymbols-*.png` at
runtime. The source PNG files remain in the repository as build-time generator
inputs, and `:s52-preslib:generateOpenCpnRasterAtlasData` refreshes the generated
Kotlin when they change.

The WebGL texture upload path no longer calls `gl.asDynamic().texImage2D(...)`.
It uses a small JS bridge function to call the browser `texImage2D` overload for
`HTMLImageElement`, avoiding the observed `P.asDynamic is not a function` runtime
error in bundled consumers.

## What remains outside this library

The log also reports `WebGL2 is not available in this browser` from the S-57
snapshot runner. This patch fixes S-52 portrayal/catalogue/resource issues, but
that specific failure is emitted by the consuming S-57 application or its CI
browser setup. The S-52 WebGL renderer itself uses the WebGL 1
`WebGLRenderingContext` API.
