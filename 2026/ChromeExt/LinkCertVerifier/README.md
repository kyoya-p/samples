# LinkCertVerifier

Web ページ上のリンクを右クリックして **「リンクのアドレスを検証」** を選ぶと、**リンク先へ遷移（HTTP GET）せずに** URL の安全性・DNS 名前解決・TLS 証明書チェーンを検証・表示する Chrome / Edge 拡張機能（Manifest V3）です。

## 主な機能

- **右クリックメニュー検証 (`リンクのアドレスを検証`)**: ページ上のリンク (`<a>`) を右クリックするだけで検証モーダルを表示
- **ページ非遷移の TLS ハンドシェイク検証**: 付属の Node.js Native Messaging Host (`com.kyoya.link_cert_verifier`) が TCP + TLS ハンドシェイクのみを実行し、リンク先の HTML 取得やスクリプト実行は行いません
- **URL なりすまし・フィッシング検知**:
  - Punycode / IDN ドメイン表示 (`xn--...` と Unicode 表示名の併記)
  - 認証情報付き URL (`user@host`) の警告
  - リンク表示テキストと実際の遷移先ドメインの不一致警告
  - IP 直接指定、非標準ポート、プライベート/ループバック IP の警告
- **TLS 証明書・チェーン詳細表示**:
  - 総合判定（安全 / 注意あり / 危険 / エラー）
  - プロトコル (`TLSv1.3` / `TLSv1.2`)・暗号スイート・ALPN・ハンドシェイク時間
  - 信頼チェーン検証結果・ホスト名一致確認
  - 発行先 (Subject)・発行者 (Issuer)・有効期間と残り日数・公開鍵・シリアル番号・SAN 一覧・SHA-256 指紋コピー
  - サーバー証明書 / 中間 CA / ルート CA の階層表示
- **オプション追跡機能（明示クリック時のみ）**:
  - `http://` リンクに対する HTTPS (443) 証明書の試行
  - `HEAD` リクエストによるリダイレクト経路追跡（短縮 URL 等の確認用）
- **フォールバック表示 & ツールバー Popup**:
  - `chrome://` 等スクリプト注入不可ページでは専用ポップアップウィンドウ (`result.html`) で結果を表示
  - 拡張アイコンクリックで開くツールバー Popup から、現在のタブや任意 URL を手動検証可能

## ファイル構成

- [manifest.json](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/manifest.json) - MV3 定義（固定 Extension ID: `pfkihokdnpmhbgdgpghoahkhmiibgojl`）
- [background.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/background.js) - 右クリックメニュー登録、Native Messaging 呼び出し、UI 注入・フォールバックウィンドウ制御
- [viewer.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/viewer.js) - 結果レンダリング共通モジュール（Shadow DOM / `result.html` 共用）
- [content.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/content.js) - ページ上 Shadow DOM モーダル（ドラッグ移動・Esc/背景クリック閉じ）
- [result.html](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/result.html) / [result.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/result.js) - フォールバックウィンドウ & ツールバー Popup UI
- [host/host.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/host/host.js) - Node.js Native Messaging Host（DNS / TLS / X.509 解析）
- [host/host.cmd](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/host/host.cmd) - Windows 用 Native Host 起動ラッパー
- [host/install.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/host/install.js) - HKCU Chrome / Edge NativeMessagingHosts 登録スクリプト

## 導入手順

1. **Native Messaging Host を登録（初回のみ）**
   ```powershell
   node C:\Users\kyoya\projs\samples\2026\ChromeExt\LinkCertVerifier\host\install.js
   ```
   （解除する場合: `node ...\host\install.js --uninstall`）

2. **Chrome / Edge に拡張機能を読み込む**
   - `chrome://extensions/`（または `edge://extensions/`）を開く
   - 右上の **「デベロッパー モード」** を ON
   - **「パッケージ化されていない拡張機能を読み込む」** から `C:\Users\kyoya\projs\samples\2026\ChromeExt\LinkCertVerifier` を選択
   - `manifest.json` に公開鍵 (`key`) を固定しているため Extension ID は常に `pfkihokdnpmhbgdgpghoahkhmiibgojl` になります

## 使い方

1. 任意の Web ページ上のリンクを右クリックし、**「リンクのアドレスを検証」** をクリック
2. ページ遷移せずにモーダルが開き、URL 解析・DNS 解決・TLS 証明書情報が表示されます
3. 必要に応じて「HTTPS (443) の証明書を試行」「HEAD リダイレクト追跡」「結果を JSON コピー」を利用できます
