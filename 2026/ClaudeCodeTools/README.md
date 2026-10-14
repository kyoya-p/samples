# ClaudeCodeTools

Claude Code の使用率（5時間枠・週次）とモデル別トークン使用量を記録し、HTML パネルで表示するツール。

## ファイル


| ファイル                  | 役割                                                                          |
| --------------------- | --------------------------------------------------------------------------- |
| `usage_hook.js`       | Stop フックから呼ぶ入口。毎回 `usage_sample.js` を実行し、追記があればパネルを再生成                      |
| `usage_sample.js`     | `claude -p "/usage"` の使用率と、セッションログのモデル別トークン使用量を `usage_samples.jsonl` に1行追記 |
| `usage_panel.js`      | `usage_samples.jsonl` とセッションログから `claude-panel.html` を生成                    |
| `usage_samples.jsonl` | 採取データ（1行 = 1採取）                                                             |
| `claude-panel.html`   | 生成されるパネル（直接編集しない。再生成で上書きされる）                                                |


## 記録の条件

フック経由では、毎回 `/usage` を取得し、次のどちらかのときだけ追記する。

- 前回記録から10分以上経過
- 5時間枠または週次の使用率が前回より減少（リセット）

`node usage_sample.js` を手動で実行した場合は毎回追記する。

## 採取データの形式

```json
{
  "ts": "2026-10-14T06:25:29.318Z",
  "sessionPct": 11, "sessionResets": "Oct 14, 7:50pm (Asia/Tokyo)",
  "weekPct": 61, "weekResets": "Oct 19, 7am (Asia/Tokyo)",
  "from": "前回記録の ts",
  "byModel": {
    "claude-opus-5-5": { "req": 8, "total": 1046064, "input": 16, "cacheCreate": 17988, "cacheRead": 1017157, "output": 10903, "effort": { "medium": 1046064 } }
  }
}
```

- `byModel` は `from` 〜 `ts` の区間の増加分。2026-10-14 以前の行には無い
- トークン集計元: `~/.claude/projects/**/*.jsonl`。重複除外（`requestId` + `message.id` + `usage`）、`<synthetic>` 除外

## パネル

- 5時間枠・週次の使用率推移: 現在値と残り時間、100% 到達予測（枠開始基準・1時間前基準）、更新時刻
- 累積トークン使用量: モデル別の積み上げエリア（最初の記録の値が起点）
- 各グラフの表示範囲: 1h / 5h / 12h / 24h / 3d / 7d / 15d / 30d（ブラウザに保存）

## セットアップ

`~/.claude/settings.json` の Stop フックに登録する（async）。

```json
"Stop": [
  { "hooks": [ { "type": "command", "command": "node <path-to-ClaudeCodeTools>/usage_hook.js", "timeout": 300, "async": true } ] }
]
```

- `usage_sample.js` を Stop に直接登録しない（`claude -p` が Stop を再発火して無限再帰になる）
- 再帰防止は環境変数 `USAGE_SAMPLING`、同時実行防止は `.usage_hook.lock`、失敗は `usage_hook.log`

## Claude Code にフック設定を指示する場合

Claude Code に次のように指示する（パスは環境に合わせて置き換える）。

```text
~/.claude/settings.json の hooks.Stop に、次のコマンドを async で追加して。
  node <path-to-ClaudeCodeTools>/usage_hook.js
timeout は 300。既存の Stop フックは残すこと。
usage_sample.js を直接登録しないこと（claude -p が Stop を再発火して無限再帰になるため）。
追加後、JSON として読めることと、参照先ファイルが存在することを確認して。
```

- フック設定は Claude Code の `update-config` スキルが扱う。「Stop のたびに」のような自動実行は、メモリではなくフックで設定する必要がある
- 反映には `/hooks` を開くか、セッションの再起動が必要な場合がある
- 確認: 10分以上あけて応答を1回行い、`usage_samples.jsonl` の末尾に行が増えるかを見る

## 注意

- Stop のたびに `claude -p "/usage"` が1回走る
- 割合はトークン数ベースで、料金・利用上限の消費率とは一致しない
- 変更は `usage_panel.js` に入れ、`node usage_panel.js` で再生成する