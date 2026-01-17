# Firebase C++ SDK TUI Sample

**[FTXUI](https://github.com/ArthurSonzogni/FTXUI)** と **Firebase C++ SDK** を使用した、ターミナルベースのGUIアプリケーションです。
Firestore上のデータをリアルタイム（擬似）にリスト表示し、追加・削除の操作を行えます。

## 機能
- **ユーザー一覧表示**: Firestoreの `samples` コレクションからデータを取得して表示。
- **ユーザー追加**: Name / Email を入力して追加。
- **ユーザー削除**: リストの `[Remove]` ボタンで削除。

## 前提条件 (WSL / Linux)

ビルドには以下のツールとライブラリが必要です。

```bash
sudo apt update
sudo apt install build-essential cmake unzip curl pkg-config libsecret-1-dev libcurl4-openssl-dev libssl-dev zlib1g-dev
```

## セットアップとビルド (WSL)

WSLのターミナルで、プロジェクトルート (`8_FBSDK_TUI`) に移動して以下を実行してください。

### 1. SDKのセットアップ
Firebase C++ SDK (Linux版) をダウンロード・展開します。

```bash
chmod +x setup_sdk.sh
./setup_sdk.sh
```

### 2. ビルド
CMakeを使用してビルドします。FTXUIは自動的にダウンロードされます。

```bash
# ビルド設定
cmake -S . -B build -DFIREBASE_CPP_SDK_DIR="./build/sdk/firebase_cpp_sdk"

# コンパイル (4並列)
cmake --build build -j 4
```

## 実行方法

Firebaseプロジェクトの認証情報を環境変数に設定して実行してください。

```bash
# 環境変数の設定 (自身のプロジェクト情報に置き換えてください)
export FB_API_KEY="your-api-key"

# アプリケーションの起動
./build/FirebaseApp
```

## 操作方法
- **Tab / Arrow Keys**: 項目間の移動
- **Enter**: ボタンの決定、入力フォームのフォーカス
- **Ctrl+C**: 終了