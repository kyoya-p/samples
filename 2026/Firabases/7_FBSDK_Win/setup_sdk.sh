#!/bin/bash
set -e

SDK_VERSION="13.3.0"
SDK_FILENAME="firebase_cpp_sdk_${SDK_VERSION}_linux.zip"
SDK_URL="https://github.com/firebase/firebase-cpp-sdk/releases/download/v${SDK_VERSION}/${SDK_FILENAME}"

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${BASE_DIR}/build"
SDK_DIR="${BUILD_DIR}/sdk"
ZIP_PATH="${BUILD_DIR}/${SDK_FILENAME}"

# 1. ディレクトリ作成
mkdir -p "${SDK_DIR}"

# 2. ダウンロード
echo "Downloading Firebase C++ SDK v${SDK_VERSION} for Linux..."
echo "URL: ${SDK_URL}"

if command -v curl >/dev/null 2>&1; then
    curl -L -o "${ZIP_PATH}" "${SDK_URL}"
elif command -v wget >/dev/null 2>&1; then
    wget -O "${ZIP_PATH}" "${SDK_URL}"
else
    echo "Error: Neither curl nor wget found."
    exit 1
fi

# 3. 展開
echo "Extracting to ${SDK_DIR}..."
unzip -o "${ZIP_PATH}" -d "${SDK_DIR}" > /dev/null

# 4. クリーンアップ
echo "Cleaning up zip file..."
rm "${ZIP_PATH}"

# 5. 完了案内
EXTRACTED_ROOT="${SDK_DIR}/firebase_cpp_sdk"

if [ -d "${EXTRACTED_ROOT}" ]; then
    echo ""
    echo "[SUCCESS] SDK setup complete."
    echo "SDK Location: ${EXTRACTED_ROOT}"
    echo ""
    echo "You can now build the project using:"
    echo "cmake -S . -B build -DFIREBASE_CPP_SDK_DIR=\"${EXTRACTED_ROOT}\""
    echo "cmake --build build"
else
    echo "Warning: Expected folder '${EXTRACTED_ROOT}' not found."
fi
