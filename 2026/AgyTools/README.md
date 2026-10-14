# AgyTools

Google Antigravity (`agy`) のモデル別リミット残量（5時間枠・週次）を記録し、HTML パネルで表示するツール。

## ファイル構成

| ファイル | 役割 |
|---|---|
| `usage_sample.js` | `agy -p "/usage"` の残量とセッションログ（`~/.gemini/antigravity-cli/brain`）のトークン使用量を `usage_samples.jsonl` に追記 |
| `usage_panel.js` | `usage_samples.jsonl` から `agy-panel.html` を生成 |
| `usage_hook.js` | エージェントのフック用入口（10分間隔／リセット時のみ追記判定しパネル再生成） |
| `usage_samples.jsonl` | 蓄積された残量データ |
| `agy-panel.html` | 残量推移と次回リセット日時を表示する HTML パネル |

## 手動実行

### 1. 使用率サンプリング
```bash
node usage_sample.js
```

### 2. パネル再生成
```bash
node usage_panel.js
```
生成された `agy-panel.html` をブラウザで開くことで現在の残量状況を確認できます。
