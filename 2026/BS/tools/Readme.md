# バトスピデッキ構築補助ツール

バトルスピリッツカードの検索と管理の支援ツール。
**AIはこのファイルの変更禁止**

# 使用
配布パッケージ (`bscard.zip`) を解凍し、中の `bscard.exe` を使用。

```shell
.\bscard.exe <subcommand> [options]...
```

## サブコマンド

### fetch
指定した条件でカードを検索し、詳細情報をキャッシュファイル(html,yaml)として保存。

```shell
.\bscard.exe fetch [options] [Keyword...]
```

Options:
-f, --force               キャッシュが存在する場合でも強制的に再取得して上書き
-d, --cache-dir=<value>   カードデータのキャッシュ先ディレクトリを指定 (default: C:\Users\kyoya\.bscards)
-c, --cost <range>        コスト範囲を指定（例: "3-5"、"7"）
-a, --color <attr>        属性（色）を指定。OR検索 (例: 赤の場合"R", 赤緑の場合"RG")。
-A, --color-and <attr>    属性（色）を指定。AND検索 (例: 全色の場合"RPGWYB")。
-s, --system <family>     系統を指定 (例: 星竜)。複数指定可(OR検索) (*1)
-S, --system-and <family> 系統を指定 (例: 星竜)。複数指定可(AND検索) (*1)
-t, --type <category>     カテゴリを指定 (例: スピリットの場合"S", アルティメットの場合"U")
-b, --block <icon>        ブロックアイコンを指定 (例: 7)。複数指定可

*1: 属性・系統のAND/ORは、引数リストの最後に指定されたオプションで決定。

### neo
キャッシュされたカードデータから Neo4j DBに格納。

```shell
.\bscard.exe neo [options] [cardId...]
```

Options:
-d, --cache-dir=<value>   入力となるキャッシュディレクトリを指定
--url <url>               neo4jのURLと認証情報を指定 (デフォルト: neo4j://127.0.0.1:7687)
--login <pattern>         neo4jの認証情報を指定(デフォルト: 'neo4j:00000000')

# 画像生成ツール (faces)
指定したカードIDの画像を3枚1組で横に連結し、縦に3回繰り返したグリッド画像を生成する。
Amperモジュールとして直接実行する。

```shell
.\amper.bat run --module faces -- <cardId>...
```

例:
```shell
.\amper.bat run --module faces -- BS75-051 BS75-068 BS75-069
```

出力先: `output/`

# フィールドシミュレーター (gamefield)
Compose Multiplatform (Desktop) を使用したカードゲームフィールドのシミュレーター。
手札からフィールドへのカード配置や、コア管理などの基本操作が可能。

```shell
.\amper.bat run --module gamefield
```

# テスト実行
```shell:Help
./amper run -m jvm-cli -- -h     # Jvm
./amper run -m linux-cli -- -h   # Linux
./amper run -m windows-cli -- -h # Windows
```

# ビルド
```shell
./amper task :jvm-cli:executableJarJvm
./amper task :linux-cli:linkLinuxX64Release
./amper task :windows-cli:linkMingwX64Release
```
# Jvm実行
```shell  
java -jar tools/build/tasks/_jvm-cli_executableJarJvm/jvm-cli-jvm-executable.jar fetch [options]... [Keyword...] 
```
# 配布用パッケージ作成 (Windows)
```powershell
./package.ps1
```
`tools/build/installer/bscard.zip` を生成。

# フォルダ構成
- .bscards/ : カードデータダウンロードキャッシュファイル
- .bscards/html/$cardNo.html : 受信したカードデータそのものをHTMLテキストの形で保存される。
- .bscards/yaml/$cardNo.yaml : 受信したカードデータがYAMLの形で保存される。
- ./temp/     : 一時データ、一時スクリプト

## Neo4j グラフモデル
`generate-cypher` サブコマンドにより、以下の構造でインポートされる。
- **ノード**: `Card`, `CardFace`, `Category`, `Rarity`, `Color`,`Cost`, `System`, `Keyword`, (【...】), `Timing` (『...』)
- **リレーションシップ**:
  - `(Card)-[:HAS_FACE]->(CardFace)`
  - `(CardFace)-[:IS_CATEGORY]->(Category)`
  - `(CardFace)-[:HAS_COLOR]->(Color)`
  - `(CardFace)-[:HAS_COST]->(Cost)`
  - `(CardFace)-[:HAS_SYSTEM]->(System)`
  - `(CardFace)-[:HAS_KEYWORD]->(Keyword)`
  - `(CardFace)-[:TRIGGERS_AT]->(Timing)`

# テスト

## 共通指定:
  - プラットフォーム: jvm-cli
  - キャッシュディレクトリ: ./.bscards-test
  - test-report: ./build/reports/ai-test.mmyydd.hhmmss.txt
    - ハルシネーション対策のため、検証可能であること

## Test 1
- キャッシュディレクトリからcardId=BS71-001に関するファイルを削除
- cardId=BS71-001 で fetch
- [確認] .bscards/html/BS71-001.html, .bscards/html/BS71-001.yaml, を確認、正しく生成されていること
- cardId=BS71-001 fetch
- [確認] 標準出力を確認。すでにキャッシュにカード情報がある場合、採取はスキップ(通信しない)。
- -f オプションとともにcardId=BS71-001 をsearch
- [確認] 標準出力を確認。通信し取得したことを確認。

## Test 2
- cardId=BS71, -c 5-6 で fetch
- [確認] 標準出力で生成ファイル確認。取得したすべてのyamlファイル内のコストが5または6であること。

## Test 3
- cardId=BS70, -a RB で fetch
- [確認] 標準出力で生成ファイル確認。取得したすべてのyamlファイル内のattributesは"赤"または"青"を含む。
- cardId=BS70, -A RB で fetch
- [確認] 標準出力で生成ファイル確認。取得したすべてのyamlファイル内のattributesは"赤"と"青"を両方含む。

## Test 4
- -d=./.tempcache, BS70-001 fetch
- [確認] ./.tempcache/html/BS70-001.html が存在すること
- [確認] ./.tempcache/yaml/BS70-001.yaml が存在すること
- [確認] 上記ふたつの内容に齟齬ないこと
- ./.tempcache ディレクトリを削除

## Test 11
- ターゲットのカードを1つ選定
- neo-mcpにてターゲットのカードが未登録であることを確認、登録されていれば選定から再試行
- --url=neo4j://127.0.0.1:7687 --login=neo4j:00000000 で neo
- [確認] neo-mcpにて投入されたことを検証

## Test 12
- ターゲットのカードを2つ選定
- neo-mcpにてターゲットのカードが未登録であることを確認、登録されていれば選定から再試行
- --url=neo4j://127.0.0.1:7687 --login=neo4j:00000000 で neo
- [確認] neo-mcpにて投入されたことを検証


## Test 91 (その他のプラットフォーム)
- [実行環境がWindowsプラットフォームの場合] windows-cliプラットフォームで Test 1を実施