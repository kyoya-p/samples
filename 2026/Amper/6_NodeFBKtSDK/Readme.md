# Kotlin KMP Firebase Client Sample (Node.js)

Kotlin Multiplatform (Gradle) プロジェクトで、Node.js 環境から Firebase Firestore への読み書きを行うサンプルです。

### テスト実行
実行には Firebase API Key (`FB_API_KEY`) の設定が必須です。
Project ID (`FB_PROJECT_ID`) はデフォルトで `riot26-70125` が使用されますが、必要に応じて環境変数で上書き可能です。

### ビルド

```powershell
$env:FB_API_KEY="your-api-key"
./gradlew :js-app:jsNodeProductionRun
```

### 実行
```powershell
./gradlew kotlinNpmInstall
./gradlew assemble
$env:FB_API_KEY="your-api-key"
cd build/js/packages/5_NodeFB-js-app
node kotlin/5_NodeFB-js-app.js
```

## プロジェクト構造
- `shared/src/commonMain/kotlin/SampleItem.kt`: 共通データモデル
- `js-app/src/jsMain/kotlin/main.kt`: メインロジック（Firebase 初期化、読み書き検証）
- `js-app/src/jsMain/kotlin/*.kt`: Firebase SDK の Kotlin 外部宣言
- `js-app/build.gradle.kts`: アプリケーションのビルド設定
- `shared/build.gradle.kts`: 共通ライブラリのビルド設定
