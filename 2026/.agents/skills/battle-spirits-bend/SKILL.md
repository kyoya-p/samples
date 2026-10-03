---
name: battle-spirits-bend
description: >-
  Guidelines and verification procedures for Battle Spirits card game engine development
  using the Bend programming language. Use when writing, testing, or validating Bend-based
  Battle Spirits game state logic, core conservation rules, or HVM parallel benchmarks.
---

# Battle Spirits Bend Engine Development Skill

Battle SpiritsのゲームエンジンおよびシミュレータをBend言語で開発・検証するためのガイドライン。

## 1. コア保存則（Core Invariant）の検証
Battle Spiritsの最重要不変条件: 全領域（Life, Reserve, Field, Trash, Void）のコア総数（通常48コア）の保存則。
- 各フェーズ遷移（スタート、コア、ドロー、リフレッシュ、メイン、アタック、エンド）前後でコア総数が一致することを検証。
- 検証コマンド:
  ```bash
  bend run tests/test_invariants.bend
  ```

## 2. 構文チェック・ビルド手順
Bendコードのコンパイルおよび構文エラーの検証:
- CPUインタープリタ実行:
  ```bash
  bend run <file.bend>
  ```
- Cコード生成 / Cターゲット実行:
  ```bash
  bend run-c <file.bend>
  ```
- CUDA / GPUターゲット実行（並列MCTSシミュレーション時）:
  ```bash
  bend run-cu <file.bend>
  ```

## 3. 既存成果物・テスト基盤の参照先
- ビルド環境: `battle_spirits_bend/Makefile`, `run.sh`
- テスト設計書: `.agents/teamwork/orchestrator_1/TEST_INFRA.md`
- 不変条件テスト実装: `battle_spirits_bend/tests/test_invariants.bend`
