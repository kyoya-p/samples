---
name: token-saver
description: >-
  Token optimization and frugal context management guidelines. Use this skill
  when the user requests to minimize token usage, asks to save tokens, or when
  running token-efficient operations.
---

# Token Saver Workflow

トークン消費を最小化するための実行手順。

## 1. ファイル閲覧の最小化 (view_file)
- ファイル全体の読み込みは禁止。
- 必ず `StartLine` および `EndLine` を指定し、必要な箇所（最大50〜100行程度）のみ参照。
- 対象箇所が不明な場合は、まず `grep` や `Select-String` で行番号を特定。

## 2. コマンド実行結果の抑制 (run_command)
- 長大な出力が予想されるコマンドは出力を絞り込み。
  - PowerShell: `| Select-Object -First 20`
  - Linux: `| head -n 20`
- 実行ログやビルドログ全体の標準出力垂れ流しを回避。

## 3. 探索タスクの分離 (invoke_subagent)
- 複数ファイルにまたがる調査や試行錯誤は `research` サブエージェントに委譲。
- メインセッションのコンテキスト肥大化を防止。

## 4. 応答の簡潔化
- 挨拶、装飾、前置きを排除。
- 要点のみ体言止めで回答。

## 5. Token残量監視と中断ルール (Usage Monitoring)
- 定期的に、または長時間・高負荷作業前にトークン残量を確認:
  - コマンド: `agy -p "/usage"`
- 週間残量（Weekly Limit Remaining）または枠残量が **10%未満** の場合:
  1. 直ちに自動実行・サブエージェント等の重い処理を停止。
  2. ユーザーに現在の残量パーセンテージとリセット日時を報告し、続行可否の問い合わせを実施。
