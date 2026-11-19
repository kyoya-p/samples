---
name: agy-tools
description: >-
  Google Antigravity (agy) および T3 Code (antigravity-acp) の使用率（5時間枠・週次、および Gemini 4 Argon 週次）採取やトークン集計、HTML パネル生成ツールの運用・保守手順。
  AgyTools の更新、パネル再生成、手動サンプリング、フック・モデル解決の確認を行う際に使用する。
---

# AgyTools 運用スキル

Google Antigravity (`agy`) および T3 Code (`antigravity-acp`) の使用状況（5時間枠・週次・別枠の Gemini 4 Argon 週次・モデル×Effort 別トークン）を追跡・可視化するツールの運用・保守ガイド。

## ディレクトリ構成
- リポジトリ: `AgyTools`
- [usage_hook.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_hook.js): Stop フック入口（`{"decision":""}` の標準出力、10分間隔／リセット時のみ追記判定、追記時のみパネル再生成、`process.exit(0)` による確実な終了、mise shim `node.exe` 自動検出）
- [usage_sample.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_sample.js): `agy -p "/usage"` の結果および各 `brain` ログを集計し追記（欠損枠は直近の既知値を自動継承）
- [usage_panel.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_panel.js): 集計データを HTML パネル (`agy-panel.html`) と自動更新用データ (`usage_data.js`) にレンダリング（3パネル構成、全系列トグル表示、欠損値 forward-fill、過去サンプルの `unknown` 再解決）
- [agy-panel.html](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/agy-panel.html): 生成される HTML パネル（git管理下）
- [usage_data.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_data.js): ブラウザ再読込なしの 30 秒自動更新用データ (`window.__USAGE = ...`)（.gitignore 対象）
- 採取データ・ログ配置先（`~/.gemini/usage/`・git管理外）:
  - `usage_samples.jsonl`: 採取データ（1行 = 1サンプリング）
  - `.usage_hook.lock`: 同時実行防止ロック（5分以上前の古いロックは無視）
  - `usage_hook.log`: フック実行エラーログ
- [README.md](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/README.md): 仕様・フック設定手順
- [LOG.md](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/LOG.md): 作業履歴・更新ログ
- [TASK.md](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/TASK.md): タスク状況・今後の課題

## パネル仕様（3パネル構成）
1. **5時間枠 残量／使用率（%）**
   - 系列: `Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 残り時間バッジ（`#rem-5h`）と現在残量（`#now-5h`: `100% /5h`、20%未満で橙・10%未満で赤）をカードヘッダーに配置
   - 予測線（`#chk-proj` ON 時）: 枠開始基準（破線 `5 4`）＋ **1時間前基準**（点線 `1 3`）
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存、`#now-5h` も表示中の系列に連動）
2. **週次 残量／使用率（%）**
   - 系列: `Gemini 4 Argon`（デフォルト表示）、`Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 残り時間バッジ（`#rem-7d`）と現在残量（`#now-7d`: 表示中系列の最小残量 `% /7d`）をカードヘッダーに配置
   - 予測線（`#chk-proj` ON 時）: 枠開始基準（破線 `5 4`）＋ **1日前（24時間前）基準**（点線 `1 3`）
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存、`#now-7d` も表示中の系列に連動）
3. **使用量（トークン）**
   - 積み上げ棒グラフ（「モデル別」「Effort別」「モデル×Effort別」の軸切替 + 「/時」「/日」「/ログ」モード + 時間範囲選択）
   - 凡例の各項目クリックで表示／非表示をトグル可能
- **自動更新**: 画面アクティブ時（`visibilityState === 'visible'`、ドラッグ中でないとき）に 60 秒間隔で `usage_data.js?t=...` を `<script>` 読み込みし、`render()` で再描画（`file://` でもページリロード不要・スクロールや選択状態を維持、非アクティブ復帰時にも経過していれば即時更新）。

