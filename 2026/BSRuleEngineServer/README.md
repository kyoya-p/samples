# BS Rule Engine Server

バトルスピリッツ（Battle Spirits / バトスピ）のルールエンジンおよびリアルタイム盤面配信 API サーバー。
Git ログ上の `BSToolsCla`（純Kotlin ルールエンジン + GRU価値ネットワーク + Playmat Web サーバー）を元に、ルールエンジンとサーバー機能に特化して再構成したプロジェクトです。

---

## 🌟 特徴

1. **純粋なルールエンジン (`engine/`)**
   - ヒューリスティック評価コードを排除し、厳密な公式ルール処理・合法手判定に特化。
   - **レベル単位正規形列挙 (`LevelActions.kt`)**: プレイ時はリザーブ払い・Lvの境目の配置、コア移動はLv変更・ソウルコア移動を各対象1回に正規化。候補数を平均約6手に抑え、無限ループやコアの無意味な往復移動を原理的に排除。
   - **圧縮列挙 (`CompactActions.kt`)** / **全合法手列挙 (`Rules.kt`, `Rearrange.kt`)** も選択可能。
   - スタンダードおよびエターナルフォーマット対応。

2. **高性能・リアルタイムサーバー (`server/`)**
   - Kotlin/Native (Windows `mingwX64`) 向け WinSock ノンブロッキング I/O。
   - **Server-Sent Events (SSE)** による盤面変更のリアルタイム・プッシュ配信。
   - **マルチセッション管理 (`Session.kt`)**: ブラウザのタブごとに独立したセッションを保持し、同一セッションURLによる複数人観戦・対局も可能。
   - **アンドゥ / リドゥ (`/api/undo`, `/api/redo`)**: 最大500手の完全ロールバック。
   - **対局リプレイ保存**: 決着時に自動で `temp/` 配下に JSON ログを出力。
   - 盤面をブラウザで直感的に操作できる **Playmat Web UI (`server/web/index.html`)** を同梱。

3. **OpenAPI 3.0 完全対応 (`server/openapi.yaml`)**
   - RESTful API として外部クライアントや別言語エージェントからの操作が可能。

4. **GRU 価値ネットワークによる手番評価 (`rl/`)**
   - 評価モードとして `GRU` または `NONE`（評価なし）を選択可能。
   - 深さ $N$ (1〜4)・幅 $k$ のミニマックス探索による評価値計算と自動プレイに対応。

---

## 📁 ディレクトリ構成

```
BSRuleEngineServer/
├── project.yaml              # Amper プロジェクト定義 (engine, server, rl)
├── mise.toml                 # mise タスク定義 (build, server 等)
├── kotlin.bat / kotlin       # Kotlin Toolchain (Amper) ランチャー
├── README.md                 # 本ドキュメント
├── decks/                    # デッキ定義 (YAML)
│   ├── deck-kogyo.yaml       # 甲魚デッキ
│   ├── deck-fara.yaml        # ファラオム等
│   └── deck-rairyu.yaml      # 雷竜デッキ
├── engine/                   # BS ルールエンジン (ライブラリ)
│   ├── module.yaml
│   └── src/
│       ├── Model.kt          # 盤面・カード・プレイヤー・コア・フェーズ定義
│       ├── Rules.kt          # ルール進行、ターン/ステップ遷移、アタック・ブロック等
│       ├── LevelActions.kt   # Lv単位の正規形アクション列挙 (既定)
│       ├── CompactActions.kt # 圧縮選択肢列挙
│       ├── Rearrange.kt      # コア一括再配置
│       ├── GameSetup.kt      # 初期盤面生成・デッキ展開
│       ├── CardLoader.kt     # カード・デッキ定義ローダー
│       ├── OfficialCards.kt  # 公式カードマスターデータ
│       ├── cards/            # 個別カードスクリプト
│       └── effects/          # カード効果基盤
├── server/                   # BS Rule Engine Server (アプリケーション)
│   ├── module.yaml
│   ├── openapi.yaml          # OpenAPI 3.0 API仕様書
│   ├── src/
│   │   ├── main.kt           # HTTP / SSE サーバー、REST ルーティング
│   │   ├── WebBoard.kt       # WebGameStateDto、JSONシリアライズ
│   │   ├── Session.kt        # セッション管理、アンドゥ/リドゥ、ログ
│   │   └── Evaluation.kt     # アクション評価器
│   └── web/
│       └── index.html        # Playmat 盤面 Web UI
└── rl/                       # GRU ニューラルネットワーク評価器
    ├── module.yaml
    └── src/
        ├── GruValueNet.kt    # 純Kotlin GRU (9,249パラメータ)
        ├── Agents.kt         # ミニマックス探索エージェント
        ├── Features.kt       # 特徴量抽出
        ├── Game.kt           # 対局進行
        ├── Parallel.kt       # 並列対局
        ├── Trainer.kt        # 強化学習
        └── League.kt         # リーグ戦評価
```

