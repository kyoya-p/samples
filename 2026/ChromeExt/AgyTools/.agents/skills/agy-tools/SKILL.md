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
- 生成・採取データ配置先（`~/.gemini/usage/`）:
  - `usage_samples.jsonl`: 採取データ（1行 = 1サンプリング）
  - `agy-panel.html`: 生成される HTML パネル
  - `usage_data.js`: ブラウザ再読込なしの 30 秒自動更新用データ (`window.__USAGE = ...`)
  - `.usage_hook.lock`: 同時実行防止ロック（5分以上前の古いロックは無視）
  - `usage_hook.log`: フック実行エラーログ
- [README.md](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/README.md): 仕様・フック設定手順
- [LOG.md](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/LOG.md): 作業履歴・更新ログ
- [TASK.md](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/TASK.md): タスク状況・今後の課題

## パネル仕様（3パネル構成）
1. **5時間枠 残量／使用率（%）**
   - 系列: `Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 残り時間バッジ: カードヘッダーに集約配置（個別凡例の冗長表記は排除）
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存）
2. **週次 残量／使用率（%）**
   - 系列: `Gemini 4 Argon`（デフォルト表示）、`Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 残り時間バッジ: カードヘッダーに集約配置
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存）
3. **トークン使用量**
   - 積み上げ棒グラフ（「モデル別」「Effort別」「モデル×Effort別」の軸切替 + 「累積」「/時」モード + 時間範囲選択）
   - 凡例の各項目クリックで表示／非表示をトグル可能