## ログ収集元とモデル・Effort 解決 (`getBrainDirs` / `loadT3ModelResolver`)
`usage_sample.js` と `usage_panel.js` の双方が同じ解決ロジックを持つ（変更時は両方を同期すること）。
1. **ログ収集元 (`getBrainDirs`)**:
   - `agy` CLI: `~/.gemini/antigravity-cli/brain/*/.system_generated/logs/transcript.jsonl`
   - T3 Code (`antigravity-acp`): `~/.t3/userdata/providers/antigravity/*/antigravity-acp/brain/*/.system_generated/logs/transcript.jsonl`
2. **モデル・Effort 解決の優先順**:
   - `transcript.jsonl` 内の `Model Selection` メッセージ（`MODEL_RE`）
   - T3 Code セッション等、`Model Selection` が無い場合は `loadT3ModelResolver()` で `~/.t3/userdata/statev2.sqlite`（Node 組み込み `node:sqlite` の `DatabaseSync` を `readOnly: true` で参照）と `~/.t3/userdata/model-manifest.json` を突き合わせる:
     - `orchestration_v2_projection_provider_threads` (`nativeThreadRef.nativeId` → `thread_id` / `provider_thread_id`)
     - `orchestration_v2_projection_runs` (`requested_at`〜`completed_at` 区間ごとの `modelSelection.model` と `options` の `thinking_level`)
     - `orchestration_v2_projection_threads`（run が見つからない場合のフォールバック）
   - SQLite に一致が無い場合は、プロンプト内の `<runtime_info>` 文（`through the Antigravity harness, as <slug>.` / `HARNESS_MODEL_RE`）やサブエージェント親スレッド（`parentOf[cid]`）をたどって解決する。
3. **既存キャッシュ行の自動修復（`usage_panel.js`）**:
   - `usage_samples.jsonl` 内の既存 `byModel` に `unknown`（モデルまたは `effort.unknown`）が残っている行も、パネル再生成時に最新の `rows` から `from < ts <= to` で再計算して上書き表示する（過去の `unknown` を残さない）。

## 表示仕様（チャート共通）
- **編集先**: `agy-panel.html` / `usage_data.js` は生成物。修正は [usage_panel.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_panel.js) に行い、`node usage_panel.js`（または `writePanel(buildData(), true)`）で再生成する。
- **配色**: ライトのみ（ダーク定義なし）。色は `:root` のトークン（`--s1`〜、週次系は `--wk`）を使い、直書きしない。
- **グラフ内フォントサイズ**: SVG グラフ内の `<text>` 要素（軸目盛・現在時刻・更新時刻・予測到達時刻）は視認性のため `font-size="15"`（従来の `10` の 1.5 倍）で統一する。
- **表示範囲**: 幅は選択値（`5h` なら 5 時間）に固定。現在時刻を x 軸の 30〜70% に収めつつ、次の更新時刻を右端に寄せる。更新時刻が無い／未採取の枠は現在 = 50%。
- **折れ線**: 値が下がった点／増加・リセット点で境界を意識し、枠をまたぐ誤補間を引かない。変化点まで水平に進み、変化点で垂直に移動する階段状（`stepPts`）。
- **クリップ**: プロット領域のクリップは全高（`H`）。0% の点・線が軸上で切れないようにする。
- **ドラッグ横スクロール**: `pointerdown/move/up` で表示幅を保ったまま全体範囲内を移動。`user-select: none` と `selectstart` 抑止で文字選択を防ぐ。`touch-action: pan-y`。現在まで戻すと追従に戻り、範囲メニュー変更で現在に戻る。
- **リセット済み枠・未使用枠**: 更新時刻を過ぎて未採取の枠は、背景帯・予測線・100% 到達マーカーを描かず、バッジは `更新済・未採取`。使用が発生していない（使用率 `0%`／残量 `100%` のままの）枠は、API のリセット時刻が現在時刻に合わせてスライドするだけで実セッションが存在しないため、過去枠・現在枠ともに背景ハイライト帯を描画しない。
- **残り時間**: 1 時間未満は分（`33m`）や少数時間表示に対応。
- **mise タスク**（`2026/mise.agents.toml` および `AgyTools/mise.toml`）:
  - `mise run agy-panel` = 再生成 + ブラウザ起動、`mise run agents-panel`（= `agents-panels`）= Agy・Claude 両パネル。
  - `mise enable-hook`（エイリアス: `enable-hooks`, `agents-hooks`）または `mise t3code-hook` / `mise agy-hook` = `~/.gemini/config/hooks.json` および T3 Code プロファイル（`~/.t3/userdata/providers/antigravity/*/config/hooks.json`）の両方に `Stop` フックを一括登録。
