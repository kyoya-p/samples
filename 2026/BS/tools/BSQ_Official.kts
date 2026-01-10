/* **指示ここから**
  BSサイトに問い合わせる
  modules: Kotlinx.io. Serialization, Ktor3.3.3, KSoap

基本IF:
val searchWords = args.map{it.toString}
typealias CardId = String
@Serializable data class Card(val id: String, val name: String)
suspend fun listCards(kfreeKyword: String): List<CardId> {}
suspend fun Card.updateCache(cardId: CardId) :Card {}
**指示ここまで** */

@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-io-core-jvm:0.6.0")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-http-jvm:3.0.3")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.16")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3")


