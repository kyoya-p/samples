# Firebase Client Sample (C++)

Firebase C++ SDK を使用して Firestore への読み書きを行うサンプルアプリケーション。
**WSL (Linux) 環境での g++ ビルド** を推奨しています。

## 前提条件 (WSL / Linux)

1.  **WSL / Linux**: Ubuntu 24.04 LTS 等
2.  **ビルドツール**: `build-essential` (`g++`, `make`), `cmake`, `unzip`, `curl`, `pkg-config`, `libsecret-1-dev`
3.  **Firebase C++ SDK**: 付属のスクリプトで自動セットアップします。
    - **注意**: SDK のサイズは約 1.2GB あり、展開後は 5GB 以上になります。ディスク容量と通信環境に注意してください。

### ツールのインストール (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install build-essential cmake unzip curl pkg-config libsecret-1-dev libcurl4-openssl-dev libssl-dev zlib1g-dev
```

## セットアップ手順

WSL ターミナルでプロジェクトルートに移動し、セットアップスクリプトを実行してください。

```bash
# 実行権限の付与
chmod +x setup_sdk.sh

# SDK のダウンロードと展開 (時間がかかります)
./setup_sdk.sh
```

これにより `./build/sdk/firebase_cpp_sdk` に SDK が配置されます。
※内部的に `dl.google.com` から統合パッケージをダウンロードします。

## ビルド手順

1.  ビルド設定の生成
    ```bash
    cmake -S . -B build -DFIREBASE_CPP_SDK_DIR="./build/sdk/firebase_cpp_sdk"
    ```

2.  ビルドの実行
    ```bash
    cmake --build build
    ```

## 実行方法

実行には Firebase API Key (`FB_API_KEY`) などの環境変数設定が必要です。

```bash
# 環境変数の設定
export FB_API_KEY="your-api-key"
export FB_PROJECT_ID="riot26-70125"
export FB_APP_ID="your-app-id"

# 実行
./build/FirebaseApp
```

---

## Windows (MSVC) 環境の場合

Visual Studio (MSVC) を使用する場合は、以下の PowerShell スクリプトを使用してください。

1.  セットアップ: `.\setup_sdk.ps1`
2.  ビルド構成: `cmake -S . -B build -DFIREBASE_CPP_SDK_DIR="./build/sdk/firebase_cpp_sdk"`
3.  ビルド: `cmake --build build --config Debug`
4.  実行: `.\build\Debug\FirebaseApp.exe`

## プロジェクト構造

- `src/main.cpp`: メインロジック (Auth, Firestore)
- `setup_sdk.sh`: Linux/WSL 用セットアップスクリプト (Python3/unzip両対応)
- `setup_sdk.ps1`: Windows 用セットアップスクリプト
- `CMakeLists.txt`: CMake ビルド設定
- `_kotlin_backup/`: 以前の Kotlin プロジェクトのバックアップ

## 動作確認済み環境 (2026-01-17)

以下の環境にてビルドおよび Firebase 連携（Auth/Firestore）の動作を確認済みです。

| コンポーネント | バージョン | 備考 |
| :--- | :--- | :--- |
| **OS** | Ubuntu 24.04.3 LTS | WSL2 |
| **Firebase C++ SDK** | 13.3.0 | Linux版 |
| **Compiler** | g++ 13.3.0 | |
| **CMake** | 3.28.3 | |
| **OpenSSL** | 3.0.13 | |

