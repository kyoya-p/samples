# ComposeAI Sample Project

このプロジェクトは、Amper と Compose Multiplatform を使用したデスクトップアプリケーションのサンプルです。
住所録（Address Book）の管理機能を、モダンなUIと自動テスト環境で提供します。

# 主な機能
- **住所録管理**: 名前とメールアドレスの一覧表示、追加、削除。
- **UI**: Material Design 3 に準拠し、SVGデザインに基づいたカスタムテーマを採用。
- **自動テスト**: `runComposeUiTest` を用いた UI ロジックの自動検証。
- **開発支援**: ランダムデータの自動入力機能による迅速な動作確認。

# 開発環境
- **ビルドツール**: Amper
- **言語**: Kotlin (Compose Multiplatform)
- **プラットフォーム**: JVM (Desktop)

# デバッグ実行
デスクトップアプリケーションをデバッグ用に起動します。

```shell
./amper run jvm-app
```

# テスト
UIテストを含む全ての自動テストを実行します。

```shell
./amper test
```

# ビルド
実行可能jar生成

```shell
./amper task :jvm-app:executableJarJvm
```
成果物: `build/tasks/_jvm-app_executableJarJvm`

# ターゲットファイル実行
生成された実行可能JAR（依存ライブラリ同梱）を直接実行する場合：

```shell
java -jar build/tasks/_jvm-app_executableJarJvm/jvm-app-jvm-executable.jar
```
※ JARファイルの生成には `./amper.bat task :jvm-app:executableJarJvm` の実行が必要です。

