@file:DependsOn("org.jetbrains.kotlinx:kotlinx-io-core:0.8.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.3")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.2.5")
@file:DependsOn("com.charleskorn.kaml:kaml-jvm:0.63.0")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.16")
@file:Import("./bsSearch.main.kts")
@file:Import("./bsDetail.main.kts")

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.*

val homeDir = System.getProperty("user.home")
val cacheDir = Path(homeDir, ".bscards")
if (!SystemFileSystem.exists(cacheDir)) SystemFileSystem.createDirectories(cacheDir)

val categoryMap = mapOf(
    "スピリット" to "S",
    "アルティメット" to "U",
    "ブレイヴ" to "B",
    "ネクサス" to "N",
    "マジック" to "M"
)

fun String.yamlEscape() = "\"" + this.replace("\"", "\\\"").replace("\n", "\\n") + "\""

if (args.get(0) == "query") runBlocking { bsqMain(args.drop(1)) }

suspend fun bsqMain(args: List<String>) {

    val keywords = args.drop(1).joinToString(" ")
    if (keywords.isEmpty()) {
        println("Please provide at least one keyword.")
        return
    }

    println("Searching for: $keywords")
    val searchResults = bsSearchMain(
        keywords = keywords,
        cardNo = "",
        costMin = 0,
        costMax = 30,
        attr = "",
        category = emptyList(),
        system = emptyList()
    )

    val cardNos = searchResults.map { it.cardNo }.distinct()

    for (cardNo in cardNos) {
        println("Processing $cardNo...")
        val detailedSides = bsDetail(cardNo)

        val sb = StringBuilder()
        sb.append("id: $cardNo\n")
        sb.append("cards:\n")

        detailedSides.forEach {
            sb.append(" - name: ").append(it.name.yamlEscape()).append("\n")
            sb.append("   category: ").append(categoryMap[it.category] ?: it.category).append("\n")
            sb.append("   attr: ").append(it.attributes.yamlEscape()).append("\n")
            sb.append("   cost: ").append(it.cost).append("\n")
            sb.append("   symbol: ").append(it.symbols.yamlEscape()).append("\n")
            sb.append("   reduction: ").append(it.reductionSymbols.yamlEscape()).append("\n")
            sb.append("   family: [").append(it.systems.joinToString(", ") { s -> s.yamlEscape() }).append("]\n")
            sb.append("   lvCost: [").append(it.lvInfo.joinToString(", ") { lv -> lv.yamlEscape() }).append("]\n")
            sb.append("   effect: ").append(it.effect.yamlEscape()).append("\n")
            sb.append("   note: \"\"\n")
        }

        val yamlString = sb.toString()
        val filePath = Path(cacheDir, "$cardNo.$car.yaml")

        val sink = SystemFileSystem.sink(filePath)
        val bufferedSink = sink.buffered()
        bufferedSink.writeString(yamlString)
        bufferedSink.close()
        println("Saved to $filePath")
    }
}
