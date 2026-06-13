# Minimal core integration sample

This sample shows the intended downstream boundary for a Kotlin/JS chart engine or chartplotter that wants to use this repository only for S-52 portrayal.

The downstream application is expected to parse S-57/S-101/S-57-like data itself and normalize it into `EncFeature` values. This library then evaluates S-52 lookup rows, conditional symbology procedures, mariner settings, and display ordering.


The sample uses `PresLibPack.synthetic()` because official Presentation Library assets are not bundled in this repository. A production integration should provide a generated `PresLibPack` from locally supplied standards-derived assets when redistribution rights are clear.

## Safety

This project is experimental and not for navigation. Do not use it as a certified ECDIS component.
