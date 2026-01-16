# Kotlin KMP Firebase Client Sample (Node.js)

Kotlin Multiplatform (Gradle) プロジェクトで、Node.js 環境から Firebase Firestore への読み書きを行うサンプルです。
プロジェクトは Amper から Gradle 構成へ移行されました。

## セットアップ

1. **Gradle によるビルドと依存関係の解決**
```powershell
./gradlew assemble
```

## 実行方法

実行には Firebase の設定（API Key, Project ID）が必要です。
これらは環境変数を通してアプリケーションに渡されます。

### 1. Gradle を使用して実行 (推奨)
最も簡単な方法です。ビルドと実行を一括で行います。
```powershell
# 環境変数を設定して実行
$env:FB_API_KEY="your-api-key"; $env:FB_PROJECT_ID="riot26-70125"; ./gradlew :js-app:jsNodeProductionRun
```

### 2. 環境変数で設定を上書きして実行
（上記「1. Gradle を使用して実行」に統合されました）

### 3. ビルド済みファイルを実行 (Node.js 直接実行)
Gradle を介さず、ビルドされた JS ファイルを直接 `node` コマンドで実行する方法です。
`node_modules` の依存関係解決のため、パッケージディレクトリに移動して実行します。

```powershell
# 1. ビルド
./gradlew assemble

# 2. 環境変数を設定 (PowerShell)
$env:FB_API_KEY="your-api-key"
$env:FB_PROJECT_ID="riot26-70125"

# 3. 実行ディレクトリへ移動して node コマンド実行
cd build/js/packages/5_NodeFB-js-app
node kotlin/5_NodeFB-js-app.js
```

**ワンライナーでの実行例 (プロジェクトルートから):**
```powershell
$env:FB_API_KEY="AIzaSyDpE5hkTVWMt8iYPPm30yNL6KJ-YivAwJ4"; $env:FB_PROJECT_ID="riot26-70125"; cd 5_NodeFB/build/js/packages/5_NodeFB-js-app; node kotlin/5_NodeFB-js-app.js
```

## 動作検証 (E2E)

`jsNodeProductionRun` を実行すると、Firestore への書き込みと読み出し検証が行われます。
検証完了後、プロセスは自動的に終了します。

※ ネットワークエラーや設定不備によるハングアップを防ぐため、15秒のタイムアウトが設定されています。タイムアウト時はエラーコード 1 で終了します。

## プロジェクト構造
- `shared/src/commonMain/kotlin/SampleItem.kt`: 共通データモデル
- `js-app/src/jsMain/kotlin/main.kt`: メインロジック（Firebase 初期化、読み書き検証）
- `js-app/src/jsMain/kotlin/*.kt`: Firebase SDK の Kotlin 外部宣言
- `js-app/build.gradle.kts`: アプリケーションのビルド設定
- `shared/build.gradle.kts`: 共通ライブラリのビルド設定