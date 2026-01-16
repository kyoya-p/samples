# Kotlin KMP Firebase Client Sample (Node.js)

Kotlin Multiplatform (Gradle) プロジェクトで、Node.js 環境から Firebase Firestore への読み書きを行うサンプルです。
プロジェクトは Amper から Gradle 構成へ移行されました。

## セットアップ

1. **Gradle によるビルドと依存関係の解決**
```powershell
./gradlew assemble
```

## 実行方法

提供された Firebase 設定が `main.kt` に埋め込まれているため、追加設定なしで実行可能です。
環境変数で上書きすることもできます。

### 1. Gradle を使用して実行 (推奨)
最も簡単な方法です。ビルドと実行を一括で行います。
```powershell
./gradlew :js-app:jsNodeProductionRun
```

### 2. 環境変数で設定を上書きして実行
```powershell
$env:FB_API_KEY="your-api-key"; `./gradlew :js-app:jsNodeProductionRun
```

### 3. ビルド済みファイルを実行
ビルド済みの JS ファイルを `node` で直接実行する場合、Gradle が管理する `node_modules` へのパス解決が必要なため、通常は `jsNodeProductionRun` タスクの使用を推奨します。

## 動作検証 (E2E)

`jsNodeProductionRun` を実行すると、Firestore への書き込みと読み出し検証が行われます。
検証完了後、プロセスは自動的に終了します。

## プロジェクト構造
- `shared/src/commonMain/kotlin/SampleItem.kt`: 共通データモデル
- `js-app/src/jsMain/kotlin/main.kt`: メインロジック（Firebase 初期化、読み書き検証）
- `js-app/src/jsMain/kotlin/*.kt`: Firebase SDK の Kotlin 外部宣言
- `js-app/build.gradle.kts`: アプリケーションのビルド設定
- `shared/build.gradle.kts`: 共通ライブラリのビルド設定