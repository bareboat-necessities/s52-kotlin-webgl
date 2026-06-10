# ESRI atlas XML / browser preview fix

This incremental fix corrects the ESRI OpenCPN-matched atlas SVG export.

## Problem

The previous ESRI atlas exporter copied the matched ESRI source SVG and prepended
an OpenCPN/ESRI metadata XML comment before the source bytes. Many ESRI SVG files
start with an XML declaration such as:

```xml
<?xml version="1.0" encoding="UTF-8"?>
```

An XML declaration is only legal at the very beginning of an XML document. Putting
our metadata comment before it made otherwise valid ESRI SVGs malformed. Browsers
then refused to display them from `index.html`, so the generated preview appeared
empty even though the files existed.

## Fix

`EsriSymbologyImageExportMain` now writes copied ESRI SVGs through a small
normalization step:

- removes BOM from the copied source;
- strips source XML declarations before adding exporter metadata;
- sanitizes metadata comments so they cannot contain illegal `--` sequences;
- writes the original `<svg>` payload untouched after the sanitized metadata;
- uses URL-safe preview paths in `index.html`;
- marks failed previews visibly with a red card instead of silently looking blank.

The exported files remain SVG source artifacts, but are now standalone browser-loadable
XML documents.

## Regression coverage

`EsriSymbologyImageExportTest.copiedEsriSvgStripsXmlDeclarationBeforeMetadataComment`
checks that a copied ESRI SVG beginning with BOM + XML declaration is exported
without an illegal XML declaration position and without illegal comment content.
