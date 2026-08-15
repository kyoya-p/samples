# X検索採取ツール

Playwright (Java版) で X (旧Twitter) にアクセスし、検索結果をすべて採取するツール。
Kotlin/JVM + JetBrains Amper で実装。

# 概要

- 指定したキーワードで X の検索画面 (`https://x.com/search`) を検索し、無限スクロールで表示されるツイートを重複なく採取する。
- X の検索結果はログインしないと閲覧できないため、事前に1回だけ手動ログインしてセッションを保存し、以降はそのセッションを再利用する。
- 採取結果は JSON ファイルとして `output/` に保存する。

# セットアップ

```shell
mise run setup
```

Amperラッパー (`amper.bat`) の更新と、Playwright用 Chromium ブラウザのインストール (`install-browsers` サブコマンド) を行う。

# 使用方法

リポジトリルートから直接実行する場合:

```shell
.\amper.bat run --module X -- <subcommand> [options]...
```

`X` ディレクトリ内の mise タスク経由で実行する場合:

```shell
mise run login
mise run search -- "<検索キーワード>" [options]
```

## 1. ログイン（初回のみ）

```shell
.\amper.bat run --module X -- login
```

ブラウザ（ヘッド付き）が起動するので、手動で X にログインする（2段階認証が必要な場合も手動で完了させる）。
ホームタイムライン (`x.com/home`) の表示を検知すると、セッション情報 (Cookie等) をリポジトリルートの `.auth/x-state.json` に保存してブラウザを終了する。

- `.auth/x-state.json` にはログインセッションが含まれるため、`.gitignore` で管理対象外としている。第三者と共有しないこと。
- セッションが失効した場合（採取時にログイン画面へリダイレクトされる等）は再度ログインを実行する。

## 2. 検索・採取

```shell
.\amper.bat run --module X -- search "<検索キーワード>" [options]...
```

### Subcommand: search

| Option | 説明 |
| --- | --- |
| `-l`, `--latest` | 「トップ」タブではなく「最新」タブで検索する（デフォルト: トップ） |
| `-m`, `--max <件数>` | 採取するツイート数の上限を指定（デフォルト: 上限なし。末尾まで採取） |
| `-o`, `--output <path>` | 出力先ファイルパスを指定（デフォルト: `output/x-search-<キーワード>-<日時>.json`） |
| `--headed` | ブラウザをヘッド付き（画面表示あり）で起動する（デフォルト: ヘッドレス。デバッグ用） |

### 例

```shell
# 「バトルスピリッツ」をトップタブで検索し、末尾まですべて採取
.\amper.bat run --module X -- search "バトルスピリッツ"

# 「バトルスピリッツ」を最新タブで検索し、最大300件まで採取
.\amper.bat run --module X -- search "バトルスピリッツ" -l -m 300

# 出力先を指定
.\amper.bat run --module X -- search "バトルスピリッツ" -o ./output/bs.json
```

# 動作仕様

1. 保存済みセッション (`.auth/x-state.json`) を使ってブラウザコンテキストを生成し、検索URL (`x.com/search?q=<query>&f=live|top`) へアクセスする。
2. ツイート要素 (`article[data-testid="tweet"]`) をDOMから抽出し、ツイートID（`status/<id>`）をキーに重複排除しながら収集する。
3. ページ末尾までスクロール (`page.mouse().wheel(...)`) → 待機 → 再抽出、を繰り返す。
4. スクロールしても新規ツイートが**5回連続**で取得できなかった場合、検索結果の末尾に到達したとみなして終了する。
5. `--max` 指定時は、採取件数が上限に達した時点で終了する。
6. 収集したツイートを配列としてJSONファイル (kotlinx.serialization) に書き出す。

## 出力フォーマット (JSON)

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

# 制限事項

- X の仕様上、検索結果として表示・取得できる件数やさかのぼれる期間には上限がある（アカウント種別やレート制限の影響を受ける）。「すべて採取」とは、**その時点でUI上にスクロールして表示可能な範囲をすべて**という意味であり、X内部に存在する全ツイートを保証するものではない。
- 短時間に大量のリクエストを行うと、一時的なレート制限やアカウント制限の対象となる可能性がある。連続実行の間隔には注意すること。
- DOM構造 (`data-testid` 属性等) は X 側の仕様変更により変わる可能性があり、その場合は `src/TweetExtractScript.kt` の抽出ロジックの修正が必要になる。
- Playwright Java (JVM) を使用しているため、本ツールはJVM上でのみ動作する（Kotlin Nativeターゲット非対応）。

# フォルダ構成

- `module.yaml` : Amperモジュール定義 (`jvm/app`)。
- `src/Main.kt` : CLIエントリポイント（clikt subcommands: `login` / `search` / `install-browsers`）。
- `src/Login.kt` : 手動ログイン用コマンド。セッションを `.auth/x-state.json` に保存。
- `src/Search.kt` : 検索・採取コマンド本体。
- `src/InstallBrowsers.kt` : Playwright用Chromiumインストールコマンド。
- `src/TweetExtractScript.kt` : DOM上のツイート要素から情報を抽出するJSスニペット。
- `src/Tweet.kt` : ツイートのデータクラスとパース処理。
- `src/Config.kt` : パス等の共通設定。
- `.auth/` (リポジトリルート) : ログインセッション保存先（Git管理対象外）。
- `output/` (リポジトリルート) : 採取結果JSONの出力先（Git管理対象外）。
