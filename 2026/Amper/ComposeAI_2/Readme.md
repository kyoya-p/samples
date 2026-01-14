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
./amper.bat run jvm-app
```

# テスト
UIテストを含む全ての自動テストを実行します。

```shell
./amper.bat test
```

# ビルド
プロジェクトをビルドし、クラスファイルやJARを生成します。

```shell
./amper.bat build
```
成果物: `build/tasks/` 内に生成されます。

# ターゲットファイル実行
ビルドされた成果物を直接実行する場合（依存関係の解決が必要です）：

```shell
java -jar 
```
