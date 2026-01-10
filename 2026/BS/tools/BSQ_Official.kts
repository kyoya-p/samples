/* **指示(AI編集禁止)**
  BSサイトに問い合わせる
  dependOn:
  - kotlinx.io
  - org.jetbrains.kotlinx:kotlinx-io-core:0.8.2
  - org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0
  - io.ktor:ktor-client:3.3.3
  - io.ktor:ktor-http:3.3.3
  - ksoap:

# usage
```
kotiln BSQ_official.kts [-h] [Keyword,Keyword,...]
```
-h またはパラメなし: ヘルプ表示

基本I/F:
val searchWords = args.map{it.toString}
typealias CardId = String
@Serializable data class Card(val id: String, val name: String)
suspend fun listCards(kfreeKyword: String): List<CardId> {}
suspend fun Card.updateCache(cardId: CardId) :Card {}

**指示ここまで** */

