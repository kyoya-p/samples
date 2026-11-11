---
okf_version: "0.2"
---

# 2026 Sample Projects Knowledge Bundle

本リポジトリ（Knowledge Bundle）に含まれるプロジェクト、ツール、ナレッジ、およびAIエージェント向けスキルへのナビゲーションインデックスです。
変更履歴は [log.md](./logs/log.md) を参照してください。

---

## AI & Agent Tools (AI・エージェント開発支援 / 運用)

* [AgyTools](./AgyTools/README.md) — Google Antigravity (`agy`) の使用率（5時間枠・週次）とモデル別トークン使用量を記録・可視化する HTML ダッシュボード
* [ClaudeCodeTools](./ClaudeCodeTools/README.md) — Claude Code の使用率とトークン使用量を記録・可視化する HTML ダッシュボード
* [ChromeRecorder](./ChromeRecorder/README.md) — デスクトップ画面とシステム音を逐次 WebM 保存する Chrome 拡張機能および Playwright 自動テスト生成
* [ChromeDoodle](./ChromeDoodle/README.md) — Webページ上に直接フリーハンド・図形・テキスト注釈を描画し、暗転クロップ領域コピーや画像貼り付けができる Chrome 拡張機能
* [GeminiNano](./GeminiNano/README.md) — Chrome 組み込み AI (Gemini Nano / Prompt API / Translator) の検証環境
* [Gemini/ComputerUse](./Gemini/ComputerUse/README.md) — Gemini モデルを用いた Computer Use / 操作自動化サンプルの検証
* [mise.agents.toml](./mise.agents.toml) — Antigravity / Claude Code パネル更新および Stop フック一括登録の mise タスク定義

## Game Systems & Formal Verification (ゲームエンジン・形式検証)

* [BSRuleEngineServer](./BSRuleEngineServer/README.md) — バトルスピリッツの厳密な公式ルール処理・合法手判定、SSE リアルタイム盤面配信、および Playmat Web UI を備えた API サーバー
* [Bend](./Bend/Readme.md) — 高並列言語 Bend による疑似ランダム並べ替え（Scramble / Feistel 構造）および Lean 4 による形式証明
* [battle_spirits_bend](./battle_spirits_bend/README.md) — バトルスピリッツの Bend 言語シミュレーション実装
* [CandleSim](./CandleSim) — キャンドル / チャートシミュレーション
* [TLA-Plus](./TLA-Plus) — TLA+ による分散アルゴリズム・状態遷移の形式仕様記述とモデル検査

## Data Analysis & Visualization (データ分析・可視化)

* [Kabuka](./Kabuka) — シャープ (6753) 等の長期株価履歴に対するフーリエ変換・周波数領域解析・周期性予測およびダッシュボード
* [googleSpreadsheet/Antigravity](./googleSpreadsheet/Antigravity/readme.md) — Google Spreadsheet API と Antigravity の連携検証
* [plantuml](./plantuml/Readme.md) — PlantUML によるアーキテクチャ図・シーケンス図生成環境

## Multiplatform & Language Toolchains (マルチプラットフォーム・言語基盤)

* [PAU](./PAU/README.md) — Compose Multiplatform / Kotlin によるデスクトップアプリ実装（RangeList 処理、SBOM 生成、脆弱性検査）
* [Kotlin2.4/Ktor](./Kotlin2.4/Ktor/Readme.md) — Kotlin 2.x 環境における Ktor サーバー / クライアント検証
* [Kotlin2.4/Kotlinx.rpc](./Kotlin2.4/Kotlinx.rpc/README.md) — Kotlinx.rpc による型安全な RPC 通信サンプル
* [Kotlin2.4/Filament](./Kotlin2.4/Filament/README.md) — Google Filament リアルタイム物理ベースレンダリングの Kotlin 連携
* [KotlinToolchain](./KotlinToolchain) — Kotlin ツールチェーン検証
* [Amper](./Amper) — JetBrains Amper ビルドツールの検証
* [DependencyCheck](./DependencyCheck/README.md) — OWASP Dependency-Check による依存関係脆弱性スキャン検証

## Networking & Real-Time Communication (ネットワーク・リアルタイム通信)

* [WebRTC](./WebRTC/Readme.md) — WebRTC による P2P / リアルタイムホワイトボード / BoardService POC
* [router](./router) — ネットワークルーティング・パケット処理 POC

## TUI & Developer Utilities (TUI・ユーティリティ・ツール)

* [filetui](./filetui/Readme.md) — ターミナル上で動作する TUI ファイルマネージャー
* [files](./files/Readme.md) — ファイル操作・同期・管理ツール群
* [filessh](./filessh) — SSH 経由のファイル操作ツール
* [mdedit](./mdedit) — Markdown エディタ
* [PwshTest](./PwshTest/v1/README.md) — PowerShell スクリプトテストスイート (v1〜v4)
* [Bookmarklet](./Bookmarklet/README.md) — ブラウザ用便利ブックマークレット集
* [dockercompose-gui](./dockercompose-gui) — Docker Compose 管理 GUI
* [AzurePortal](./AzurePortal/README.md) — Azure Portal 操作自動化スクリプト
* [vision/ffmpeg](./vision/ffmpeg/README.md) — FFmpeg による動画・画像変換処理
* [vision/geminiAgenticVision](./vision/geminiAgenticVision/README.md) — Gemini Agentic Vision による視覚認識サンプル

## Agent Skills (AIエージェント用スキル定義)

* [agy-tools SKILL](./AgyTools/.agents/skills/agy-tools/SKILL.md) — AgyTools の使用ログ収集・HTML パネル更新用エージェントスキル
* [claude-code-tools SKILL](./ClaudeCodeTools/.agents/skills/claude-code-tools/SKILL.md) — ClaudeCodeTools の使用ログ収集・HTML パネル更新用エージェントスキル
* [chrome-recorder SKILL](./ChromeRecorder/.agents/skills/chrome-recorder/SKILL.md) — Chrome 拡張レコーダーと Playwright 自動テスト生成スキル
* [bs-rule-engine-dev SKILL](./BSRuleEngineServer/.agents/skills/bs-rule-engine-dev/SKILL.md) — バトスピルールエンジンサーバーの開発・テスト・API連携スキル
* [bend SKILL](./Bend/.agents/skills/bend/SKILL.md) — Bend 言語および Lean 4 形式証明のスキル
* [pau SKILL](./PAU/.agents/skills/pau/SKILL.md) — PAU プロジェクトのビルド・テスト・検証スキル