- **自動更新**: 画面アクティブ時（`visibilityState === 'visible'`、ドラッグ中でないとき）に 30 秒間隔で `usage_data.js?t=...` を `<script>` 読み込みし、`render()` で再描画（`file://` でもページリロード不要・スクロールや選択状態を維持、非アクティブ復帰時にも経過していれば即時更新）。

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
- **編集先**: `agy-panel.html` / `usage_data.js` は生成物。修正は [usage_panel.js](file:///C:/Users/kyoya/projs/samples/2026/AgyTools/usage_panel.js) に行い、`node usage_panel.js` で再生成する。
- **配色**: ライトのみ（ダーク定義なし）。色は `:root` のトークン（`--s1`〜、週次系は `--wk`）を使い、直書きしない。
- **見出し**: トークン使用量グラフは見出し「トークン使用量」で、切り替え欄は「モデル別 / Effort別 / モデル×Effort別」「累積 / /時」。
- **表示範囲**: 幅は選択値（`5h` なら 5 時間）に固定。現在時刻を x 軸の 30〜70% に収めつつ、次の更新時刻を右端に寄せる。更新時刻が無い／未採取の枠は現在 = 50%。
- **折れ線**: 値が下がった点／増加・リセット点で境界を意識し、枠をまたぐ誤補間を引かない。変化点まで水平に進み、変化点で垂直に移動する階段状（`stepPts`）。
- **クリップ**: プロット領域のクリップは全高（`H`）。0% の点・線が軸上で切れないようにする。
- **ドラッグ横スクロール**: `pointerdown/move/up` で表示幅を保ったまま全体範囲内を移動。`user-select: none` と `selectstart` 抑止で文字選択を防ぐ。`touch-action: pan-y`。現在まで戻すと追従に戻り、範囲メニュー変更で現在に戻る。
- **リセット済み枠**: 更新時刻を過ぎて未採取の枠は、背景帯・予測線・100% 到達マーカーを描かず、バッジは `更新済・未採取`。
- **残り時間**: 1 時間未満は分（`33m`）や少数時間表示に対応。
- **アクセシビリティ**: チャート SVG に `aria-label`、button/select に `:focus-visible`、`prefers-reduced-motion` 対応、`touch-action`（使用率グラフは `pan-y`）。
- **mise タスク**（`2026/mise.agents.toml`、OS・シェル非依存）: `mise run agy-panel` = 再生成 + ブラウザ起動、`mise run agents-panel`（= `agents-panels`）= Agy・Claude 両パネル。ブラウザ起動は `2026/open-panel.mjs`（`rundll32` / `open` / `xdg-open` を OS 別に選択）。タスクにシェル固有コマンド（`cmd /c start` 等）を書かない。
- **トークン使用量グラフ（累積・/時）**: 両モードとも積み上げ棒。累積は各区間の累積値、/時は区間の平均トークン/時。棒は不透明（`fill-opacity="1"`）＋ `shape-rendering="crispEdges"` ＋ 0.5px 重ね（境目の隙間防止）。/時 は次の区間まで 10 分（`GAP`）以内の隙間を次の棒まで伸ばす。区間が前と重なる場合は前の終端から描く。
- **ホイール（全グラフ）**: 下=隣の広い範囲、上=隣の狭い範囲へ切り替え。右端（終端時刻）は変えない。`preventDefault` のためグラフ上のページスクロールは止まる。
- **軸ラベル**: 端から 90px 以内の目盛りは間引き、端のラベルと重ならないようにする。

## 枠情報の継続・欠損補完
- `agy -p "/usage"` の出力に特定モデル（例: 未使用モデルや別枠の `Gemini 4 Argon` 等）が含まれない場合、直近の過去サンプルから最後に記録された既知値（`lastKnownLimits`）を自動的に引き継いで保存する。
- パネル生成（`usage_panel.js`）時も、過去サンプルの欠損値に対して前方補完（forward-fill）を適用し、折れ線グラフの途切れを防止する。

## 基本操作

### 1. 手動での使用率サンプリング
即時で使用率・トークン使用量を記録する場合（`node` が PATH に無い環境では `C:\Users\kyoya\AppData\Local\mise\shims\node.exe` を使用）:
```powershell
& "C:\Users\kyoya\AppData\Local\mise\shims\node.exe" AgyTools/usage_sample.js
```
※ `USAGE_GATE` 環境変数を付けない直接実行では、インターバルに関係なく常に `~/.gemini/usage/usage_samples.jsonl` へ追記されます。

### 2. HTML パネル・データの手動再生成
`~/.gemini/usage/agy-panel.html` および `usage_data.js` を手動で再出力する場合:
```powershell
& "C:\Users\kyoya\AppData\Local\mise\shims\node.exe" AgyTools/usage_panel.js
```

### 3. フックの動作確認・手動シミュレーション
フック実行（追記判定＋追記時のみ自動再生成）の動作をテストする場合（`process.stdin.resume()` を待つため stdin に空 JSON 等を渡す）:
```powershell
'{}' | & "C:\Users\kyoya\AppData\Local\mise\shims\node.exe" AgyTools/usage_hook.js
```
- **発火対象・挙動の重要な仕様**:
  - `~/.gemini/config/hooks.json` の `Stop` フックは **`agy` CLI の応答ターン完了時のみ**発火する（T3 Code の `antigravity-acp` セッションでは `~/.gemini/config/hooks.json` は実行されないため、T3 Code のみ使っている間はサンプリングされず `usage_samples.jsonl` やパネルは更新されない）。
  - `usage_hook.js` は `USAGE_GATE=1` 付きで `usage_sample.js` を呼び、**前回追記から 10 分経過またはリセット（残量増加）で `usage_samples.jsonl` のファイルサイズが変化したときだけ** `usage_panel.js` を実行する。エラーは `~/.gemini/usage/usage_hook.log`、`agy` CLI 側フックログは `~/.gemini/antigravity-cli/log/cli-*.log` で確認する。

### 4. ブラウザでの表示確認
`~/.gemini/usage/agy-panel.html` をブラウザで開く（または `mise run agy-panel`）。確認用プレビューではヘッドレス Edge 等で表示や `unknown` の有無を確認する。

## 実装・運用上の重要事項
- **環境非依存**: ユーザーディレクトリ（`os.homedir()`）を動的に解決し、ハードコードしない。`node:sqlite` は Node 組み込みモジュール（Windows の mise shim `node.exe`）を使用する。
- **PATH 補完**: フック経由等で `agy` や `node` に PATH が通っていない場合に備え、`~/AppData/Local/mise/shims` を `PATH` 先頭へ補完する実装を維持する。
- **Stop フック契約**: `~/.gemini/config/hooks.json` に登録する際、標準出力に `{"decision":""}` を出力し、`process.exit(0)` で確実に終了する。`usage_sample.js` を直接フックに登録してはならない（`agy -p` による無限再帰防止：`AGY_USAGE_SAMPLING=1`）。
