# Firebase C++ SDK TUI Sample

**[FTXUI](https://github.com/ArthurSonzogni/FTXUI)** と **Firebase C++ SDK** を使用した、ターミナルベースのGUIアプリケーション。
Firestore上のデータをリアルタイムにリスト表示し、追加・削除の操作を行う。

# 参照

- [UI仕様](UI_Specification.md)
- [設計](SoftwareDesign.md)
- [履歴](History.md)

# 環境

- Linux
  - OS: Ubuntu
  - Compiler: g++
  
- Windwos
  - OS: Windows 11
  - Compiler: MSVC 2022

# ツール/ライブラリ追加
```powershell:windows
# Visual Studio 2022 (C++ Desktop Development) が必要です。
# CMakeはVisual Studioに含まれるものを使用します。
```
```shell:ubuntu
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

Windows (MSVC):
Visual Studio の "Developer Command Prompt"を開き、以下のコマンドを実行。

```powershell
cmake -S . -B build        # firebase sdkのダウンロードに時間がかかる。ファイルサイズ確認しながら待つ
cmake --build build --config Release -j 4
```

Ubuntu:
```bash
cmake -S . -B build        # [CMakeLixt.txt修正後]Makefile作成
cmake --build build -j 4   # 実行ファイル作成
```

# 実行

Windows:
```powershell
$env:API_KEY="your-api-key"
.\build\Release\FirebaseApp.exe
```

Ubuntu:
```bash
export API_KEY="your-api-key"
./build/FirebaseApp
```

# ディレクトリ構成
- {Root}
  - src
    - main
    - test
  - build: ビルド用一時ファイル
    - win.msvc: Windows環境
    - linux: linux環境
