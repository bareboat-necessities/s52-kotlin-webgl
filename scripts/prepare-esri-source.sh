#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

TARGET="${ESRI_NAUTICAL_CHART_SYMBOLS_DIR:-s52/esri/source}"
REQUIRED="$TARGET/CustomPresentationLibrary/CustomSymbolMap.xml"

if [[ -f "$REQUIRED" ]]; then
  echo "ESRI nautical-chart-symbols source already present at $TARGET"
  exit 0
fi

if [[ -e "$TARGET" ]]; then
  if [[ -n "$(find "$TARGET" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null || true)" ]]; then
    echo "ESRI source directory exists but is incomplete: $TARGET" >&2
    echo "Missing: $REQUIRED" >&2
    echo "Remove it or set ESRI_NAUTICAL_CHART_SYMBOLS_DIR to a complete nautical-chart-symbols checkout." >&2
    exit 1
  fi
  rm -rf "$TARGET"
fi

mkdir -p "$(dirname "$TARGET")"
echo "Fetching ESRI nautical-chart-symbols into $TARGET"
git clone --depth 1 https://github.com/Esri/nautical-chart-symbols.git "$TARGET"

test -f "$REQUIRED" || {
  echo "ESRI nautical-chart-symbols checkout is incomplete after clone. Missing: $REQUIRED" >&2
  exit 1
}
