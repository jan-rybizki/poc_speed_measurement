#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "Usage: $0 <package_name> <local_model_path>"
  echo "Example: $0 com.example.pocspeed ~/Downloads/yolo11n_float32.tflite"
  exit 1
fi

PACKAGE_NAME="$1"
LOCAL_MODEL_PATH="$2"
TARGET_NAME="yolo11n.tflite"
TARGET_DIR="/data/data/${PACKAGE_NAME}/files/models"

if [[ ! -f "$LOCAL_MODEL_PATH" ]]; then
  echo "Model file not found: $LOCAL_MODEL_PATH"
  exit 1
fi

echo "Checking adb connection..."
adb get-state >/dev/null

echo "Creating model directory in app sandbox..."
adb shell "run-as ${PACKAGE_NAME} mkdir -p files/models"

echo "Pushing model to temporary location..."
adb push "$LOCAL_MODEL_PATH" /data/local/tmp/${TARGET_NAME} >/dev/null

echo "Moving model into app internal storage..."
adb shell "run-as ${PACKAGE_NAME} cp /data/local/tmp/${TARGET_NAME} files/models/${TARGET_NAME}"

adb shell "rm -f /data/local/tmp/${TARGET_NAME}" >/dev/null

echo "Done. Model installed at ${TARGET_DIR}/${TARGET_NAME}"
