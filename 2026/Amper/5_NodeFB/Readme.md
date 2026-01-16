# Kotlin KMP Firebase Client Sample (Node.js)

Kotlin Multiplatform (Gradle) プロジェクトで、Node.js 環境から Firebase Firestore への読み書きを行うサンプルです。

## セットアップ

1. **npm 依存関係のインストール**
   ```powershell
   npm install
   ```

2. **ビルド**
   ```powershell
   ./gradlew build
   ```

## 実行方法

環境変数を設定して、Gradle タスク経由で実行します。

### PowerShell
```powershell
${'$'}env:FB_API_KEY="your-api-key"; `
${'$'}env:FB_AUTH_DOMAIN="your-project-id.firebaseapp.com"; `
${'$'}env:FB_PROJECT_ID="your-project-id"; `
${'$'}env:FB_STORAGE_BUCKET="your-project-id.firebasestorage.app"; `
${'$'}env:FB_MESSAGING_SENDER_ID="your-messaging-sender-id"; `
${'$'}env:FB_APP_ID="your-app-id"; `
./gradlew jsNodeRun
```


### Bash / Zsh
```bash
FB_API_KEY="your-api-key" \
FB_AUTH_DOMAIN="your-project-id.firebaseapp.com" \
FB_PROJECT_ID="your-project-id" \
FB_STORAGE_BUCKET="your-project-id.firebasestorage.app" \
FB_MESSAGING_SENDER_ID="your-messaging-sender-id" \
FB_APP_ID="your-app-id" \
./gradlew jsNodeRun
```

## テスト実行

ユニットテストを実行します。
```powershell
./gradlew jsNodeTest
```

## 動作検証 (E2E)

`jsNodeRun` タスクを実行すると、自動的に以下の検証フローが動作します。

1.  Firestore にテスト用ドキュメントを書き込む。
2.  コレクションからドキュメント一覧を取得する。
3.  書き込んだデータが正しく取得できたか検証し、結果を出力する。

## プロジェクト構造
- `js-app/src/jsMain/kotlin/main.kt`: メインロジック（Firebase 初期化、読み書き検証）
- `js-app/src/jsMain/kotlin/*.kt`: Firebase SDK の Kotlin 外部宣言
- `shared/src/commonMain/kotlin/`: 共通モジュール（データモデル等）

