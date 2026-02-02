# 概要

Kotlin Multiplatform Compose で[Mosaic](https://github.com/JakeWharton/mosaic)を使用するデモ。
buildtoolはAmper(最新)。

# プロジェクト構成

- **shared**: 共通ロジック (Mosaic TUIの実装)
- **jvmApp**: JVM用アプリケーション
- **windowsApp**: Windows Native用アプリケーション

# テスト実行
```shell
./amper run -m jvmApp  # jvmモジュール
./amper run -p linuxX64  # linuxプラットフォーム
```

# ビルド
```shell
./amper task
```

# アプリ実行
```shell
./build/tasks/.../....exe  # Windows版
```

# インストーラ作成
```shell
./amper task
```
