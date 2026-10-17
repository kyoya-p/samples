# Battle Spirits Project: Universal Agent Guidelines & Memory

This repository contains tools, data pipelines, simulation engines, and analysis applications for the Battle Spirits (バトスピ) TCG.

---

## 1. Directory & Subsystem Map

- **`GameServer/`**: 統合ルールエンジン・盤面シミュレーション・AI評価・SSE配信サーバー (Kotlin/Native)
- **`Playmats/`**: 単一HTML盤面UI (`Playmats/index.html`)。GameServer と SSE で常時接続しリアルタイム同期
- **`SurveyX/`**: X (Twitter) 探索および大会結果・環境メタデータ調査ツール (Kotlin JVM / Playwright / Clikt)
- **`model/`**: 共通データモデルおよび公式ルール判定エンジン ([`Rules.kt`](file:///C:/Users/kyoya/projs/samples/2026/BSTools/model/src/Rules.kt))
- **`.agents/skills/`**: 標準プロジェクトスキル (`batspi`, `batspi-standard`, `batspi-eternal`, `battlespirits-deck-builder`, `bstools-standard-playmat`, `bstools-issues-roadmap`, `kann-es-training`)

---

## 2. 対戦フォーマットとカードプール（最新スタンダード仕様）

2026年4月1日よりバトルスピリッツは2大フォーマット制（スタンダード／エターナル）へ移行：

### ① スタンダードフォーマット (Standard)
- **使用可能範囲**: ブロックアイコンが**英字（現在:「A」）**のカードのみ使用可能（2026年4月1日〜2027年3月31日）。
- **同型再録ルール**:
  - 過去のカード（数字ブロックアイコン1〜13等）であっても、スタンダード環境向けにブロックアイコン「A」で同型再録されたカードは、**「数字」に加え「A」も併せ持つ扱い**となる。
  - 過去バージョンの同名・同効果カードであってもスタンダードフォーマットで使用可能。
  - 同名カードはバージョン（数字/英字）に関わらずデッキに合計3枚まで（禁止・制限カードリスト遵守）。
- **ブロックアイコン「A」の主要カードプール**:
  - **ブースターパック**:
    - `[26RBS01]` 創世の鼓動
    - `[26RBS02]` 幻惑の翔風
    - `[26RBS03]` 絶界の覇者
    - `[26RBS04]` 炎空の騎帝
  - **エントリーデッキ / スターター**:
    - `[26RSD03]` バトスピエントリーデッキ【翆海ノ甲魚】
    - `[26RSD04]` バトスピエントリーデッキ【白命ノ鉱人】
    - `[26RSD05]` バトスピエントリーデッキ【煌陽ノ雷竜】
    - `[26RSD06]` バトスピエントリーデッキ【蒼淵ノ凶獣】
    - `[26RSD07]` コラボスターター【仮面ライダー AGENT OF DREAM】
    - `[26RSD08]` ディーバスターター【始まりのシンフォニー】
    - `[26RSD09]` アニメスターター【覚醒の煉凰】
    - `[26RSD10]` アニメスターター【深淵の怪鯨】
    - `[26RSD11]` アニメスターター【蒼風の剣豪】
    - `[26RSD12]` コラボスターター【アイカツ！ スタンダードステージ！】

### ② エターナルフォーマット (Eternal)
- **使用可能範囲**: これまでに発売されたすべてのカード（数字ブロックアイコン1〜13、英字A）が使用可能（禁止カードを除く）。

---

## 3. 基本行動原則（推測の完全禁止）

1. **効果テキスト・ステータスの推測禁止**:
   - カード名、型番、コスト、軽減シンボル、系統、Lv/BP、効果テキスト、契約条件、煌臨条件は記憶から捏造・推測せず、必ず**公式サイト** ([battlespirits.com/cardlist](https://www.battlespirits.com/cardlist)) または **batspi wiki** ([batspi.com](https://batspi.com)) で完全一致を確認する。
2. **相互参照の原文照合**:
   - サーチ対象（例:「カード名に『○○』を含む」「系統:『○○』を持つ」）や煌臨条件・合体条件は、対象となるカードの原文テキストを取得して合致を検証する。
3. **ルール裁定遵守**:
   - 消滅（Lv0によるトラッシュ送り）は「破壊」ではないため「破壊時効果」は発揮されない。
   - 「この効果は防げない」効果よりも「効果を受けない」耐性が優先される。
