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
- [usage_hook.js](~/projs/samples/2026/ClaudeCodeTools/usage_hook.js): Stop フック入口（10分間隔／リセット時のみ追記判定、パネル再生成）
- [usage_sample.js](~/projs/samples/2026/ClaudeCodeTools/usage_sample.js): `claude -p "/usage"` の結果および `~/.claude/projects` ログを集計し追記（欠損枠は直近の既知値を自動継承）
- [usage_panel.js](~/projs/samples/2026/ClaudeCodeTools/usage_panel.js): 集計データを HTML パネル (`claude-panel.html`) にレンダリング（欠損値 forward-fill、画面アクティブ時 30 秒定期自動リフレッシュ）
- [usage_samples.jsonl](~/projs/samples/2026/ClaudeCodeTools/usage_samples.jsonl): 採取データ（1行 = 1サンプリング）
- [claude-panel.html](~/projs/samples/2026/ClaudeCodeTools/claude-panel.html): 生成される HTML パネル
- [README.md](~/projs/samples/2026/ClaudeCodeTools/README.md): 仕様・フック設定手順

## 表示仕様（チャート共通）
- **編集先**: `claude-panel.html` は生成物。修正は `usage_panel.js` のテンプレートに行い、`node usage_panel.js` で再生成する。
- **配色**: ライトのみ（ダーク定義なし）。色は `:root` のトークン（週次は `--wk`）を使い、直書きしない。
- **見出し**: トークン使用量グラフは見出し「トークン使用量」で、切り替え欄は「累積」「/時」。
- **表示範囲**: 幅は選択値（`5h` なら 5 時間）に固定。現在時刻を x 軸の 30〜70% に収めつつ、次の更新時刻を右端に寄せる。更新時刻が無い場合は現在 = 50%。
- **折れ線**: 使用率が下がった点（リセット）で線を分割し、枠をまたぐ補間線を引かない。
- **折れ線の描画**: 変化点まで水平に進み、変化点で垂直に移動する階段状（斜線は引かない）。`stepPts` で座標列を変換。
- **クリップ**: プロット領域のクリップは全高（`H`）。0% の点・線が軸上で切れないようにする。
- **ドラッグ横スクロール（使用率グラフ）**: `pointerdown/move/up` で表示幅を保ったまま全体範囲内を移動。`user-select: none` と `selectstart` 抑止で文字選択を防ぐ。`touch-action: pan-y`。
- **リセット済み枠**: 更新時刻を過ぎた枠は、背景帯・予測線・100% 到達マーカーを描かない。
- **残り時間**: 1 時間未満は分（`33m`）表示。
- **アクセシビリティ**: チャート SVG に `aria-label`、button/select に `:focus-visible`、`prefers-reduced-motion` 対応、`touch-action`（使用率グラフは `pan-y`）。
- **確認**: ヘッドレス Edge のスクリーンショットで表示を確認する。

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
- 発火条件: Stop は応答ターン完了ごと。前回追記から 10 分未満かつ使用率が減っていなければ追記しない（フック失敗ではない）。エラー時のみ `usage_hook.log` が作られる

## 実装・運用上の注意点
- **環境非依存**: ユーザーディレクトリ（`os.homedir()`）を動的に解決し、特定環境（ユーザー名など）をハードコードしない。
- **Stop フック設定**: `usage_sample.js` を直接 Stop フックに登録しないこと（`claude -p` が Stop を再帰呼出しするため、必ず `usage_hook.js` を経由する）。
