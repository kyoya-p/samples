JetBrains Amper で管理された、REST API を使用する Firebase Firestore クライアントの実装例を示す Kotlin Multiplatform (KMP) プロジェクトです。

# 概要

このプロジェクトは、JVM および Native (Windows/Linux) ターゲットで動作するクロスプラットフォームの Firestore クライアントを提供します。プラットフォーム固有の Firebase SDK に依存せず、Firestore REST API を使用することで、すべてのプラットフォームでの互換性を確保しています。

ビルドアーティファクトとテストスクリプトの内容から、Compose を使用したターミナルユーザーインターフェース (TUI) アプリケーションとして構成されています。

# 前提条件

- [JetBrains Amper](https://github.com/JetBrains/amper)
- Kotlin 2.x
- Windows Terminal (Windows 固有のテスト実行用)

# ビルド - Windows (Native)
```powershell
amper build :windowsApp
```

# ビルド - JVM
```powershell
amper build :jvmApp
```

# 実行 - Windows (Native)
```powershell
$env:API_KEY = "YOUR_FIREBASE_API_KEY"
./build/tasks/_windowsApp_linkMingwX64Debug/windowsApp.exe
```

# 実行 - JVM
```powershell
$env:API_KEY = "YOUR_FIREBASE_API_KEY"
java -jar ./build/tasks/_jvmApp_jarJvm/jvmApp-jvm.jar
```
