---
okf_version: "0.2"
---

# 2026 Sample Projects Knowledge Bundle

本リポジトリ（Knowledge Bundle）に含まれるプロジェクト、ツール、ナレッジ、およびAIエージェント向けスキルへのナビゲーションインデックスです。

---

## AI & Agent Tools (AI・エージェント開発支援 / 運用)

* [AgyTools](./ChromeExt/AgyTools/README.md) - Google Antigravity (`agy`) の使用率（5時間枠・週次）とモデル別トークン使用量を記録・可視化する HTML ダッシュボード
* [ClaudeCodeTools](./ChromeExt/ClaudeCodeTools/README.md) - Claude Code の使用率とトークン使用量を記録・可視化する HTML ダッシュボード
* [ChromeUsagePanel](./ChromeExt/ChromeUsagePanel/README.md) - Claude Code 版 (`claude/`)・Antigravity 版 (`agy/`) に分離されたドロワー型 Chrome / Edge 拡張機能（`chrome.alarms` + Native Messaging 定期採取）
* [ChromeRecorder](./ChromeExt/ChromeRecorder/README.md) - デスクトップ画面とシステム音を逐次 WebM 保存する Chrome 拡張機能および Playwright 自動テスト生成
* [ChromeDoodle](./ChromeExt/ChromeDoodle/README.md) - Webページ上に直接フリーハンド・図形・テキスト注釈を描画し、暗転クロップ領域コピーや画像貼り付けができる Chrome 拡張機能
* [ChromeItr](./ChromeExt/ChromeItr/README.md) - 🤖 Tab Transcriber: `chrome.tabCapture` でタブ音声を取得し、Gemini Nano(不可時は Whisper / transformers.js)で文字起こしをページ上のダイアログに表示する Chrome 拡張機能
* [NanoTranslator](./ChromeExt/NanoTranslator/README.md) - 🤖 Chrome 内蔵 Gemini Nano で Web ページ・選択テキスト・入力文を翻訳する Chrome 拡張機能(外部通信なし、訳文を端末内辞書に保存)
* [GeminiNano](./GeminiNano/README.md) - Chrome 組み込み AI (Gemini Nano / Prompt API / Translator) の検証環境
* [Gemini/ComputerUse](./Gemini/ComputerUse/README.md) - Gemini モデルを用いた Computer Use / 操作自動化サンプルの検証
* [e-gemma2](./e-gemma2/mise.toml) - 🤖 gemma-2-2b-it(GGUF Q4_K_M)を llama-cpp-python のサーバで動かす mise タスク定義(127.0.0.1:8080)
* [mise.agents.toml](./mise.agents.toml) - Antigravity / Claude Code パネル更新および Stop フック一括登録の mise タスク定義
* [open-panel.mjs](./open-panel.mjs) - 🤖 HTML パネルを既定ブラウザで開く OS 非依存スクリプト(mise タスクから呼ぶ)
* [AgyTools/TASK.md](./AgyTools/TASK.md) - 🤖 AgyTools / ClaudeCodeTools のタスク管理(ステータス・完了タスク)

## Game Systems & Formal Verification (ゲームエンジン・形式検証)

* [BSRuleEngineServer](./BSRuleEngineServer/README.md) - バトルスピリッツの厳密な公式ルール処理・合法手判定、SSE リアルタイム盤面配信、および Playmat Web UI を備えた API サーバー
* [Bend](./Bend/Readme.md) - 高並列言語 Bend による疑似ランダム並べ替え（Scramble / Feistel 構造）および Lean 4 による形式証明
* [battle_spirits_bend](./battle_spirits_bend/README.md) - バトルスピリッツの Bend 言語シミュレーション実装
* [CandleSim](./CandleSim) - キャンドル / チャートシミュレーション
* [TLA-Plus](./TLA-Plus) - TLA+ による分散アルゴリズム・状態遷移の形式仕様記述とモデル検査

## Data Analysis & Visualization (データ分析・可視化)

* [Kabuka](./Kabuka) - シャープ (6753) 等の長期株価履歴に対するフーリエ変換・周波数領域解析・周期性予測およびダッシュボード
* [googleSpreadsheet/Antigravity](./googleSpreadsheet/Antigravity/readme.md) - Google Spreadsheet API と Antigravity の連携検証
* [plantuml](./plantuml/Readme.md) - PlantUML によるアーキテクチャ図・シーケンス図生成環境

## Multiplatform & Language Toolchains (マルチプラットフォーム・言語基盤)

* [PAU](./PAU/README.md) - Compose Multiplatform / Kotlin によるデスクトップアプリ実装（RangeList 処理、SBOM 生成、脆弱性検査）
* [Kotlin2.4/Ktor](./Kotlin2.4/Ktor/Readme.md) - Kotlin 2.x 環境における Ktor サーバー / クライアント検証
* [Kotlin2.4/Kotlinx.rpc](./Kotlin2.4/Kotlinx.rpc/README.md) - Kotlinx.rpc による型安全な RPC 通信サンプル
* [Kotlin2.4/Filament](./Kotlin2.4/Filament/README.md) - Google Filament リアルタイム物理ベースレンダリングの Kotlin 連携
* [KotlinToolchain](./KotlinToolchain) - Kotlin ツールチェーン検証
* [Firebases](./Firebases) - 🤖 Firebase 関連サンプル群(1_KMPComposeTUI / 6_NodeFBKtSDK / 7_FBSDK_Win / 18_FB_MS1 / 23_FB_MS2 / 24_FB_Send / 25_FB / 31_FbKmp / 41_DevMng)
* [Amper](./Amper) - JetBrains Amper ビルドツールの検証
* [DependencyCheck](./DependencyCheck/README.md) - OWASP Dependency-Check による依存関係脆弱性スキャン検証

