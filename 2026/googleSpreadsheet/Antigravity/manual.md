# SheetMaster Desktop: 認証実装技術マニュアル

本書では、Compose Desktop アプリにおいて Google Sheets API を「クライアントシークレットなし（PKCE方式）」で利用するための最終結論と、開発過程で判明した NG ケースについて整理します。

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
- **プロセス間通信**: `Signal File` (シグナルファイル方式)
    - **課題**: ブラウザがアプリを呼び出す際、新しいインスタンスが起動してしまう。
    - **解決**: 2つ目のインスタンスが認証コードを一時ファイルに書き込み、待機中のメインインスタンスがそれを読み取って認証を継続。

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

### プロトコルのレジストリ登録 ([GoogleSheetsService.kt](file:///c:/Users/kyoya/works/samples/2026/Antigravity/src/main/kotlin/com/example/GoogleSheetsService.kt))
Windows 環境で `com.googleusercontent.apps...` というリンクをアプリが拾えるように、起動時に自動でレジストリを書き換えます。

### 起動引数の処理 ([Main.kt](file:///c:/Users/kyoya/works/samples/2026/Antigravity/src/main/kotlin/com/example/Main.kt))
ディープリンクから起動された際、コマンドライン引数から `code=` パラメータを抽出します。

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

---
このドキュメントは、シークレットレスなデスクトップアプリ開発の標準ガイドラインとして活用してください。
