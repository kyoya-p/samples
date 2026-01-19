#!/bin/bash
set -e

SDK_VERSION="13.3.0"
SDK_FILENAME="firebase_cpp_sdk_${SDK_VERSION}.zip"
SDK_URL="https://dl.google.com/firebase/sdk/cpp/${SDK_FILENAME}"

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${BASE_DIR}/build"
SDK_DIR="${BUILD_DIR}/sdk"
ZIP_PATH="${BUILD_DIR}/${SDK_FILENAME}"

mkdir -p "${SDK_DIR}"
curl -L -o "${ZIP_PATH}" "${SDK_URL}"
unzip -o "${ZIP_PATH}" -d "${SDK_DIR}" > /dev/null
rm "${ZIP_PATH}"
