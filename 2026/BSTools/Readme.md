# バトルスピリッツツール群 (Battle Spirits Project)

Kotlin/Native (Amper) によるバトルスピリッツ向けツール群。

---

# ツール
- GameServer : ゲームの各状況での選択肢列挙・盤面シミュレーション・AI評価、および Playmats WebUI/SSE イベント配信を行う統合サーバー (Kotlin/Native)
- Playmats : 単一HTMLによる Web 盤面UI (`Playmats/index.html`)。GameServer に接続し盤面表示・操作・リアルタイム同期を行う
- SurveyX : X (Twitter) 探索および大会結果・環境メタデータ調査ツール (Kotlin JVM / Playwright / Clikt)
- model : 共通データモデル定義ライブラリ (Kotlin Multiplatform / Native)

---

## 実行方法

### 1. mise task による実行 (推奨)

```shell
# ビルド
mise run build

# GameServer 起動 (WebUI http://localhost:8080 も同時配信)
mise run game-server
# または
mise run playmats

# ブラウザでアクセス:
# http://localhost:8080

# SurveyX (対戦環境調査ツール) 起動
mise run survey-x
```

### 2. kotlin.bat による直接実行

```shell
.\kotlin.bat build

# GameServer 起動
.\kotlin.bat run -m GameServer

# SurveyX (対戦環境調査ツール) 起動
.\kotlin.bat run -m SurveyX
```