---

## 🚀 ビルドと起動

### 必要な環境
- Windows 10/11 (x64)
- mise または PowerShell
- Kotlin Toolchain (Amper) は `kotlin.bat` により自動プロビジョニングされます。

### ビルド
```powershell
# BSRuleEngineServer ディレクトリ内で実行
cd BSRuleEngineServer

# デバッグビルド (全モジュール)
.\kotlin.bat build

# または mise を使用したリリースビルド
mise run build-server
```

### サーバー起動引数（CLI パラメータ）

サーバー起動時に指定できるコマンドライン引数一覧です。

| オプション | 既定値 | 説明 |
|---|---|---|
| `--port <n>` | `8090` | サーバーがリッスンするポート番号 |
| `--actions <type>` | `level` | 合法手列挙アルゴリズム (`level`: Lv単位正規形列挙, `compact`: 圧縮列挙, `all`: 全合法手列挙) |
| `--depth <n>` | `1` | ミニマックス探索の深さ (1〜4)。深さ2以上を指定する場合は `--width` に2以上または0を指定 |
| `--width <k>` | `1` | 探索の幅 (1=最善1手のみ深く読む, 0=全候補展開, 2以上=上位k手展開) |
| `--weights <file>` | なし | Player1 / Player2 共通の GRU 価値ネットワーク重みファイル |
| `--weights-p1 <file>` | なし | Player1 専用の重みファイル |
| `--weights-p2 <file>` | なし | Player2 専用の重みファイル |

#### 起動例
```powershell
# デフォルト起動 (Port: 8090, actions: level)
mise run server

# ポート8080、全合法手列挙モードで起動
mise run server --port 8080 --actions all

# 深さ2、幅3 の先読み探索で起動
mise run server --depth 2 --width 3 --weights weights/g1_g2-best.gru.weight
```

---

## 🌐 API エンドポイントとパラメータの与え方

詳細な OpenAPI 3.0 スキーマは [`server/openapi.yaml`](server/openapi.yaml) に定義されています。

### 共通パラメータ仕様

- **`room` / `session` (ルーム / セッションID)**:
  - 型: `string` (英数字・ハイフン・アンダースコア、最大64文字)
  - **既定値: `"default"`**
  - 対戦部屋ごとに独立した盤面・手番・アンドゥスタックを保持します。
  - 与え方:
    - GET リクエスト: クエリパラメータ `?room=<id>`（または `?session=<id>`）
    - POST リクエスト: JSON Body の `"room": "<id>"`（または `"session": "<id>"`）、またはクエリパラメータ
- **`viewer` (視点)**:
  - 型: `integer` (`0`: 神視点/全公開, `1`: Player 1 視点, `2`: Player 2 視点)
  - **既定値: `0`**
  - 与え方: クエリパラメータ `?viewer=1` またはリクエストヘッダ `X-Viewer: 1`

---

### エンドポイント別パラメータ詳細

#### 1. `GET /api/state`（盤面取得）
現在の局面（ライフ、リザーブ、手札、フィールド、選択可能手など）を取得します。

- **クエリパラメータ**:
  - `session`: 対象セッションID（省略時 `"default"`）
  - `viewer`: 視点 (`0`, `1`, `2`)
- **リクエスト例**:
  ```powershell
  # PowerShell
  Invoke-RestMethod -Uri "http://localhost:8090/api/state?session=game1&viewer=1"

  # curl
  curl "http://localhost:8090/api/state?session=game1&viewer=1"
  ```

#### 2. `GET /api/actions` または `GET /api/action`（合法手列挙）
現在の手番プレイヤーが選択可能な合法手の一覧を取得します。

- **クエリパラメータ**:
  - `session`: 対象セッションID
