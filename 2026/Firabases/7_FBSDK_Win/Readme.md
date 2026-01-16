# Firebase Client Sample (C++)

Firebase C++ SDK を使用して Firestore への読み書きを行うサンプルアプリケーション。
**WSL (Linux) 環境での g++ ビルド** を推奨しています。

## 前提条件 (WSL / Linux)

1.  **WSL / Linux**: Ubuntu 20.04+ 推奨
2.  **ビルドツール**: `build-essential` (`g++`, `make`), `cmake`, `unzip`, `curl`
3.  **Firebase C++ SDK (Linux)**: 付属のスクリプトで自動セットアップします。

### ツールのインストール (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install build-essential cmake unzip curl
```

## セットアップ手順

WSL ターミナルでプロジェクトルートに移動し、セットアップスクリプトを実行してください。

```bash
# 実行権限の付与
chmod +x setup_sdk.sh

# SDK のダウンロードと展開
./setup_sdk.sh
```

これにより `./build/sdk/firebase_cpp_sdk` に Linux 版 SDK が配置されます。

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
- `setup_sdk.sh`: Linux/WSL 用セットアップスクリプト
- `setup_sdk.ps1`: Windows 用セットアップスクリプト
- `CMakeLists.txt`: CMake ビルド設定
- `_kotlin_backup/`: 以前の Kotlin プロジェクトのバックアップ