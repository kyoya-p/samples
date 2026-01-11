# バトスピデッキ構築補助ツール

このプロジェクトは、バトルスピリッツカードの検索と管理を支援します。
**AIはこのファイルの変更禁止**

# 使用  
```shell  
kotlin tools/bsq.main.kts [-h] [-n] [-l] [-c キャッシュフォルダ] [Keyword Keyword ...]  
```  
-h: またはパラメなし: ヘルプ表示  
-n: 検索結果件数表示
-l: カード基本情報(id,名前)のみ1行1件で列挙 (キャッシュは行われます)
-c キャッシュフォルダ: 指定したフォルダにカード情報をキャッシュとして保持。指定なければ`~/.bscards`
Keyword: Keywordで指示され検出したカードの情報をキャッシュファイル(cards/*.yaml)として保存

# 内容
```yaml
  id: BS-**
  cards: 
   - name: カード名
     category: S # スピリット:S,アルティメット:U, ブレイヴ:B, ネクサス:N, マジック:M
     attr: "赤紫緑..."
     cost: 7
     symbol: "赤1"
     reduction: "赤1紫2全3" # ◇=全
     family: ["系統1","系統2","系統3"]
     lvCost: ["1,3,8000", "2,5,12000", "3,7,16000"]
     effect: "効果の記述"
     note: "制限"、"禁止"、"何枚でもデッキに入れられる" 等の情報
```

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
- io.ktor:ktor-client:3.3.3 (JVM)
- io.ktor:ktor-http:3.3.3 (JVM)
- org.jsoup:jsoup:1.17.2 (Instead of regex/ksoap)
```

- 基本I/F
```
val searchWords = args.map{it.toString}  
typealias CardId = String  
@Serializable data class Cards(val id: String, val cards: List<Card>)
@Serializable data class Card(val name: String, category,:String, ...)
suspend fun listCards(keywords: List<String>, httpClient: HttpClient): List<Card> {}  
suspend fun Card.updateCache(httpClient: HttpClient) {}
```