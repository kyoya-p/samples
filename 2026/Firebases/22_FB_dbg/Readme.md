# Firebase C++ SDK TUI Sample

Firestore上のデータをリアルタイムにリスト表示し、追加・削除の操作を行う。
Testには microsoft/tui-test 使用。

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
# Visual Studio 2022 (C++ Desktop Development) が必要です。
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
Visual Studio の "Developer Command Prompt for VS 2022" (または x64 Native Tools Command Prompt) を開き、以下のコマンドを実行してください。

```powershell
cmake -S . -B build
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
.\build\Release\AddrApp.exe
```

Ubuntu:
```bash
export API_KEY="your-api-key"
./build/AddrApp
```

# テスト

Node.js (v20以上) と `@microsoft/tui-test` を使用。 `mise` を利用した実行手順は以下の通り。

```powershell
# 依存関係のインストール
mise exec node@20 -- npm install

# テスト実行
$API_KEY = "*******"
mise exec node@20 -- npx tui-test  # 全部
mise exec node@20 -- npx tui-test .\testspec\1.1_close.spec.ts  # 個別テスト
```

