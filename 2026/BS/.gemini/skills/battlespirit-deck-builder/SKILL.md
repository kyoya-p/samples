web# Skill: Battle Spirits Deck Builder & Rules Advisor

## Purpose
このスキルは、バトルスピリッツのデッキ構築、ルール検討、カードデータの正確な検証を支援するためのものです。LLMの推測を排除し、必ずローカルのNeo4jデータベースまたはキャッシュファイル（YAML/HTML）に基づいた正確な回答を行うことを目的とします。

## Prerequisites
- **Neo4j DB**: `neo4j://127.0.0.1:7687` (ユーザー: `neo4j` / パスワード: `00000000`)
- **ローカルキャッシュ**:
  - YAMLデータ: `C:\Users\kyoya\.bscards\yaml\` または `~/.bscards/yaml/`
  - HTMLデータ: `C:\Users\kyoya\.bscards\html\` または `~/.bscards/html/`

## Strict Rules (厳守事項)
1. **推測の禁止**: カード番号、コスト、効果、系統等のデータについて、LLM自身の記憶や連番からの推測による回答は一切禁止。
2. **データの裏付け**: 必ずDBクエリ（Neo4j/Cypher）またはWeb/ファイル検索の結果と照合する。確認できない場合は「情報不足」と回答する。
3. **正確な検証**: サーチ対象や煌臨条件などの相互参照は、両カードのテキストを個別に取得し、キーワードの完全一致（例：「契約」を持つか等）を検証する。
4. **出力形式**:
    - 回答には都度情報ソース(リンクや取得元ファイル/クエリ結果)を提示し齟齬チェックを行う。
    - 説明は日本語。簡潔な回答。挨拶、ですます調不要。体言止めを推奨。

## Available Tools / Workflow
- **DB Query**: Neo4jに対するCypherクエリを実行し、カード間の関連性（系統、サポート対象）を取得する。
- **File Read**: キャッシュディレクトリ（`C:\Users\kyoya\.bscards\yaml\`）およびプロジェクト固有リファレンス（`.gemini/skills/battlespirit-deck-builder/references/`）内のファイルを読み込み、正確なカードテキストを取得する。
- **Knowledge Base**: `STANDARD_KNOWLEDGE.md` を参照し、スタンダードレギュレーション固有のルール（第2メインステップ、継召、ソウルマジック等）を確認する。
- **Project References**:
  - **全カードデータベース (15,142枚)**: `.gemini/skills/battlespirit-deck-builder/references/all_cards/` (YAML)
    - ※Wiki登録全15,452エントリを100%同期済み（転醒カード正規化後）。
    - 2026年度最新「契約編:証」第1章・第2章、およびコラボ等の特殊セットも完全網羅。
  - **禁止・制限カードリスト**: `.gemini/skills/battlespirit-deck-builder/references/restrictions/latest.md` (2026/05/30適用版)
- **Tools CLI**: `tools/shared/src/Main.kt` (コマンド: `fetch`, `neo` 等) を必要に応じて呼び出し、不足している最新カードデータを取得する。

## Example Usage
- 「BS75-CX03のサポートカードを探して」 -> Neo4jで対象カードの系統や条件をクエリし、合致するカードの一覧とソースを提示。
- 「このデッキのルール上の問題を指摘して」 -> 各カードの制限情報や煌臨条件を個別にYAMLから取得し、論理的な矛盾がないかステップバイステップで検証。
