指示:
  - アリケーションを作成
  - APIキーを取得
  - googleアカウントでログイン
  - API経由でspreadsheetを生成
  - Google spreadsheet をAPIで操作

# 実行

```bash
./gradlew run
```

----- 

# SheetMaster Desktop: シークレットレス PKCE 認証 完遂レポート

本プロジェクトでは、Compose Desktop アプリケーションにおいて `client_secret` を隠蔽し、最高レベルのセキュリティとネイティブなユーザー体験を両立する OAuth2 PKCE フローを実装・実証しました。

## 実現した主要機能

1. **シークレットレス PKCE 認証**:
   - [credentials.json](file:///c:/Users/kyoya/works/samples/2026/Antigravity/src/main/resources/credentials.json) に `client_secret` を含ませず、iOS 型クライアント ID を利用。
   - SHA-256 によるコードチャレンジ生成により、配布済みバイナリからのシークレット漏洩リスクを排除。

2. **カスタム URI スキームによるディープリンク**:
   - 認可完了後、ブラウザから `com.googleusercontent.apps.[ClientID]://` 形式の URL でアプリへ自動的に戻る仕組みを構築。
   - Windows レジストリへの動的プロトコル登録機能を [GoogleSheetsService.kt](file:///c:/Users/kyoya/works/samples/2026/Antigravity/src/main/kotlin/com/example/GoogleSheetsService.kt) に実装。

3. **マルチインスタンス通信 (シグナルファイル方式)**:
   - ブラウザから起動された別プロセスのアプリから、待機中のメインプロセスへ認証コードを安全に渡す仕組みを確立。

4. **認証後のスプレッドシート自動表示**:
   - 認証完了シグナルを受信した直後、作成されたスプレッドシートをブラウザの新しいタブで即座に開くロジックを実装。
   - 認証後のタブが `google.com` に留まる制約下でも、ユーザーを迷わせずシートへ誘導可能。

## 検証結果

### 最新の実行ログ (V10)
```text
--- Starting Sheets API Test ---
1. Creating a test spreadsheet...
Successfully registered protocol in registry.
Waiting for authorization code from browser (custom scheme redirect)...
Success! Received Auth Code.
Success! Created Spreadsheet ID: [SpreadsheetID]
Opening spreadsheet in browser: https://docs.google.com/spreadsheets/d/[SpreadsheetID]
2. Updating cell A1 with 'Hello Compose Desktop!'...
Success! Cell updated.
--- Test Completed Successfully ---
```

## ドキュメント
- [実装プラン (implementation_plan.md)](file:///C:/Users/kyoya/.gemini/antigravity/brain/29a17cdc-c53a-44e0-9d1b-90b2267ed196/implementation_plan.md)
- [完了ウォークスルー (walkthrough.md)](file:///C:/Users/kyoya/.gemini/antigravity/brain/29a17cdc-c53a-44e0-9d1b-90b2267ed196/walkthrough.md)

## 今後の展望
この基盤により、不特定多数のユーザーが自身の Google アカウントで安全にログインし、アプリが提供するスプレッドシート操作機能を享受できるようになります。自動表示機能の追加により、UX 面でも高い完成度を達成しました。


