---
name: link-cert-verifier
description: >-
  Chrome 拡張 LinkCertVerifier: 右クリックメニュー「リンクのアドレスを検証」で
  リンク先へ遷移（HTTP GET）せずに URL 安全性・DNS 解決・TLS 証明書チェーンを検証表示する MV3 拡張機能と
  Node.js Native Messaging Host (com.kyoya.link_cert_verifier) の構成・規約・検証手順。
---

# LinkCertVerifier 運用・開発スキル

## ファイル構成
- [manifest.json](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/manifest.json): MV3 定義（`contextMenus`, `nativeMessaging`, `activeTab`, `scripting`, `storage`、固定公開鍵 `key` により ID `pfkihokdnpmhbgdgpghoahkhmiibgojl` を維持）
- [background.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/background.js):
  - `MENU_ID = 'lcv-verify-link-address'`（タイトル: `リンクのアドレスを検証`, `contexts: ['link']`）
  - `chrome.runtime.sendNativeMessage('com.kyoya.link_cert_verifier', ...)` で Node ホストを呼び出し、未登録時は URL 構文/偽装解析＋登録コマンド案内 (`nativeHostMissing: true`) へフォールバック
  - 通常タブへは `chrome.scripting.executeScript({ files: ['viewer.js', 'content.js'] })` でオンデマンド注入、`chrome://` 等注入不可ページは `result.html?requestId=...` のポップアップウィンドウを開く
- [viewer.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/viewer.js): `window.__LCV_VIEWER__` として結果 HTML・CSS・コピー/再検証イベントを提供（すべての動的値は `escHtml` でエスケープ必須）
- [content.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/content.js): `#link-cert-verifier-host` に `attachShadow({ mode: 'open' })` でドラッグ移動可能な検証モーダルを構築
- [result.html](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/result.html) / [result.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/result.js): `requestId` 待機モードと `mode=popup`（手動 URL 入力 / アクティブタブ検証）を兼用
- [host/host.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/host/host.js):
  - stdout は 4-byte UInt32LE 長 + UTF-8 JSON 専用（`console.log = console.error` に転送済み）
  - `tls.getCACertificates('default')` + `tls.getCACertificates('system')` を結合して Windows 信頼ストアも反映
  - `tls.connect({ rejectUnauthorized: false })` で期限切れ・ホスト名不一致・自己署名時も証明書チェーンを取得し、`socket.authorized` / `tls.checkServerIdentity` / `crypto.X509Certificate` で詳細評価
  - デフォルトは TCP + TLS ハンドシェイクのみ（`followRedirects: true` 明示時のみ `HEAD` リクエスト）
- [host/host.cmd](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/host/host.cmd) / [host/install.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/LinkCertVerifier/host/install.js): `HKCU\Software\Google\Chrome\NativeMessagingHosts\com.kyoya.link_cert_verifier` と Edge 側の登録/解除

## 検証手順・落とし穴
1. **構文チェック**:
   ```powershell
   $node = "C:\Users\kyoya\AppData\Local\mise\shims\node.exe"
   & $node -c ChromeExt/LinkCertVerifier/background.js
   & $node -c ChromeExt/LinkCertVerifier/viewer.js
   & $node -c ChromeExt/LinkCertVerifier/content.js
   & $node -c ChromeExt/LinkCertVerifier/result.js
   & $node -c ChromeExt/LinkCertVerifier/host/host.js
   & $node -c ChromeExt/LinkCertVerifier/host/install.js
   ```
2. **Native Host 単体試験 & 登録**:
   - `require('./ChromeExt/LinkCertVerifier/host/host.js').verifyLink(...)` で `https://`、Punycode、`http://`、自己署名/期限切れ等の判定を確認する。
   - `host/install.js` を実行して `host/com.kyoya.link_cert_verifier.json` 生成と HKCU レジストリ登録を行う。
   - `host/host.cmd` へ Native Messaging バイナリフレーム（4-byte LE + JSON）を流し、`ping` / `verifyLink` が壊れていない（stdout 汚染がない）ことを確認する。
