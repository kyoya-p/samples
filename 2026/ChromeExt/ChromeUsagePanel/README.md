# ChromeUsagePanel — Claude / Antigravity Usage Panel Chrome 拡張機能

Claude Code (`claude -p "/usage"`) と Antigravity (`agy -p "/usage"`) の `/usage` をバックグラウンドで定期採取し、**開いている既存 Web ページの右端ドロワー**として 5時間枠・週次残量推移およびモデル別トークン使用量を可視化する Chrome / Edge 拡張機能です。

Claude 版 (`claude/`) と Antigravity 版 (`agy/`) を独立した 2 つの拡張機能に分離しています。

## 構成

```text
ChromeExt/ChromeUsagePanel/
├── claude/                 # Claude Code 版拡張機能（chrome://extensions に読み込む）
│   ├── manifest.json       # ID 固定: ckchlpjbnhhnfdigpnaigpjkpakeefoi / Alt+Shift+C
│   ├── background.js       # chrome.alarms 定期実行・バッジ・ドロワー注入トグル
│   ├── drawer.js           # 既存ページへ Shadow DOM 右ドロワー＋開閉タブを注入（横ドラッグ幅変更・縦ドラッグ位置移動）
│   ├── panel_core.js       # チャート・操作 UI 描画
│   ├── styles.js           # ドロワー / タブ共用 CSS
│   ├── panel.html / panel_page.js  # chrome:// 等で注入不可時のフォールバック表示
│   └── icons/
├── agy/                    # Antigravity 版拡張機能（chrome://extensions に読み込む）
│   ├── manifest.json       # ID 固定: hffbpilenookaphajanlienkcedbbmpe / Alt+Shift+A
│   └── ...
└── host/                   # 共通 Native Messaging Host (Node.js)
    ├── host.js             # stdin/stdout 4バイト長プレフィックス JSON プロトコル
    ├── host.cmd            # mise Node (node:sqlite) 起動ラッパー
    ├── install.js          # 両拡張機能 ID を許可するホストマニフェスト生成 & Chrome / Edge 登録
    └── com.kyoya.usage_panel.json
```

## セットアップ手順

### 1. Native Messaging Host を登録する（初回のみ）

```powershell
node C:\Users\kyoya\projs\samples\2026\ChromeExt\ChromeUsagePanel\host\install.js
```

* `host/com.kyoya.usage_panel.json` を生成し、Claude 版・Agy 版両方の拡張機能 ID を `allowed_origins` に登録したうえで Chrome / Edge の `HKCU\...\NativeMessagingHosts\com.kyoya.usage_panel` に書き込みます。
* 解除したい場合は `node ChromeExt\ChromeUsagePanel\host\install.js --uninstall` を実行します。

### 2. Chrome / Edge に 2 つの拡張機能を読み込む

1. ブラウザで `chrome://extensions`（Edge の場合は `edge://extensions`）を開く
2. 右上の **デベロッパーモード** を ON にする
3. **パッケージ化されていない拡張機能を読み込む** から以下をそれぞれ読み込む:
   * Claude 版: `C:\Users\kyoya\projs\samples\2026\ChromeExt\ChromeUsagePanel\claude`
   * Antigravity 版: `C:\Users\kyoya\projs\samples\2026\ChromeExt\ChromeUsagePanel\agy`

## 使い方

* **サイドパネル開閉タブ**: 通常の Web ページ右端に各拡張の開閉タブ（`C` / `A`）が表示されます。
  * **クリック**: ドロワーを開閉（ツールバーアイコン / `Alt+Shift+C` / `Alt+Shift+A` / `Esc` でも開閉可）
  * **横方向ドラッグ**: ドロワー（Window）幅を変更
  * **縦方向ドラッグ**: 開閉タブの上下位置を移動（ページ間で位置保存）
  * `chrome://extensions` 等のスクリプト注入不可ページでツールバーアイコンを押した場合は専用タブ (`panel.html`) を開きます。
* **バックグラウンド自動採取**: `OFF` / `5分` / `10分` / `15分` / `30分` ごとに `chrome.alarms` 経由で `/usage` を自動採取（`/usage` はローカル枠照会のためトークン消費なし）。
* **ツールバーアイコンバッジ**: 最新の 5時間枠残量 (`%`) を常時表示。
* **チャート操作**: 横ドラッグで時間パン、マウス位置中心ホイールで拡大/縮小、凡例ボタンで系列/モデル表示切替。設定は `chrome.storage.local` に保存されるため全ページで共通維持されます。