## Networking & Real-Time Communication (ネットワーク・リアルタイム通信)

* [WebRTC](./WebRTC/Readme.md) - WebRTC による P2P / リアルタイムホワイトボード / BoardService POC
* [router](./router) - ネットワークルーティング・パケット処理 POC

## TUI & Developer Utilities (TUI・ユーティリティ・ツール)

* [filetui](./filetui/Readme.md) - ターミナル上で動作する TUI ファイルマネージャー
* [files](./files/Readme.md) - ファイル操作・同期・管理ツール群
* [filessh](./filessh) - SSH 経由のファイル操作ツール
* [mdedit](./mdedit) - Markdown エディタ
* [PwshTest](./PwshTest/v1/README.md) - PowerShell スクリプトテストスイート (v1〜v4)
* [Bookmarklet](./Bookmarklet/README.md) - ブラウザ用便利ブックマークレット集
* [AzurePortal](./AzurePortal/README.md) - Azure Portal 操作自動化スクリプト
* [vision/ffmpeg](./vision/ffmpeg/README.md) - FFmpeg による動画・画像変換処理
* [vision/geminiAgenticVision](./vision/geminiAgenticVision/README.md) - Gemini Agentic Vision による視覚認識サンプル
* [vision/screencapt](./vision/screencapt/mise.toml) - 🤖 指定間隔で画面を採取するロガー(Amper、windows_x64。mise タスク build / run / example1)

## Agent Skills (AIエージェント用スキル定義)

* [agy-tools SKILL](./ChromeExt/AgyTools/.agents/skills/agy-tools/SKILL.md) - AgyTools の使用ログ収集・HTML パネル更新用エージェントスキル
* [claude-code-tools SKILL](./ChromeExt/ClaudeCodeTools/.agents/skills/claude-code-tools/SKILL.md) - ClaudeCodeTools の使用ログ収集・HTML パネル更新用エージェントスキル
* [chrome-recorder SKILL](./ChromeExt/ChromeRecorder/.agents/skills/chrome-recorder/SKILL.md) - Chrome 拡張レコーダーと Playwright 自動テスト生成スキル
* [bs-rule-engine-dev SKILL](./BSRuleEngineServer/.agents/skills/bs-rule-engine-dev/SKILL.md) - バトスピルールエンジンサーバーの開発・テスト・API連携スキル
* [bend SKILL](./Bend/.agents/skills/bend/SKILL.md) - Bend 言語および Lean 4 形式証明のスキル
* [pau SKILL](./PAU/.agents/skills/pau/SKILL.md) - PAU プロジェクトのビルド・テスト・検証スキル
* [battle-spirits-bend SKILL](./battle_spirits_bend/.agents/skills/battle-spirits-bend/SKILL.md) - 🤖 Bend 言語によるバトルスピリッツのゲームエンジン開発・検証手順(保存則、HVM 並列ベンチ)
* [candlesim SKILL](./CandleSim/.agents/skills/candlesim/SKILL.md) - 🤖 線香の煙シミュレータ CandleSim の構成・運用知識
* [chrome-doodle SKILL](./ChromeExt/ChromeDoodle/.agents/skills/chrome-doodle/SKILL.md) - 🤖 ChromeDoodle の構成・実装規約・落とし穴・検証手順
* [chrome-itr SKILL](./ChromeExt/ChromeItr/.agents/skills/chrome-itr/SKILL.md) - 🤖 ChromeItr(Tab Transcriber)の構成・MV3 の落とし穴・検証手順
* [nano-translator SKILL](./ChromeExt/NanoTranslator/.agents/skills/nano-translator/SKILL.md) - 🤖 Nano Translator の構成・設定キー・Gemini Nano 実機での検証手順
* [files SKILL](./files/.agents/skills/files/SKILL.md) - 🤖 Node.js 製 TUI ファイルマネージャー files の実行・試験・保守
* [filetui SKILL](./filetui/.agents/skills/filetui/SKILL.md) - 🤖 TUI ファイルマネージャー filetui(ローカル・Explorer D&D・SSH 越し Linux)の実行・試験・保守
* [kabuka SKILL](./Kabuka/.agents/skills/kabuka/SKILL.md) - 🤖 株価ダッシュボード Kabuka の構成・運用知識
* [ktor SKILL](./Kotlin2.4/Ktor/.agents/skills/ktor/SKILL.md) - 🤖 Ktor 3.6(Kotlin 2.4.20)サンプル群のビルド・試験・起動・保守、HTTPS 証明書の生成
* [neo4j-installer SKILL](./Gemini/neo4j-installer/SKILL.md) - 🤖 Neo4j Community Edition の Windows への zip 導入と設定
