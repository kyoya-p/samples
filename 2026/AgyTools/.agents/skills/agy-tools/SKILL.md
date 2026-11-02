---
name: agy-tools
description: >-
  Google Antigravity (agy) の使用率（5時間枠・週次、および Gemini 4 Argon 週次）採取やトークン集計、HTML パネル生成ツールの運用・保守手順。
  AgyTools の更新、パネル再生成、手動サンプリング、フック設定の確認を行う際に使用する。
---

# AgyTools 運用スキル

Google Antigravity (`agy`) の使用状況（5時間枠・週次・別枠の Gemini 4 Argon 週次・モデル別トークン）を追跡・可視化するツールの運用・保守ガイド。

## ディレクトリ構成
- リポジトリ: `AgyTools`
- [usage_hook.js](~/projs/samples/2026/AgyTools/usage_hook.js): Stop フック入口（`{"decision":""}` の標準出力、10分間隔／リセット時のみ追記判定、パネル再生成、`process.exit(0)` による確実な終了）
- [usage_sample.js](~/projs/samples/2026/AgyTools/usage_sample.js): `agy -p "/usage"` の結果および `~/.gemini/antigravity-cli/brain` ログを集計し追記（欠損枠は直近の既知値を自動継承）
- [usage_panel.js](~/projs/samples/2026/AgyTools/usage_panel.js): 集計データを HTML パネル (`agy-panel.html`) にレンダリング（3パネル構成、全系列トグル表示、欠損値 forward-fill）
- [usage_samples.jsonl](~/projs/samples/2026/AgyTools/usage_samples.jsonl): 採取データ（1行 = 1サンプリング）
- [agy-panel.html](~/projs/samples/2026/AgyTools/agy-panel.html): 生成される HTML パネル
- [README.md](~/projs/samples/2026/AgyTools/README.md): 仕様・フック設定手順
- [LOG.md](~/projs/samples/2026/AgyTools/LOG.md): 作業履歴・更新ログ
- [TASK.md](~/projs/samples/2026/AgyTools/TASK.md): タスク状況・今後の課題

## パネル仕様（3パネル構成）
1. **5時間枠 使用率（%）**
   - 系列: `Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 残り時間バッジ: カードヘッダーに集約配置（個別凡例の冗長表記は排除）
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存）
2. **週次 使用率（%）**
   - 系列: `Gemini 4 Argon`（デフォルト表示）、`Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 残り時間バッジ: カードヘッダーに集約配置
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存）
3. **トークン使用量**
   - モデル別積み上げエリアグラフ（「累積」と「/時」の切り替え）
   - 凡例の各モデル名クリックで表示／非表示をトグル可能
- **自動更新**: 画面アクティブ時（表示中）に 30 秒間隔で定期自動リフレッシュ（非アクティブ時は停止、復帰時に自動更新）。

