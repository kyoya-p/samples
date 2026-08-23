# X検索採取ツール

Playwright (Java版) で X (旧Twitter) にアクセスし、検索結果をすべて採取するツール。

# 概要

- 指定したキーワードで X の検索画面 (`https://x.com/search`) を検索し、無限スクロールで表示されるツイートを重複なく採取する。
- X の検索結果はログインしないと閲覧できないため、事前に1回だけ手動ログインしてセッションを保存し、以降はそのセッションを再利用する。
- 採取されたツイートはメッセージ単位で `.x/<tweetId>.json` にキャッシュ保存されるほか、検索結果全体の JSON ファイルとして `output/` に保存する。

---

# ビルド・実行・セットアップ

ビルド、実行、各コマンドのオプションや実行例については [ルート Readme.md](../Readme.md) を参照。

```shell
# クイックリファレンス
mise run setup       # 初期セットアップ
mise run package     # JARビルド
mise run login        # ログイン
mise run search       # 検索・採取
mise run extract-sb   # 大会・ショップバトル結果抽出
mise run clean-cache  # キャッシュクリア
```

---

# 動作仕様

1. 保存済みセッション (`.auth/x-state.json`) を使ってブラウザコンテキストを生成し、検索URL (`x.com/search?q=<query>&f=live|top`) へアクセスする。
2. ツイート要素 (`article[data-testid="tweet"]`) をDOMから抽出し、ツイートID（`status/<id>`）をキーに重複排除しながら収集する。
3. ページ末尾までスクロール (`page.mouse().wheel(...)`) → 待機 → 再抽出、を繰り返す。
4. スクロールしても新規ツイートが**5回連続**で取得できなかった場合、検索結果の末尾に到達したとみなして終了する。
5. `--max` 指定時は、採取件数が上限に達した時点で終了する。
6. 収集したツイートを配列としてJSONファイル (kotlinx.serialization) に書き出す。
7. `extract-sb` コマンドで、収集ツイートを Gemini API (`gemini-2.5-flash-lite`) にバッチ送信し、大会結果（店舗名、イベント種別、参加人数、優勝者、デッキ名、備考）を構造化抽出して CSV に保存する。

## 出力フォーマット

### 1. 検索結果 (JSON)

```json
[
  {
    "id": "1234567890123456789",
    "url": "https://x.com/i/web/status/1234567890123456789",
    "author": "表示名",
    "handle": "screen_name",
    "postedAt": "2026-08-14T09:00:00.000Z",
    "text": "ツイート本文",
    "hasImage": false,
    "hasVideo": false,
    "replyCount": 0,
    "retweetCount": 0,
    "likeCount": 0
  }
]
```

### 2. ショップバトル抽出結果 (CSV)

```csv
id,url,posted_at,event_category,store_or_event_name,format,deck_type,participants,winner,notes,author,tweet_text
"1234567890","https://x.com/i/web/status/1234567890","2026-08-15T10:00:00.000Z","店舗バトル","カードショップ○○","スタンダード","鋼契約","16","プレイヤーA","準優勝: 紫エヴァ","カードショップ○○","【#バトスピ 大会結果】本日開催のショップバトル..."
```

### 3. 月足シェア率 可視化ビューア (HTML)

抽出した CSV ファイルをドラッグ＆ドロップするだけで、即座にフォーマット別（全フォーマット / スタンダード / エターナル）の月足シェア率折れ線グラフとデータテーブルを生成するビューア:

- [`deck_share_trend.html`](deck_share_trend.html)（ブラウザで直接開いて利用可能）

---

# 制限事項

- X の仕様上、検索結果として表示・取得できる件数やさかのぼれる期間には上限がある（アカウント種別やレート制限の影響を受ける）。「すべて採取」とは、**その時点でUI上にスクロールして表示可能な範囲をすべて**という意味であり、X内部に存在する全ツイートを保証するものではない。
- 短時間に大量のリクエストを行うと、一時的なレート制限やアカウント制限の対象となる可能性がある。連続実行の間隔には注意すること。
- DOM構造 (`data-testid` 属性等) は X 側の仕様変更により変わる可能性があり、その場合は `src/TweetExtractScript.kt` の抽出ロジックの修正が必要になる。
- Playwright Java (JVM) を使用しているため、本ツールはJVM上でのみ動作する（Kotlin Nativeターゲット非対応）。

---

# 環境変数

| 変数名 | 説明 | デフォルト値 |
|---|---|---|
| `X_QUERY` | デフォルトの検索キーワード | `(バトスピ OR バトルスピリッツ OR battlespirits) (優勝 OR 全勝 OR ウィナー OR 勝者)` |
| `X_CACHE_DIR` | 個別ツイートJSONのキャッシュ保存先ディレクトリ | `.x` |
| `X_OUTPUT_DIR` | 総合JSON/CSVの出力先ディレクトリ | `output` |
| `X_COOLDOWN_THRESHOLD` | 安全のための連続採取件数閾値 | `600` |
| `X_COOLDOWN` / `X_COOLDOWN_SEC` | クールダウン待機時間（秒） | `240` |
| `GEMINI_API_KEY` | 大会結果抽出用 Gemini API キー | - |

---

# フォルダ構成

- `module.yaml` : Amperモジュール定義 (`jvm/app`)。
- `src/Main.kt` : CLIエントリポイント（clikt subcommands: `login` / `search` / `install-browsers` / `extract-sb`）。
- `src/Login.kt` : 手動ログイン用コマンド。セッションを `.auth/x-state.json` に保存。
- `src/Search.kt` : 検索・採取コマンド本体。
- `src/ExtractShopBattle.kt` : Gemini APIを用いたショップバトル結果抽出・CSV出力コマンド。
- `src/InstallBrowsers.kt` : Playwright用Chromiumインストールコマンド。
- `src/TweetExtractScript.kt` : DOM上のツイート要素から情報を抽出するJSスニペット。
- `src/Tweet.kt` : ツイートのデータクラスとパース処理。
- `src/Config.kt` : パス等の共通設定。
- `.auth/` (リポジトリルート) : ログインセッション保存先（Git管理対象外）。
- `output/` (リポジトリルート) : 採取結果JSONおよびCSVの出力先（Git管理対象外）。
