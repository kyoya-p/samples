# SurveyX (X検索採取・メタゲーム分析ツール)

Playwright (Java版) で X (旧Twitter) にアクセスし、検索結果を採取・分析するツール。

# 概要

- 指定したキーワードで X の検索画面 (`https://x.com/search`) を検索し、無限スクロールで表示されるツイートを重複なく採取する。
- X の検索結果はログインしないと閲覧できないため、事前に1回だけ手動ログインしてセッションを保存し、以降はそのセッションを再利用する。
- 採取されたツイートはメッセージ単位で `.x/<tweetId>.json` にキャッシュ保存されるほか、検索結果全体の JSON ファイルとして `output/` に保存する。
- 採取データから Gemini API による大会結果構造化抽出 (`extract-sb`) や、環境メタゲーム集計・レポート生成 (`analyze`) を行う。

---

# コマンド体系

```shell
# ヘルプ表示
.\amper.bat run -m SurveyX -- --help

# ログイン (セッション保存)
.\amper.bat run -m SurveyX -- login

# 検索・採取
.\amper.bat run -m SurveyX -- search -q "バトスピ 優勝" -n 50

# ショップバトル結果抽出 (Gemini API)
.\amper.bat run -m SurveyX -- extract-sb

# メタゲーム分析・レポート生成 (サンプル実行)
.\amper.bat run -m SurveyX -- analyze --sample

# メタゲーム分析・レポート生成 (キャッシュデータ + JSON出力)
.\amper.bat run -m SurveyX -- analyze -o output/meta_report.json

# キャッシュクリア
.\amper.bat run -m SurveyX -- clear-cache

# Playwright用ブラウザインストール
.\amper.bat run -m SurveyX -- install-browsers
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
8. `analyze` コマンドで、収集ツイートまたはサンプルデータからアーキタイプ（蒼契約、獄契約、秘契約、アイツのデッキ等）を自動判定・分類し、勝率シェア・メタレポート (`MetaReport`) を集計・出力する。