## 表示仕様（チャート共通）
- **編集先**: `agy-panel.html` は生成物。修正は `usage_panel.js` のテンプレートに行い、`node usage_panel.js` で再生成する。
- **配色**: ライトのみ（ダーク定義なし）。色は `:root` のトークン（`--s1`〜、週次系は `--wk`）を使い、直書きしない。
- **見出し**: トークン使用量グラフは見出し「トークン使用量」で、切り替え欄は「累積」「/時」。
- **表示範囲**: 幅は選択値（`5h` なら 5 時間）に固定。現在時刻を x 軸の 30〜70% に収めつつ、次の更新時刻を右端に寄せる。更新時刻が無い／未採取の枠は現在 = 50%。
- **折れ線**: 値が下がった点（リセット）で線を分割し、枠をまたぐ補間線を引かない。
- **折れ線の描画**: 変化点まで水平に進み、変化点で垂直に移動する階段状（斜線は引かない）。`stepPts` で座標列を変換。
- **クリップ**: プロット領域のクリップは全高（`H`）。0% の点・線が軸上で切れないようにする。
- **ドラッグ横スクロール（使用率グラフ）**: `pointerdown/move/up` で表示幅を保ったまま全体範囲内を移動。`user-select: none` と `selectstart` 抑止で文字選択を防ぐ。`touch-action: pan-y`。
- **リセット済み枠**: 更新時刻を過ぎて未採取の枠は、背景帯・予測線・100% 到達マーカーを描かず、バッジは `更新済・未採取`。
- **残り時間**: 1 時間未満は分（`33m`）表示。
- **アクセシビリティ**: チャート SVG に `aria-label`、button/select に `:focus-visible`、`prefers-reduced-motion` 対応、`touch-action`（使用率グラフは `pan-y`）。
- **mise タスク**（`2026/mise.agents.toml`、OS・シェル非依存）: `mise run agy-panel` = 再生成 + ブラウザ起動、`mise run agents-panel`（= `agents-panels`）= Agy・Claude 両パネル。ブラウザ起動は `2026/open-panel.mjs`（`rundll32` / `open` / `xdg-open` を OS 別に選択）。タスクにシェル固有コマンド（`cmd /c start` 等）を書かない。
- **トークン使用量グラフ（累積・/時）**: 両モードとも積み上げ棒。累積は各区間の累積値、/時は区間の平均トークン/時。棒は不透明（`fill-opacity="1"`）＋ `shape-rendering="crispEdges"` ＋ 0.5px 重ね（境目の隙間防止）。/時 は次の区間まで 10 分（`GAP`）以内の隙間を次の棒まで伸ばす。区間が前と重なる場合は前の終端から描く。
- **トークン使用量グラフのドラッグ**: 横ドラッグで表示幅を保ったまま移動（最初の記録〜現在の範囲）。現在まで戻すと追従に戻る。範囲メニュー変更で現在に戻る。
- **ホイール（全グラフ）**: 下=隣の広い範囲、上=隣の狭い範囲へ切り替え。右端（終端時刻）は変えない。`preventDefault` のためグラフ上のページスクロールは止まる。
- **軸ラベル**: 端から 90px 以内の目盛りは間引き、端のラベルと重ならないようにする。
- **確認**: ヘッドレス Edge のスクリーンショットで表示を確認する。

## 枠情報の継続・欠損補完
- `agy -p "/usage"` の出力に特定モデル（例: 未使用モデルや別枠の `Gemini 4 Argon` 等）が含まれない場合、直近の過去サンプルから最後に記録された既知値（`lastKnownLimits`）を自動的に引き継いで保存する。
- パネル生成（`usage_panel.js`）時も、過去サンプルの欠損値に対して前方補完（forward-fill）を適用し、折れ線グラフの途切れを防止する。

## 基本操作

### 1. 手動での使用率サンプリング
即時で使用率・トークン使用量を記録する場合:
```bash
node usage_sample.js
```
※ `USAGE_GATE` 環境変数を付けない直接実行では、インターバルに関係なく常に追記されます。

### 2. HTML パネルの再生成
可視化パネル (`agy-panel.html`) を手動で再出力する場合:
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
- 再帰防止フラグ: `AGY_USAGE_SAMPLING=1`
- 発火条件: Stop は応答ターン完了ごと。承認待ちのまま終了するなどターンが完了しない場合は採取されない（フック失敗ではない）。ログは `~/.gemini/antigravity-cli/log/cli-*.log` で確認

### 4. ブラウザでの表示・確認（Orca）
```bash
orca goto --url "~/projs/samples/2026/AgyTools/agy-panel.html"
```

## 実装・運用上の重要事項
- **バックグラウンド実行方針 (`kihonn bg de syori seyo`)**:
  コマンド実行やタスクは原則バックグラウンドで実行し、UI をブロックしない。
- **環境非依存**:
  ユーザーディレクトリ（`os.homedir()`）を動的に解決し、ハードコードしない。
- **Stop フック契約**:
  `~/.gemini/config/hooks.json` に登録する際、標準出力に `{"decision":""}` を出力し、`process.exit(0)` で確実に終了する。`usage_sample.js` を直接フックに登録してはならない（`agy -p` による無限再帰防止）。
