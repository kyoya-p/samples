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
- [usage_hook.js](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/usage_hook.js): Stop フック入口（`{"decision":""}` の標準出力、10分間隔／リセット時のみ追記判定、パネル再生成、`process.exit(0)` による確実な終了）
- [usage_sample.js](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/usage_sample.js): `agy -p "/usage"` の結果および `~/.gemini/antigravity-cli/brain` ログを集計し追記（欠損枠は直近の既知値を自動継承）
- [usage_panel.js](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/usage_panel.js): 集計データを HTML パネル (`agy-panel.html`) にレンダリング（3パネル構成、全系列トグル表示、欠損値 forward-fill）
- [usage_samples.jsonl](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/usage_samples.jsonl): 採取データ（1行 = 1サンプリング）
- [agy-panel.html](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/agy-panel.html): 生成される HTML パネル
- [README.md](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/README.md): 仕様・フック設定手順
- [LOG.md](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/LOG.md): 作業履歴・更新ログ
- [TASK.md](file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/TASK.md): タスク状況・今後の課題

## パネル仕様（3パネル構成）
1. **5時間枠 使用率（%）**
   - 系列: `Gemini Models (5h)`（デフォルト表示）、`Claude & GPT (5h)`（デフォルト非表示）
   - 残り時間バッジ: カードヘッダーに集約配置（個別凡例の冗長表記は排除）
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存）
2. **週次 使用率（%）**
   - 系列: `Gemini 4 Argon (週次)`（デフォルト表示）、`Gemini Models (週次)`（デフォルト表示）、`Claude & GPT (週次)`（デフォルト非表示）
   - 残り時間バッジ: カードヘッダーに集約配置
   - 凡例クリックで各系列の表示／非表示をトグル可能（`localStorage` に保存）
3. **累積トークン使用量 / トークン使用速度**
   - モデル別積み上げエリアグラフ（「累積」と「時間あたり」の切り替え）
   - 凡例の各モデル名クリックで表示／非表示をトグル可能

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

### 4. ブラウザでの表示・確認（Orca）
```bash
orca goto --url "file:///C:/Users/sharp/dev/projs/samples/2026/AgyTools/agy-panel.html"
```

## 実装・運用上の重要事項
- **バックグラウンド実行方針 (`kihonn bg de syori seyo`)**:
  コマンド実行やタスクは原則バックグラウンドで実行し、UI をブロックしない。
- **環境非依存**:
  ユーザーディレクトリ（`os.homedir()`）を動的に解決し、ハードコードしない。
- **Stop フック契約**:
  `~/.gemini/config/hooks.json` に登録する際、標準出力に `{"decision":""}` を出力し、`process.exit(0)` で確実に終了する。`usage_sample.js` を直接フックに登録してはならない（`agy -p` による無限再帰防止）。
