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

# ビルド/実行

Windows (MSVC):
Visual Studio の "Developer Command Prompt for VS 2022" (または x64 Native Tools Command Prompt) を開き、以下のコマンドを実行してください。

```powershell
cmake -S . -B build
cmake --build build --config Release -j 4
$env:API_KEY="your-api-key"
.\build\Release\AddrApp.exe
```

Ubuntu:
```bash
cmake -S . -B build        # [CMakeLixt.txt修正後]Makefile作成
cmake --build build -j 4   # 実行ファイル作成
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

# 評価検証手順 (Windows)

Windows Terminal と PowerShell を使用して、アプリケーションの動作を自動的に検証し、証跡（スクリーンショット）を採取します。

### 1. 事前準備
- **Windows Terminal** がインストールされていること。
- `build/Release/AddrApp.exe` がビルド済みであること。

### 2. 検証の実行
以下のコマンドを実行します。

```powershell
# APIキーの設定
$env:API_KEY = "your-api-key"

# 検証スクリプトの実行（アプリ起動 -> キャプチャ -> 終了）
powershell.exe -ExecutionPolicy Bypass -File run_app_with_screenshot.ps1
```

### 3. 結果の確認
- カレントディレクトリに **`app_run_screenshot.png`** が生成されます。
- 画像を開き、以下の点を確認します：
    - `Status: Connected` と表示され、Firebaseとの通信が成功していること。
    - 連絡先リストが表示されていること。
    - `Name`, `Time`, `Operation` カラムに反転表示（ハイライト）がないこと。

## Windows 自動化の知見 (Tips)

Windows Terminal 内で実行される TUI アプリを PowerShell 等から外部操作する場合の注意点：

- **SendKeys の対象**: `pwsh.exe` はウィンドウハンドルを持たないため、親プロセスの `WindowsTerminal.exe` をタイトル等で特定してキーを送信する必要があります。
- **入力方式**: TUI アプリは標準的なウィンドウメッセージを無視することが多いため、コンソール入力バッファへの書き込みや、PTY (Pseudo Terminal) 経由の操作が推奨されます。

