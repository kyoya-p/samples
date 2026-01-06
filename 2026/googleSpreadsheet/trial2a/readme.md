指示:
  - googleアカウントでログイン
  - API経由でspreadsheetを生成
  - Google spreadsheet をAPIで操作
  - clientId, clientSecretをアプリに持たない(secret-less PKCE)

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


