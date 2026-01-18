# Firebase C++ SDK Sample
**[AI編集禁止]**

**Firebase C++ SDK** を使用した、Firestore上のデータを参照・操作するコマンドライン(CLI)アプリケーション。
Windows (MSVC) および Linux (g++) 環境で動作可能。

機能:
- **一覧表示**: `addressbook` コレクションのデータを取得して表示。
- **データ追加**: 名前(ID) と メールアドレスを指定して追加（サーバータイムスタンプ付与）。
- **データ削除**: 指定したIDのドキュメントを削除。

# 環境

- OS: Windows 10/11, Linux (Ubuntu等)

### ツール/ライブラリ
- **Windows**: Visual Studio 2022 Build Tools (C++ Workload), CMake
- **Linux**: g++, CMake, libcurl, openssl, zlib, uuid

#### Windows でのツールインストール例:
```powershell
# CMakeのインストール
winget install --id Kitware.CMake
# MSVC Build Toolsのインストール (C++ワークロードとSDKを含む)
winget install --id Microsoft.VisualStudio.2022.BuildTools --override "--passive --config https://aka.ms --add Microsoft.VisualStudio.Workload.VCTools --add Microsoft.VisualStudio.Component.Windows11SDK.22621 --includeRecommended"
```

# ビルド

- Windows向け: `build/msvc`
- Linux向け: `build/linux`

```bash
cmake -S . -B build/msvc
cmake --build build/msvc --config Release
```
※ 初回ビルド時は Firebase SDK (約600MB) のダウンロードが行われるため、ネットワーク環境により時間がかかります。

# 実行方法

API Key は環境変数 `API_KEY` または引数 `--api_key` で指定します。

### Windows (コマンドプロンプト / Releaseビルド)
```cmd
.\build\Release\FBTest.exe --api_key "your-api-key" --list
```

### 各種操作例
*   **一覧取得**:
    `FBTest.exe --api_key "..." --list <nItem>`
*   **データ追加**:
    `FBTest.exe --api_key "..." --add "User Name" "user@example.com"`
*   **データ削除**:
    `FBTest.exe --api_key "..." --remove "User Name"`

### 環境変数を使用する場合 (Windows)
```cmd
set API_KEY=your-api-key
.\build\msvc\Release\FBTest.exe --list
```