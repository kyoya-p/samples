# FbKmp

## 概要
- Kotlin Multiplatform (Native/JVM) アプリケーション
- FTXUI (C++ TUIフレームワーク) を cinterop を介して Kotlin Native から使用する基盤構成

## 環境構築
このプロジェクトは `mise` を使用してツールチェーンを管理しています。

```bash
# Gradleのインストール
mise install gradle
```

## ビルド・実行手順

### Windows (MingwX64)
Windowsネイティブバイナリをビルドし、実行します。

```powershell
# ビルド
mise exec gradle -- gradle :windows-cli:linkDebugExecutableMingwX64

# 実行
.\windows-cli\build\bin\mingwX64\debugExecutable\windows-cli.exe
```

### Linux (LinuxX64)
WSLまたはLinux環境向けのバイナリをビルドし、実行します。

```bash
# ビルド
mise exec gradle -- gradle :linux-cli:linkDebugExecutableLinuxX64

# 実行 (WSL)
wsl ./linux-cli/build/bin/linuxX64/debugExecutable/linux-cli.kexe
```

## プロジェクト構造
- `shared`: 共通ロジックおよびcinterop定義（`nativeInterop`）
- `windows-cli`: Windows向け実行ファイルモジュール
- `linux-cli`: Linux向け実行ファイルモジュール

## 開発メモ
- cinterop定義は `shared/nativeInterop/cinterop/ftxui.def` にあります。
- ラッパーヘッダーは `shared/nativeInterop/include/ftxui_wrapper.h` です。
- C++の実装を拡張する場合は、上記ヘッダーおよび `.def` ファイルのリンク設定を適宜更新してください。
