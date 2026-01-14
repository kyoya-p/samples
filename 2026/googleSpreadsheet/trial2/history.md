# SheetMaster Desktop: 認証実装技術マニュアル

Compose Desktop アプリにおいて Google Sheets API を「クライアントシークレットなし（PKCE方式）」で利用するための最終結論と、開発過程で判明した NG ケースについて整理します。

## 1. 最終的な構成 (Success Pattern)

最も安全でネイティブな体験を実現する構成は以下の通りです。

- **クライアント ID タイプ**: `iOS`
    - **理由**: iOS 型はシークレットを不要とする「公開クライアント」でありながら、Android 型のような「デスクトップブラウザからの認可ブロック」が極めて限定的であるため。
- **認可方式**: `OAuth 2.0 PKCE` (Proof Key for Code Exchange)
    - **セキュリティ**: バイナリにシークレットを埋め込む必要がなく、逆コンパイルによる漏洩リスクを排除。
- **リダイレクト方式**: `Custom URI Scheme` + `Windows Registry`
    - **URI**: `com.googleusercontent.apps.[CLIENT_ID]:/oauth2callback`
    - **仕組み**: 認可完了後にブラウザから直接アプリを起動。
- **フォールバック**: 自動起動が失敗した場合は、端末に認証コードを直接入力可能。
    - **改善点**: 入力欄はURL全体のペーストにも対応。
- **プロセス間通信**: `Signal File` (シグナルファイル方式)
    - **課題**: ブラウザがアプリを呼び出す際、新しいインスタンスが起動してしまう。
    - **解決**: 2つ目のインスタンスが認証コードを一時ファイルに書き込み、待機中のメインインスタンスがそれを読み取って認証を継続。
- **プロキシ対応**: 社内ネットワーク等、プロキシ環境下での動作をサポート。
    - 環境変数 (`https_proxy` 等) を自動検出し、`System.setProperty` および `Authenticator` に適用。

## 2. NG ケースと失敗の分析 (NG Cases)

### ケース A: 「デスクトップアプリ」型 ID の使用
- **結果**: 失敗
- **原因**: Google の Java ライブラリはデスクトップ型において `client_secret` を必須とする設計。配布用アプリにシークレットを含めることはセキュリティポリシー（漏洩リスク）に反するため却下。

### ケース B: 「Android」型 ID + `localhost` リダイレクト
- **結果**: `Error 400: invalid_request - The loopback flow has been blocked`
- **原因**: Android 型 ID はセキュリティが非常に厳しく、モバイル端末以外（デスクトップブラウザ）からのループバックフローを Google が遮断している。

### ケース C: 「Android」型 ID + カスタム URI スキーム
- **結果**: 失敗 (認可画面の最終ステップでブロックされる)
- **原因**: Android 型は「アプリのパッケージ名」と「署名のハッシュ」を厳密に照合するため、Windows 環境でのシミュレートが事実上不可能。

## 3. 実装のハイライト

### プロトコルのレジストリ登録 ([GoogleSheetsService.kt](file:///src/main/kotlin/com/example/GoogleSheetsService.kt))
Windows 環境で `com.googleusercontent.apps...` というリンクをアプリが拾えるように、起動時に自動でレジストリを書き換えます。

### 起動引数の処理 ([Main.kt](file:///src/main/kotlin/com/example/Main.kt))
ディープリンクから起動された際、コマンドライン引数から `code=` パラメータを抽出します。
デバッグ用に起動引数を一時ファイルに出力する機能も搭載。

- **API**: Google Sheets API v4

## 5. セットアップ手順

### Google Cloud プロジェクトの設定

1.  **プロジェクトの作成**: [Google Cloud Console](https://console.cloud.google.com/) で新しいプロジェクトを作成します。
2.  **API の有効化**: 「API とサービス」 > 「ライブラリ」から **Google Sheets API** および **Google Drive API** を検索し、有効化します。
3.  **OAuth 同意画面の設定**:
    - 「API とサービス」 > 「OAuth 同意画面」を選択。
    - User Type を「外部」に設定（テスト用なら内部でも可）。
    - 必要なスコープとして `.../auth/spreadsheets` および `.../auth/drive.file` を追加します。
4.  **クライアント ID の作成**:
    - 「API とサービス」 > 「認証情報」 > 「認証情報を作成」 > 「OAuth クライアント ID」を選択。
    - アプリケーションの種類を **iOS** に設定。
    - 名前を入力し、Bundle ID（例: `com.example.sheetmaster`）を入力して作成します。
    - 作成後、JSON ファイルをダウンロードし `src/main/resources/credentials.json` として配置します。
    > [!IMPORTANT]
    > `client_secret` は JSON に含まれませんが、本システムはその状態で動作するよう設計されています。

### スプレッドシート側の準備

- 本システムは実行時に自動でスプレッドシートを作成します。
- 手動で既存のシートを操作する場合は、プログラム内の `spreadsheetId` を対象の ID に書き換えてください。
- Google ドライブ上でアプリが作成したファイルを確認・共有することができます。

## 6. 実行方法

### 開発時の実行
Gradle を使用してアプリケーションを起動します。

```bash
./gradlew run
```

### 認可コードのテスト（手動）
カスタム URI スキームの動作をシミュレートする場合、以下の引数付きコマンドでコードを渡すことができます。

```bash
./gradlew run --args="com.googleusercontent.apps.[CLIENT_ID]:/oauth2callback?code=[認証コード]"
```

# SheetMaster Desktop: シークレットレス PKCE 認証 完遂レポート

本プロジェクトでは、Compose Desktop アプリケーションにおいて `client_secret` を隠蔽し、最高レベルのセキュリティとネイティブなユーザー体験を両立する OAuth2 PKCE フローを実装・実証しました。

## 実現した主要機能

1. **シークレットレス PKCE 認証**:
    - [credentials.json](file:///src/main/resources/credentials.json) に `client_secret` を含ませず、iOS 型クライアント ID を利用。
    - SHA-256 によるコードチャレンジ生成により、配布済みバイナリからのシークレット漏洩リスクを排除。

2. **カスタム URI スキームによるディープリンク**:
    - 認可完了後、ブラウザから `com.googleusercontent.apps.[ClientID]://` 形式の URL でアプリへ自動的に戻る仕組みを構築。
    - Windows レジストリへの動的プロトコル登録機能を [GoogleSheetsService.kt](file:///src/main/kotlin/com/example/GoogleSheetsService.kt) に実装。

3. **マルチインスタンス通信 (シグナルファイル方式)**:
    - ブラウザから起動された別プロセスのアプリから、待機中のメインプロセスへ認証コードを安全に渡す仕組みを確立。

4. **認証後のスプレッドシート自動表示**:
    - 認証完了シグナルを受信した直後、作成されたスプレッドシートをブラウザの新しいタブで即座に開くロジックを実装。
    - `Desktop.browse` を優先しつつ、Windows `start` コマンドへのフォールバックも実装。

5. **プロキシ環境サポート**:
    - `https_proxy` 環境変数を検出し、Google API Client および Java HttpClient に自動適用。

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