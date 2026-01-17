# Firebase C++ SDK TUI Sample
**[AI編集禁止]**

**[FTXUI](https://github.com/ArthurSonzogni/FTXUI)** と **Firebase C++ SDK** を使用した、ターミナルベースのGUIアプリケーション。
Firestore上のデータをリアルタイムにリスト表示し、追加・削除の操作を行う。

詳細な仕様については [specification.md](specification.md) を参照のこと。

# 環境

- OS: Linux

### ツール/ライブラリ追加
```
sudo apt install -y build-essential cmake unzip curl pkg-config libsecret-1-dev libcurl4-openssl-dev libssl-dev zlib1g-dev
```
### SDKのセットアップ

# ビルド
```bash
cmake -S . -B build        # [CMakeLixt.txt修正後]Makefile作成
cmake --build build -j 4   # 実行ファイル作成
```

# 実行

```bash
export API_KEY="your-api-key"
./build/FirebaseApp
```

