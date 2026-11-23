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
- [usage_panel.js](file:///C:/Users/kyoya/projs/samples/2026/ClaudeCodeTools/usage_panel.js): 集計データを HTML パネル (`claude-panel.html`) と自動更新用データ (`usage_data.js`) にレンダリング
- [claude-panel.html](file:///C:/Users/kyoya/projs/samples/2026/ClaudeCodeTools/claude-panel.html): 生成される HTML パネル（git管理下）
- [usage_data.js](file:///C:/Users/kyoya/projs/samples/2026/ClaudeCodeTools/usage_data.js): 自動更新用データ（.gitignore 対象）
- `~/.claude/usage/usage_samples.jsonl`: 採取ログデータ（git管理外）
- [README.md](file:///C:/Users/kyoya/projs/samples/2026/ClaudeCodeTools/README.md): 仕様・フック設定手順

## パネル仕様
1. **5時間枠 残量／使用率（%）**
   - 残り時間バッジ（`#rem-5h`）と現在残量（`#now-5h`: `% /5h`、20%未満で橙・10%未満で赤）をカードヘッダーに配置
   - 予測線（`#chk-proj` ON 時）: 枠開始基準（破線 `5 4`）＋ **1時間前基準**（点線 `1 3`）
   - 表示範囲切替（1h〜30d、`localStorage` に保存）
2. **週次 残量／使用率（%）**
   - 残り時間バッジ（`#rem-1w`）と現在残量（`#now-1w`: `% /7d`）をカードヘッダーに配置
   - 予測線（`#chk-proj` ON 時）: 枠開始基準（破線 `5 4`）＋ **1日前（24時間前）基準**（点線 `1 3`）
   - ドットなし折れ線グラフ（階段状、`stepPts` 補正）
3. **使用量（トークン）**
   - 「モデル別」「Effort別」「モデル×Effort別」および「/時」「/日」「/ログ」の切り替え
   - 下部凡例ボタン（`.cl-toggle`）に表示期間内の合計トークンを表示（ツールチップに平均レート併記）、クリックでモデル／系列ごとの表示／非表示をトグル可能（`localStorage` に保存）
- **自動更新**: 画面アクティブ時（表示中）に 60 秒間隔で定期自動リフレッシュ（非アクティブ時は停止、復帰時に自動更新）。

## 表示仕様（チャート共通）
- **編集先**: `claude-panel.html` は生成物。修正は `usage_panel.js` のテンプレートに行い、`node usage_panel.js`（または `writePanel(buildData(), true)`）で再生成する。
- **配色**: ライトのみ（ダーク定義なし）。色は `:root` のトークン（週次は `--wk`）を使い、直書きしない。
- **グラフ内フォントサイズ**: SVG グラフ内の `<text>` 要素（軸目盛・現在時刻・更新時刻・予測到達時刻）は視認性のため `font-size="15"`（従来の `10` の 1.5 倍）で統一する。
- **表示範囲**: 幅は選択値（`5h` なら 5 時間）に固定。現在時刻を x 軸の 30〜70% に収めつつ、次の更新時刻を右端に寄せる。更新時刻が無い場合は現在 = 50%。
- **折れ線**: 使用率が下がった点（リセット）で線を分割し、枠をまたぐ補間線を引かない。変化点まで水平に進み、変化点で垂直に移動する階段状（`stepPts`）。
- **クリップ**: プロット領域のクリップは全高（`H`）。0% の点・線が軸上で切れないようにする。
- **ドラッグ横スクロール（使用率グラフ）**: `pointerdown/move/up` で表示幅を保ったまま全体範囲内を移動。`user-select: none` と `selectstart` 抑止で文字選択を防ぐ。`touch-action: pan-y`。
- **リセット済み枠・未使用枠**: 更新時刻を過ぎた枠は、背景帯・予測線・100% 到達マーカーを描かない。また使用が発生していない（使用率 `0%`／残量 `100%` の）枠は、過去枠・現在枠ともに背景ハイライト帯を描画しない。
- **残り時間**: 1 時間未満は分（`33m`）表示。
- **mise タスク**（`2026/mise.agents.toml` および `ClaudeCodeTools/mise.toml`）:
  - `mise run claude-panel` = 再生成 + ブラウザ起動、`mise run agents-panel`（= `agents-panels`）= Agy・Claude 両パネル。
  - `mise enable-hook`（エイリアス: `enable-hooks`, `agents-hooks`）または `mise claude-hook` = `~/.claude/settings.json` に `Stop` フックを登録（Claude Code CLI および T3 Code 内 Claude Agent で共通動作）。
- **ホイール（全グラフ）**: 下=隣の広い範囲、上=隣の狭い範囲へ切り替え。右端（終端時刻）は変えない。`preventDefault` のためグラフ上のページスクロールは止まる。

## 枠情報の継続・欠損補完
- `claude -p "/usage"` の出力で一時的に特定の枠情報（5時間枠・週次など）が欠落した場合でも、直近のサンプルで記録された最新値を自動的に引き継いで保存する。
- パネル生成（`usage_panel.js`）時も、過去サンプルの欠損値に対して前方補完（forward-fill）を適用し、グラフの連続性を保持する。

## 基本操作

### 1. フックの有効化
```powershell
mise enable-hook
```
（または `mise claude-hook`、`ClaudeCodeTools/` ディレクトリ内での `mise enable-hook`）

### 2. 手動での使用率サンプリング
```powershell
node ClaudeCodeTools/usage_sample.js
```
※ `USAGE_GATE` 環境変数を付けない直接実行では、インターバルに関係なく常に追記されます。

### 3. HTML パネルの再生成
`claude-panel.html` の HTML テンプレート変更を反映して強制再生成する場合:
```powershell
node -e "const cl = require('./ClaudeCodeTools/usage_panel.js'); cl.writePanel(cl.buildData(), true);"
```
（データ `usage_data.js` のみ更新する場合は `node ClaudeCodeTools/usage_panel.js`）

### 4. フックの動作確認・排他制御
```powershell
node ClaudeCodeTools/usage_hook.js
```
- **排他制御**: `fs.openSync(lock, 'wx')` によるアトミックロック（`.usage_hook.lock`）。複数エージェントが同時に `Stop` しても多重実行されず安全に1プロセスのみが採取を行う（5分以上経過した古いロックは自動解除）。
- **エラーログ**: `usage_hook.log`
- **再帰防止フラグ**: `USAGE_SAMPLING=1`
- **発火条件**: Stop は応答ターン完了ごと。前回追記から 10 分未満かつ使用率が減っていなければ追記しない（フック失敗ではない）。

## 実装・運用上の注意点
- **環境非依存**: ユーザーディレクトリ（`os.homedir()`）を動的に解決し、特定環境（ユーザー名など）をハードコードしない。
- **Stop フック設定**: `usage_sample.js` を直接 Stop フックに登録しないこと（`claude -p` が Stop を再帰呼出しするため、必ず `usage_hook.js` を経由する）。
