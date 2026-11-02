# AgyTools

Google Antigravity (`agy`) の使用率（5時間枠・週次、および別枠の Gemini 4 Argon 週次）とモデル別トークン使用量を記録し、HTML パネルで表示するツール。

## ファイル

| ファイル              | 役割                                                                                                    |
| --------------------- | ------------------------------------------------------------------------------------------------------- |
| `usage_hook.js`       | Stop フックから呼ぶ入口。`{"decision":""}` を返しつつ `usage_sample.js` を実行し、追記があればパネルを再生成 |
| `usage_sample.js`     | `agy -p "/usage"` の残量（使用率換算）と、セッションログのモデル別トークン使用量を `usage_samples.jsonl` に1行追記 |
| `usage_panel.js`      | `usage_samples.jsonl` とセッションログから `agy-panel.html` を生成                                      |
| `usage_samples.jsonl` | 採取データ（1行 = 1採取）                                                                               |
| `agy-panel.html`      | 生成されるパネル（直接編集しない。再生成で上書きされる）                                                |

## 記録の条件

フック経由では、毎回 `/usage` を取得し、次のどちらかのときだけ追記する。

- 前回記録から10分以上経過
- いずれかの枠の残量が前回より増加（リセット＝使用率が減少）

`node usage_sample.js` を手動で実行した場合は毎回追記する。

### 枠情報の継続（欠損補完）

`agy -p "/usage"` の出力に一時的に特定の枠（例: 使用していないモデルや別枠の `Gemini 4 Argon` など）の情報が含まれない場合、直近のサンプルで記録された最新値を自動的に引き継いで保存します。
これにより、データ取得のタイミングによってモデル枠が欠落してもグラフが途切れず、連続した推移を維持できます。
また、HTML パネル生成時（`usage_panel.js`）にも過去サンプルの欠損値に対して同様の前方補完（forward-fill）が適用されます。

## 採取データの形式

```json
{
  "ts": "2026-10-16T01:04:31.344Z",
  "from": "前回記録の ts",
  "limits": {
    "gemini_5h": { "group": "Gemini Models", "type": "Five Hour Limit Remaining", "pct": 100, "usedPct": 0, "reset": "2026-10-16T05:57:45Z" },
    "gemini_week": { "group": "Gemini Models", "type": "Weekly Limit Remaining", "pct": 100, "usedPct": 0, "reset": "2026-10-23T00:57:45Z" },
    "gemini_argon_week": { "group": "Gemini 4 Argon", "type": "Gemini 4 Argon Weekly Limit", "pct": 95, "usedPct": 5, "reset": "2026-10-23T00:56:44Z" },
    "claude_gpt_5h": { "group": "Claude and GPT models", "type": "Five Hour Limit Remaining", "pct": 100, "usedPct": 0, "reset": "2026-10-16T06:04:08Z" },
    "claude_gpt_week": { "group": "Claude and GPT models", "type": "Weekly Limit Remaining", "pct": 100, "usedPct": 0, "reset": "2026-10-23T01:04:08Z" }
  },
  "tokens": { "total": 128072779, "input": 4054477, "cacheRead": 123867757, "output": 150545, "req": 898 },
  "byModel": {
    "Gemini 4 Argon": { "req": 8, "total": 3967763, "input": 25000, "cacheRead": 3940000, "output": 2763, "effort": { "Medium": 3967763 } }
  }
}
```

- `Gemini 4 Argon` は通常の `Gemini Models` とは独立した別枠の週次上限（`Gemini 4 Argon Weekly Limit`）として管理される
- `byModel` は `from` 〜 `ts` の区間の増加分
- トークン集計元: `~/.gemini/antigravity-cli/brain/*/.system_generated/logs/transcript.jsonl`（サブエージェントは親会話からモデル設定を継承）

## パネル（3パネル構成）

1. **5時間枠 使用率（%）**
   - `Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 凡例の項目をクリックすることで各枠の表示／非表示を自由に切り替え可能
2. **週次 使用率（%）**
   - `Gemini 4 Argon`（デフォルト表示）、`Gemini Models`（デフォルト表示）、`Claude & GPT`（デフォルト非表示）
   - 凡例の項目をクリックすることで各枠の表示／非表示を自由に切り替え可能（サンプル点のドットは非描画で折れ線のみ表示）
3. **トークン使用量**
   - 集計単位を「モデル別」「Effort別」「モデル×Effort別」からプルダウンで選択可能
   - 積み上げエリアグラフ（「累積」と「時間あたり」の切り替え表示）
   - 凡例の各項目をクリックして表示／非表示を切り替え可能
- 各グラフの表示範囲: 1h / 5h / 12h / 24h / 3d / 7d / 15d / 30d（ブラウザに保存、右端は枠の最終日時または100%到達日時の早いほう）
- 全系列・モデル・集計単位の選択状態はブラウザ（`localStorage`）に保存されます。
- 画面アクティブ時（表示中）に定期自動リフレッシュ（30秒間隔、非アクティブ時は停止、タブ復帰時に自動更新）。


## セットアップ

`~/.gemini/config/hooks.json` の `Stop` フックに登録する。

```json
{
  "agy-usage": {
    "Stop": [
      {
        "type": "command",
        "command": "node <path-to-AgyTools>/usage_hook.js",
        "timeout": 300
      }
    ]
  }
}
```

- `usage_sample.js` を `Stop` に直接登録しない（`agy -p` が `Stop` を再発火して無限再帰になる）
- 再帰防止は環境変数 `AGY_USAGE_SAMPLING`、同時実行防止は `.usage_hook.lock`、失敗は `usage_hook.log`

## Antigravity (`agy`) にフック設定を指示する場合

`agy` に次のように指示する（パスは環境に合わせて置き換える）。

```text
~/.gemini/config/hooks.json に agy-usage の Stop フックとして次のコマンドを追加して。
  node <path-to-AgyTools>/usage_hook.js
timeout は 300。既存のフック設定（orca-status など）は残すこと。
usage_sample.js を直接登録しないこと（agy -p が Stop を再発火して無限再帰になるため）。
追加後、JSON として読めることと、参照先ファイルが存在することを確認して。
```

## 注意

- Stop のたびに `agy -p "/usage"` が1回走る
- 変更は `usage_panel.js` に入れ、`node usage_panel.js` で再生成する
