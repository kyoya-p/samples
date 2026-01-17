# Firebase C++ SDK TUI Sample

**[FTXUI](https://github.com/ArthurSonzogni/FTXUI)** と **Firebase C++ SDK** を使用した、ターミナルベースのGUIアプリケーション
Firestore上のデータをリアルタイムにリスト表示し、追加・削除の操作を行う。。

機能:
- アドレス帳一覧表示**: Firestoreの `addressbook` コレクションからデータを取得して表示。
- アドレス帳追加: Name / Email を入力して追加。Name/Emailはあらかじめランダムな値が入力される(テスト用)
- アドレス帳削除: リストの `[Remove]` ボタンで削除。
- 終了: [Close]ボタンでプログラム終了


# 環境

- OS: Linux/wsl

### ツール/ライブラリ追加
```
sudo apt install -y build-essential cmake unzip curl pkg-config libsecret-1-dev libcurl4-openssl-dev libssl-dev zlib1g-dev
```
### SDKのセットアップ
- Firebase C++ SDK (Linux版)
```bash
sh setup_sdk.sh
```

# ビルド
```bash
```

# 実行

```bash
export FB_API_KEY="your-api-key"
./build/FirebaseApp
```

