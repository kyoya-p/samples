---
name: claude-code-tools
description: >-
  Claude Code の使用率（5時間枠・週次）採取やトークン集計、HTML パネル生成ツールの運用・保守手順。
  ClaudeCodeTools の更新、パネル再生成、手動サンプリング、フック設定の確認を行う際に使用する。
---

# Claude Code Tools 運用スキル

Claude Code の使用状況（5時間枠・週次・モデル別トークン）を追跡・可視化するツールの運用・保守ガイド。

## ディレクトリ構成
- リポジトリ: `ClaudeCodeTools`
- [usage_hook.js](file:///C:/Users/sharp/dev/projs/samples/2026/ClaudeCodeTools/usage_hook.js): Stop フック入口（10分間隔／リセット時のみ追記判定、パネル再生成）
- [usage_sample.js](file:///C:/Users/sharp/dev/projs/samples/2026/ClaudeCodeTools/usage_sample.js): `claude -p "/usage"` の結果および `~/.claude/projects` ログを集計し追記（欠損枠は直近の既知値を自動継承）
- [usage_panel.js](file:///C:/Users/sharp/dev/projs/samples/2026/ClaudeCodeTools/usage_panel.js): 集計データを HTML パネル (`claude-panel.html`) にレンダリング（欠損値 forward-fill）
- [usage_samples.jsonl](file:///C:/Users/sharp/dev/projs/samples/2026/ClaudeCodeTools/usage_samples.jsonl): 採取データ（1行 = 1サンプリング）
- [claude-panel.html](file:///C:/Users/sharp/dev/projs/samples/2026/ClaudeCodeTools/claude-panel.html): 生成される HTML パネル
- [README.md](file:///C:/Users/sharp/dev/projs/samples/2026/ClaudeCodeTools/README.md): 仕様・フック設定手順

## 枠情報の継続・欠損補完
- `claude -p "/usage"` の出力で一時的に特定の枠情報（5時間枠・週次など）が欠落した場合でも、直近のサンプルで記録された最新値を自動的に引き継いで保存する。
- パネル生成（`usage_panel.js`）時も、過去サンプルの欠損値に対して前方補完（forward-fill）を適用し、グラフの連続性を保持する。

## 基本操作

### 1. 手動での使用率サンプリング
即時で使用率・トークン使用量を記録する場合:
```bash
node usage_sample.js
```
※ `USAGE_GATE` 環境変数を付けない直接実行では、インターバルに関係なく常に追記されます。

### 2. HTML パネルの再生成
可視化パネル (`claude-panel.html`) を手動で再出力する場合:
```bash
node usage_panel.js
```

### 3. フックの動作確認・手動シミュレーション
フック実行（追記判定＋自動再生成）の動作をテストする場合:
```bash
node usage_hook.js
```
- 排他制御: `.usage_hook.lock`
- エラーログ: `usage_hook.log`
- 再帰防止フラグ: `USAGE_SAMPLING=1`

## 実装・運用上の注意点
- **環境非依存**: ユーザーディレクトリ（`os.homedir()`）を動的に解決し、特定環境（ユーザー名など）をハードコードしない。
- **Stop フック設定**: `usage_sample.js` を直接 Stop フックに登録しないこと（`claude -p` が Stop を再帰呼出しするため、必ず `usage_hook.js` を経由する）。
