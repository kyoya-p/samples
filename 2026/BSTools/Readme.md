# バトルスピリッツツール群 (Battle Spirits Project)

Kotlin/Native (Amper) によるバトルスピリッツ向けツール群。

---

# ツール
- GameServer : ゲームの各状況での選択肢を列挙・盤面シミュレーションを行う。 (Kotlin/Native)
- Playmats : Game の WebUI。 GameServerに接続し、状況の表示と選択の指示 (Kotlin/Native)
- SurveyX : X (Twitter) 探索および大会結果・環境メタデータ調査ツール (Kotlin JVM / Playwright / Clikt)
- model : 共通データモデル定義ライブラリ (Kotlin Multiplatform / Native)

---

## 実行方法

### 1. mise task による実行 (推奨)

```shell
# ビルド
mise run build

# GameServer (選択肢列挙エンジン) 起動 — 先に起動する
mise run game-server

# Playmats (WebUI / RuleAPI エンジン) 起動
# GameServer に選択肢の列挙を委譲する。未起動でも同一ルールでローカル列挙にフォールバックする
mise run playmats

# 引数 (接続先URL等) を指定して起動する場合
mise run playmats -- --game-url http://localhost:8081

# SurveyX (対戦環境調査ツール) 起動
mise run survey-x
```

### 2. Amper による直接実行

```shell
.\amper.bat build

# GameServer (選択肢列挙エンジン) 起動 — 先に起動する
.\amper.bat run -m GameServer

# Playmats (WebUI / RuleAPI エンジン) 起動
.\amper.bat run -m Playmats -- --game-url http://localhost:8081

# SurveyX (対戦環境調査ツール) 起動
.\amper.bat run -m SurveyX
```
