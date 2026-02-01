# 概要

Kotlin Multiplatform Compose で[Mosaic](https://github.com/JakeWharton/mosaic)を使用するデモ。
buildtoolはAmper (v0.5.0)。

## プロジェクト構成

- **shared**: 共通ロジック (Mosaic TUIの実装)
- **jvmApp**: JVM用アプリケーション
- **windowsApp**: Windows Native用アプリケーション
- **linuxApp**: Linux Native用アプリケーション

## 実行方法

### JVM
```bash
./amper.bat run --module jvmApp
```

### Windows (Native)
```bash
./amper.bat run --module windowsApp
# または生成されたexeを直接実行
# ./build/tasks/_windowsApp_linkMingwX64Debug/windowsApp.exe
```

### Linux (Native)
Linux環境にて:
```bash
./amper run --module linuxApp
```