#!/usr/bin/env bash
set -euo pipefail

PKG="com.example.pocspeed"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
README_PATH="README.md"
README_MARKER="<!-- AUTO-APK-LINK -->"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Please install Android platform-tools." >&2
  exit 1
fi

./gradlew assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "APK not found at $APK_PATH" >&2
  exit 1
fi

APK_ABS_PATH="$(cd "$(dirname "$APK_PATH")" && pwd)/$(basename "$APK_PATH")"
APK_SIZE_BYTES=$(wc -c < "$APK_PATH" | tr -d ' ')
APK_UPDATED_UTC="$(date -u +"%Y-%m-%d %H:%M:%S UTC")"
AUTO_LINK_LINE="${README_MARKER} [Latest Debug APK](file://${APK_ABS_PATH}) (updated: ${APK_UPDATED_UTC}, size: ${APK_SIZE_BYTES} bytes)"

if [ -f "$README_PATH" ]; then
  if rg -q "^${README_MARKER}" "$README_PATH"; then
    python - "$README_PATH" "$README_MARKER" "$AUTO_LINK_LINE" <<'PY'
import sys
from pathlib import Path
readme = Path(sys.argv[1])
marker = sys.argv[2]
line = sys.argv[3]
content = readme.read_text(encoding='utf-8').splitlines()
for i, row in enumerate(content):
    if row.startswith(marker):
        content[i] = line
        break
readme.write_text("\n".join(content) + "\n", encoding='utf-8')
PY
  else
    {
      echo
      echo "## Download"
      echo "$AUTO_LINK_LINE"
    } >> "$README_PATH"
  fi
  echo "Updated README download link."
fi

set +e
INSTALL_OUTPUT=$(adb install -r "$APK_PATH" 2>&1)
INSTALL_CODE=$?
set -e

echo "$INSTALL_OUTPUT"

if [ $INSTALL_CODE -eq 0 ]; then
  echo "Install/update successful."
  exit 0
fi

if echo "$INSTALL_OUTPUT" | rg -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE|INSTALL_FAILED_VERSION_DOWNGRADE|INSTALL_PARSE_FAILED"; then
  echo "Install failed due to incompatible existing install. Uninstalling $PKG and retrying..."
  adb uninstall "$PKG" || true
  adb install "$APK_PATH"
  echo "Fresh install successful."
  exit 0
fi

echo "Install failed for another reason. See output above." >&2
exit $INSTALL_CODE
