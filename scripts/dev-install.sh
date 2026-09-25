#!/usr/bin/env bash
set -euo pipefail

PKG="com.example.pocspeed"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Please install Android platform-tools." >&2
  exit 1
fi

./gradlew assembleDebug

if [ ! -f "$APK_PATH" ]; then
  echo "APK not found at $APK_PATH" >&2
  exit 1
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

if echo "$INSTALL_OUTPUT" | rg -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE|INSTALL_FAILED_VERSION_DOWNGRADE"; then
  echo "Install failed due to incompatible existing install. Uninstalling $PKG and retrying..."
  adb uninstall "$PKG" || true
  adb install "$APK_PATH"
  echo "Fresh install successful."
  exit 0
fi

echo "Install failed for another reason. See output above." >&2
exit $INSTALL_CODE
