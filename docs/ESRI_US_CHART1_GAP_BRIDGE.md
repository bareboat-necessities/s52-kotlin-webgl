# ESRI U.S. Chart No. 1 gap bridge

This incremental patch adds a hand-written U.S. Chart No. 1 / ECDIS fallback
bridge for ESRI symbology. Generated `CustomSymbolMap.xml` rules remain the
preferred source. The new fallback rules are appended after generated rules and
only provide broad coverage where generated ESRI direct rules are absent.

Covered groups:

- depths, soundings, depth contours, dredged/unsurveyed areas;
- coastlines, shoreline constructions, land, roads, rivers, lakes and vegetation;
- seabed, underwater rocks, wrecks, obstructions, spoil ground and aquaculture;
- submarine/overhead cables and pipelines, offshore installations;
- anchorages, berths, ports, docks, pontoons and mooring facilities;
- restricted/caution areas, routing measures, TSS, ferries and navigation lines;
- lights, buoys, beacons, daymarks and topmarks;
- landmarks, buildings, tanks, silos, pylons and cranes;
- magnetic/control points, tides/currents, fog/radar/radio/service points.

The intent is to close visual coverage gaps from U.S. Chart No. 1 semantics while
still allowing future generator output or better exact aliases to override these
fallbacks.
