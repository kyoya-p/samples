# バトスピデッキ構築補助

このプロジェクトは、「超星」などの特定の系統に焦点を当てたバトルスピリッツカードの検索と管理を支援します。

## スクリプト

`gemini/` ディレクトリには、バトスピWikiからカードデータをスクレイピングするためのPythonスクリプトが含まれています。

### 前提条件

- Python 3.7以上
- Playwright
- BeautifulSoup4

### インストール

1. Pythonパッケージをインストールします:
   ```bash
   pip install playwright beautifulsoup4
   ```
2. Playwrightブラウザをインストールします:
   ```bash
   playwright install chromium
   ```

### 使用方法

#### カード検索
`search_system.py` スクリプトを実行して、Wiki上でカードを検索します。系統や効果テキストを指定できます。


```shell : 系統と効果を組み合わせて検索
python gemini/search_system.py -s 超星 -e "回復"
```

#### Kotlin Script版 (`search_system.kts`)
Python版と同様の機能をKotlinスクリプトとして実装したものです。実行には `kscript` またはKotlinコンパイラが必要です。

```bash : 系統と効果を組み合わせて検索
kotlin gemini/search_system.kts -s 超星 -e "回復"
```

#### 検索ページの調査
`inspect_search_page.py` を使用して、Wikiの検索ページの構造（フォームや入力フィールド）を解析します。スクレイパーのデバッグや更新に役立ちます。

```bash
python gemini/inspect_search_page.py
```

## データ
カードデータは `card/` ディレクトリにJSONファイルとして保存されています。
