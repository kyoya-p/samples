# ChromeUsagePanel — AI Usage Panel Chrome 拡張機能

Claude Code (`claude -p "/usage"`) と Antigravity (`agy -p "/usage"`) の `/usage` をバックグラウンドで定期採取し、5時間枠・週次残量推移およびモデル別トークン使用量を可視化する Manifest V3 Chrome / Edge 拡張機能です。

静的 HTML (`file://`) ではブラウザのセキュリティ制約によりローカルコマンド (`claude` / `agy`) を定期実行できないため、**`chrome.alarms` + Native Messaging Host** を用いて安全にローカル集計スクリプトと連携します。

## 構成

```text
ChromeUsagePanel/
├── extension/              # Chrome / Edge へ「パッケージ化されていない拡張機能」として読み込むフォルダ
│   ├── manifest.json       # MV3 マニフェスト（固定公開鍵 "key" により拡張機能 ID を固定）
│   ├── background.js       # Service Worker（chrome.alarms 定期実行・バッジ表示・Native Messaging 制御）
│   ├── panel.html / .css / .js  # ダッシュボード UI（Claude Code & Antigravity 統合チャート）
│   └── icons/
└── host/                   # Native Messaging Host (Node.js)
    ├── host.js             # stdin/stdout 4バイト長プレフィックス JSON プロトコル実装
    ├── host.cmd            # mise Node (node:sqlite 対応) 起動ラッパー
    ├── install.js          # ホストマニフェスト生成 & Chrome / Edge HKCU レジストリ登録
    └── com.kyoya.usage_panel.json  # install.js により生成されるホストマニフェスト
```

## セットアップ手順

### 1. Native Messaging Host を登録する（初回のみ）

```powershell
node C:\Users\kyoya\projs\samples\2026\ChromeUsagePanel\host\install.js
```

* `host/com.kyoya.usage_panel.json` を生成し、以下のレジストリキーに登録します（管理者権限不要・`HKCU`）:
  * `HKCU\Software\Google\Chrome\NativeMessagingHosts\com.kyoya.usage_panel`
  * `HKCU\Software\Microsoft\Edge\NativeMessagingHosts\com.kyoya.usage_panel`
* 解除したい場合は `node ChromeUsagePanel\host\install.js --uninstall` を実行します。

### 2. Chrome / Edge に拡張機能を読み込む

1. ブラウザで `chrome://extensions`（Edge の場合は `edge://extensions`）を開く
2. 右上の **デベロッパーモード** を ON にする
3. **パッケージ化されていない拡張機能を読み込む** をクリック
4. `C:\Users\kyoya\projs\samples\2026\ChromeUsagePanel\extension` フォルダを選択する

> `manifest.json` に公開鍵 (`key`) を含めているため、拡張機能 ID は常に `hcgagjaolphkmepikeiicapjpgcalfbg` に固定されます。

## 主な機能

* **バックグラウンド定期 `/usage` 採取**: `chrome.alarms` により `OFF` / `5分` / `10分` / `15分` / `30分` ごとに選択したプロバイダ（Claude Code / Antigravity）の `/usage` を自動取得（`/usage` はローカル枠照会のためトークン消費なし）。
* **即時操作ボタン**:
  * **今すぐ /usage 実行**: 10分ゲートをバイパスして即座に `/usage` を実行・記録・再描画。
  * **データ再読込**: `/usage` を叩かずに `~/.claude/usage`・`~/.gemini/usage`・セッションログを再集計して描画。
* **ツールバーアイコンバッジ**: 選択中のプロバイダの最新 5時間枠残量 (`%`) をアイコンバッジに表示（残量に応じて緑 / 黄 / 赤）。
* **チャート操作**:
  * 横ドラッグによる時間範囲パン、マウス位置中心のホイール拡大・縮小、凡例ボタンによる系列/モデル表示切替。
  * 既存の `ClaudeCodeTools` / `AgyTools` の Stop フックや `usage_samples.jsonl`・`localStorage` キーと完全互換。
