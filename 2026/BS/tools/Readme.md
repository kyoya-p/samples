# バトスピデッキ構築補助ツール

このプロジェクトは、バトルスピリッツカードの検索と管理を支援します。

# 使用  
```shell  
kotlin tools/bsq.main.kts [-h] [-n] [-l] [Keyword Keyword ...]  
```  
-h: またはパラメなし: ヘルプ表示  
-n: 検索結果件数表示
-l: カード基本情報(id,名前)のみ1行1件で列挙 (キャッシュは行われます)
-c(デフォルト): Keywordで指示され検出したカードの情報をキャッシュファイル(cards)として保存

# 設計
- 言語/プラットフォーム
  - Kotlin KMP (Script runs on JVM)
  - javaモジュール(java.io等)不可

- 依存モジュール
```
dependOn:  
- [kotlinx-io](https://kotlinlang.org/api/kotlinx-io/) :0.8.1
- com.fleeksoft.ksoup:ksoup-network:0.2.5
- org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3 (JVM)
- io.ktor:ktor-client:3.0.3 (JVM)
- io.ktor:ktor-http:3.0.3 (JVM)
- org.jsoup:jsoup:1.17.2 (Instead of regex/ksoap)
```

- 基本I/F
```
val searchWords = args.map{it.toString}  
typealias CardId = String  
@Serializable data class Card(val id: String, val name: String, ...)
suspend fun listCards(keywords: List<String>, httpClient: HttpClient): List<Card> {}  
suspend fun Card.updateCache(httpClient: HttpClient) {}
```