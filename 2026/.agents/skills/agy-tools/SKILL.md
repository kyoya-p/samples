---
name: agy-tools
description: >-
  Google Antigravity (agy) の使用制限残量（5時間枠・週次）採取やトークン集計、HTML パネル生成ツールの運用・保守手順。
  AgyTools の更新、パネル再生成、手動サンプリングを行う際に使用する。
---

# AgyTools 運用スキル

Google Antigravity (`agy`) のモデル別リミット残量（Gemini Models / Claude and GPT models の5時間枠・週次）を追跡・可視化するツールの運用・保守ガイド。

## ディレクトリ構成
- ディレクトリ: `C:\Users\kyoya\projs\samples\2026\AgyTools`
- [usage_sample.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_sample.js): `agy -p "/usage"` の結果およびセッションログ（`~/.gemini/antigravity-cli/brain`）を集計し追記
- [usage_panel.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_panel.js): 集計データを HTML パネル (`agy-panel.html`) に出力
- [usage_hook.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_hook.js): フック用エントリ（10分間隔／リセット判定、排他制御）
- [README.md](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/README.md): 仕様・実行手順

## 基本操作

### 1. 手動サンプリング
即時で使用制限残量・トークン使用量を記録する場合:
```bash
node C:/Users/kyoya/projs/samples/2026/AgyTools/usage_sample.js
```

### 2. パネル再生成
可視化パネル (`agy-panel.html`) を手動で再出力する場合:
```bash
node C:/Users/kyoya/projs/samples/2026/AgyTools/usage_panel.js
```
生成された `agy-panel.html` をブラウザで開くことで、モデルグループ別の残量推移とリセット予定時刻を確認可能。
