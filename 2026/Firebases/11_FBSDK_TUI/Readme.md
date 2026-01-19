# Firebase C++ SDK TUI Sample

**[FTXUI](https://github.com/ArthurSonzogni/FTXUI)** と **Firebase C++ SDK** を使用した、ターミナルベースのGUIアプリケーション。
Firestore上のデータをリアルタイムにリスト表示し、追加・削除の操作を行う。

# 参照

- [UI仕様](UI_Specification.md)
- [設計](SoftwareDesign.md)
- [履歴](History.md)

# 環境

- OS: Ubuntu

# ツール/ライブラリ追加

```
sudo apt install -y \
build-essential \
cmake \
unzip \
curl \
pkg-config \
libsecret-1-dev \
libcurl4-openssl-dev \
libssl-dev \
zlib1g-dev
```

# ビルド

```bash
cmake -S . -B build        # [CMakeLixt.txt修正後]Makefile作成
cmake --build build -j 4   # 実行ファイル作成
```
```powershell: wsl
wsl cmake -S . -B build        # [CMakeLixt.txt修正後]Makefile作成
wsl cmake --build build -j 4   # 実行ファイル作成
```

# 実行

```bash
export API_KEY="your-api-key"
./build/FirebaseApp
```

