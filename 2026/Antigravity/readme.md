指示:
  - アリケーションを作成
  - APIキーを取得
  - googleアカウントでログイン
  - API経由でspreadsheetを生成
  - Google spreadsheet をAPIで操作




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

## 検証結果

### 実行ログ成功例 ([test_log_final_v3.txt](file:///c:/Users/kyoya/works/samples/2026/Antigravity/test_log_final_v3.txt))
```text
1. Creating a test spreadsheet...
Successfully registered protocol: com.googleusercontent.apps.307495712434-ueklearfqoqp1u4657fk550pj49odfet
Waiting for authorization code from browser (custom scheme redirect)...
Success! Created Spreadsheet ID: 19XQv5sbY2HmInXO_dt0cRH-SisZVZYwJaFRWOa2iZdQ
URL: https://docs.google.com/spreadsheets/d/19XQv5sbY2HmInXO_dt0cRH-SisZVZYwJaFRWOa2iZdQ
2. Updating cell A1 with 'Hello Compose Desktop!'...
Success! Cell updated.
--- Test Completed Successfully ---
```

### 認証済スプレッドシート
![作成されたスプレッドシート](https://docs.google.com/spreadsheets/d/19XQv5sbY2HmInXO_dt0cRH-SisZVZYwJaFRWOa2iZdQ)
> [!NOTE]
> 各ステップの詳細は [implementation_plan.md](file:///C:/Users/kyoya/.gemini/antigravity/brain/2e77902b-8cbd-40d3-ac15-e02fa1d15af6/implementation_plan.md) に記録されています。

## 今後の展望
この基盤により、不特定多数のユーザーが自身の Google アカウントで安全にログインし、アプリが提供するスプレッドシート操作機能を享受できるようになります。セキュリティと利便性の究極のバランスが達成されました。

