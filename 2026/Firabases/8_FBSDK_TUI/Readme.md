# Firebase C++ SDK TUI Sample

**[FTXUI](https://github.com/ArthurSonzogni/FTXUI)** と **Firebase C++ SDK** を使用した、ターミナルベースのGUIアプリケーションです。
Firestore上のデータをリアルタイム（擬似）にリスト表示し、追加・削除の操作を行えます。

機能:
- **ユーザー一覧表示**: Firestoreの `samples` コレクションからデータを取得して表示。
- **ユーザー追加**: Name / Email を入力して追加。
- **ユーザー削除**: リストの `[Remove]` ボタンで削除。

# 環境

- OS: Linux/wsl

### ライブラリ
```
sudo apt install -y build-essential cmake unzip curl pkg-config libsecret-1-dev libcurl4-openssl-dev libssl-dev zlib1g-dev
```
### SDKのセットアップ
- Firebase C++ SDK (Linux版)
```bash
chmod +x setup_sdk.sh
./setup_sdk.sh
```

# ビルド
```bash
cmake -S . -B build -DFIREBASE_CPP_SDK_DIR="./build/sdk/firebase_cpp_sdk"
cmake --build build -j 4
```

# 実行

```bash
export FB_API_KEY="your-api-key"
./build/FirebaseApp
```

