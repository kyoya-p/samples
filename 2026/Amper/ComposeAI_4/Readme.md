# ComposeAI Sample Project

このプロジェクトは、Gradle (Kotlin DSL) と Compose Multiplatform を使用したデスクトップアプリケーションのサンプルです。
住所録（Address Book）の管理機能を、モダンなUIと自動テスト環境で提供します。

# 主な機能
- **住所録管理**: 名前とメールアドレスの一覧表示、追加、削除。
- **UI**: Material Design 3 に準拠し、カスタムアイコンをウィンドウとインストーラーに採用。
- **自動テスト**: `runComposeUiTest` を用いた UI ロジックの自動検証。
- **開発支援**: ランダムデータの自動入力機能による迅速な動作確認。

# 開発環境
- **ビルドツール**: Gradle 9.0 (Kotlin DSL)
- **言語**: Kotlin 2.2.20 (Compose Multiplatform)
- **プラットフォーム**: JVM (Desktop)
- **推奨Java**: Java 21 (LTS) ※Java 25環境では実行時に互換性エラーが発生する場合があります。

# デバッグ実行
デスクトップアプリケーションを起動します。

```shell
./gradlew :jvm-app:run
```

# テスト
UIテストを含む全ての自動テストを実行します。

```shell
./gradlew test
```

# ビルド (MSIパッケージ)
Windows 用のインストーラー（MSI）を生成します。

```shell
./gradlew :jvm-app:packageMsi
```
成果物: `jvm-app/build/compose/binaries/main/msi/ComposeAI_3-1.0.0.msi`

# ビルド (実行可能JAR)
依存ライブラリを同梱したJARファイルを生成します。

```shell
./gradlew :jvm-app:packageUberJar-debug
```
成果物: `jvm-app/build/compose/binaries/main/uberjar/`