- **ホイール（全グラフ）**: 下=隣の広い範囲、上=隣の狭い範囲へ切り替え。右端（終端時刻）は変えない。`preventDefault` のためグラフ上のページスクロールは止まる。

## 枠情報の継続・欠損補完
- `agy -p "/usage"` の出力に特定モデル（例: 未使用モデルや別枠の `Gemini 4 Argon` 等）が含まれない場合、直近の過去サンプルから最後に記録された既知値（`lastKnownLimits`）を自動的に引き継いで保存する。
- パネル生成（`usage_panel.js`）時も、過去サンプルの欠損値に対して前方補完（forward-fill）を適用し、折れ線グラフの途切れを防止する。

## 基本操作

### 1. フックの有効化
```powershell
mise enable-hook
```
（または T3 Code 用のみなら `mise t3code-hook`、`AgyTools/` ディレクトリ内での `mise enable-hook`）

### 2. 手動での使用率サンプリング
```powershell
node AgyTools/usage_sample.js
```
※ `USAGE_GATE` 環境変数を付けない直接実行では、インターバルに関係なく常に `~/.gemini/usage/usage_samples.jsonl` へ追記されます。

### 3. HTML パネル・データの手動再生成
`AgyTools/agy-panel.html` の HTML テンプレート変更を反映して強制再生成する場合:
```powershell
node -e "const agy = require('./AgyTools/usage_panel.js'); agy.writePanel(agy.buildData(), true);"
```
（データ `usage_data.js` のみ更新する場合は `node AgyTools/usage_panel.js`）

### 4. フックの動作確認・排他制御
```powershell
'{}' | node AgyTools/usage_hook.js
```
- **発火対象・排他制御の仕様**:
  - `~/.gemini/config/hooks.json`（CLI 用）および `~/.t3/userdata/providers/antigravity/<profile>/config/hooks.json`（T3 Code 用）の両方に登録することで、CLI でも T3 Code でもターン完了時に自動発火する。
  - `usage_hook.js` は `fs.openSync(lock, 'wx')` による**アトミックな排他ロック**を行うため、T3 Code 等で複数のサブエージェントや複数スレッドが同時に `Stop` しても多重実行されず安全に1プロセスのみが採取を行う（5分以上経過した古いロックは自動解除）。
  - `USAGE_GATE=1` により、**前回追記から 10 分経過またはリセット（残量増加）で `usage_samples.jsonl` のファイルサイズが変化したときだけ** `usage_panel.js` を実行する。

## 実装・運用上の重要事項
- **環境非依存**: ユーザーディレクトリ（`os.homedir()`）を動的に解決し、ハードコードしない。`node:sqlite` は Node 組み込みモジュールを使用する。
- **PATH 補完**: フック経由等で `agy` や `node` に PATH が通っていない場合に備え、`~/AppData/Local/mise/shims` 等を `PATH` 先頭へ補完する実装を維持する。
- **Stop フック契約**: 標準出力に `{"decision":""}` を出力し、`process.exit(0)` で確実に終了する。`usage_sample.js` を直接フックに登録してはならない（`agy -p` による無限再帰防止：`AGY_USAGE_SAMPLING=1`）。