- **リクエスト例**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8090/api/actions?session=game1"
  ```

#### 3. `POST /api/act`（アクション実行・ステップ終了）
指定した選択肢番号のアクションを実行します。

- **JSON Body パラメータ**:
  - `index` (`integer`, 必須): 選択肢番号。`/api/actions` で返された `index`、またはステップ終了の場合は `999`
  - `session` (`string`, 任意): セッションID
- **リクエスト例**:
  ```powershell
  # アクション #2 (召喚など) を実行
  Invoke-RestMethod -Uri "http://localhost:8090/api/act" -Method POST `
    -ContentType "application/json" -Body '{"session":"game1","index":2}'

  # ステップ終了 (メインステップ終了等)
  Invoke-RestMethod -Uri "http://localhost:8090/api/act" -Method POST `
    -ContentType "application/json" -Body '{"session":"game1","index":999}'
  ```

#### 4. `POST /api/new`（新規ゲーム初期化）
新しいデッキやシード値でゲーム盤面を新規生成します。

- **JSON Body パラメータ**:
  - `deck1` (`string`, 任意): Player1 デッキファイル名（例: `"deck-kogyo.yaml"`, 省略時 `"deck-kogyo.yaml"`）
  - `deck2` (`string`, 任意): Player2 デッキファイル名（省略時 `"deck-kogyo.yaml"`）
  - `seed` (`integer`, 任意): 乱数シード（省略時はランダム生成）
  - `format` (`string`, 任意): ルールフォーマット (`"STANDARD"` または `"ETERNAL"`)
  - `session` (`string`, 任意): セッションID
- **リクエスト例**:
  ```powershell
  $body = @{
      session = "game1"
      deck1 = "deck-fara.yaml"
      deck2 = "deck-rairyu.yaml"
      seed = 12345
      format = "STANDARD"
  } | ConvertTo-Json
  Invoke-RestMethod -Uri "http://localhost:8090/api/new" -Method POST -ContentType "application/json" -Body $body
  ```

#### 5. `POST /api/restart`（同条件リスタート）
直前のデッキと同一設定で盤面を初期状態に戻します。

- **JSON Body パラメータ**:
  - `session` (`string`, 任意): セッションID
- **リクエスト例**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8090/api/restart" -Method POST `
    -ContentType "application/json" -Body '{"session":"game1"}'
  ```

#### 6. `POST /api/undo` / `POST /api/redo`（局面ロールバック・進める）
最大 500 手前の盤面へ巻き戻し、または進めます。

- **JSON Body パラメータ**:
  - `session` (`string`, 任意): セッションID
- **リクエスト例**:
  ```powershell
  # 1手戻す
  Invoke-RestMethod -Uri "http://localhost:8090/api/undo" -Method POST `
    -ContentType "application/json" -Body '{"session":"game1"}'

  # 1手進める
  Invoke-RestMethod -Uri "http://localhost:8090/api/redo" -Method POST `
    -ContentType "application/json" -Body '{"session":"game1"}'
  ```

#### 7. `GET /api/replay`（対局ログ取得）
ゲーム開始から現在（または終了時）までの着手履歴と局面スナップショット一覧を取得します。

- **クエリパラメータ**:
  - `session`: 対象セッションID
- **リクエスト例**:
  ```powershell
  Invoke-RestMethod -Uri "http://localhost:8090/api/replay?session=game1"
  ```

#### 8. `GET/POST /api/eval-config`（探索・評価器設定）
AI 探索の深さ・幅や、ニューラルネット評価モードを設定・取得します。

- **POST 時の JSON Body パラメータ**:
  - `mode` (`string`): `"GRU"` または `"NONE"` (評価なし)
  - `depth` (`integer`): ミニマックス探索の深さ ($1 \sim 4$)
  - `width` (`integer`): 探索の幅 ($0=$ 無制限, $1=$ 最善手のみ, $2 \ge$ 上位k手)
  - `weightsPathP1` (`string`, 任意): P1 重みファイルパス
  - `weightsPathP2` (`string`, 任意): P2 重みファイルパス
- **リクエスト例**:
  ```powershell
  # 設定取得
  Invoke-RestMethod -Uri "http://localhost:8090/api/eval-config"

  # 設定変更 (深さ2, 幅2)
  Invoke-RestMethod -Uri "http://localhost:8090/api/eval-config" -Method POST `
    -ContentType "application/json" -Body '{"mode":"NONE","depth":2,"width":2}'
  ```

#### 9. `GET /api/events`（SSE リアルタイム配信）
盤面に変更があった際に自動で最新の盤面 DTO が Server-Sent Events としてストリーミング配信されます。

- **クエリパラメータ**:
  - `session`: 購読するセッションID
  - `viewer`: 視点 (`0`, `1`, `2`)
- **利用例 (JavaScript)**:
  ```javascript
  const es = new EventSource('/api/events?session=game1&viewer=0');
  es.onmessage = (event) => {
      const state = JSON.parse(event.data);
      console.log('盤面更新:', state);
  };
  ```

---

## 🃏 ルールエンジン仕様詳細

- **コアシステム**: ライフ・リザーブ・トラッシュ・フィールド・創界神等の各ゾーン間でのコア移動、ソウルコア (Soul Core) の移動と管理。
- **ターン・ステップ進行**:
  - `START` → `CORE` → `DRAW` → `REFRESH` → `MAIN` → `ATTACK_DECLARATION` → `FLASH` → `BLOCK_DECLARATION` → `BATTLE_RESOLUTION` → `MAIN_2` → `END`
- **選択肢列挙 (`--actions`)**:
  - `level` (既定): Lv単位の正規形列挙。候補数が少なく、高速かつ安全。
  - `compact`: 圧縮列挙。
  - `all`: 支払いと配置の全組み合わせ（一括再配置を含む）。
