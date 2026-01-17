#!/bin/bash
set -e

# 色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== Firebase C++ SDK TUI Sample : WSL Build Script ===${NC}"

# 1. 依存関係の確認
echo -e "\n${GREEN}[1/4] Checking dependencies...${NC}"
MISSING_DEPS=0
for pkg in build-essential cmake unzip curl pkg-config; do
    if ! command -v $pkg >/dev/null 2>&1; then
        echo -e "${YELLOW}Missing command: $pkg${NC}"
        MISSING_DEPS=1
    fi
done

# ライブラリのチェック (pkg-configが使える場合)
if command -v pkg-config >/dev/null 2>&1; then
    for lib in libcurl libssl zlib libsecret-1; do
        if ! pkg-config --exists $lib; then
             echo -e "${YELLOW}Missing library: $lib${NC}"
             MISSING_DEPS=1
        fi
    done
fi

if [ $MISSING_DEPS -eq 1 ]; then
    echo -e "${RED}Some dependencies seem to be missing.${NC}"
    echo "Please run the following command to install them:"
    echo -e "${YELLOW}sudo apt update && sudo apt install -y build-essential cmake unzip curl pkg-config libsecret-1-dev libcurl4-openssl-dev libssl-dev zlib1g-dev${NC}"
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "Dependencies look good."
fi

# 2. SDKセットアップ
echo -e "\n${GREEN}[2/4] Setting up Firebase SDK...${NC}"
if [ -d "./build/sdk/firebase_cpp_sdk" ]; then
    echo "SDK already exists. Skipping download."
else
    chmod +x setup_sdk.sh
    ./setup_sdk.sh
fi

# 3. ビルド
echo -e "\n${GREEN}[3/4] Building project...${NC}"
mkdir -p build
# キャッシュを活かすためクリーンはしない
cmake -S . -B build -DFIREBASE_CPP_SDK_DIR="./build/sdk/firebase_cpp_sdk"
cmake --build build -j $(nproc)

echo -e "\n${GREEN}[4/4] Build complete!${NC}"

# 4. 実行確認
if [ -z "$FB_API_KEY" ]; then
    echo -e "${RED}Environment variable FB_API_KEY is not set.${NC}"
    echo "Please export your Firebase credentials and run the app manually:"
    echo ""
    echo "  export FB_API_KEY=\"your-api-key\""
    echo "  export FB_PROJECT_ID=\"riot26-70125\""
    echo "  export FB_APP_ID=\"your-app-id\""
    echo "  ./build/FirebaseApp"
    echo ""
else
    echo -e "${GREEN}Starting application...${NC}"
    ./build/FirebaseApp
fi
